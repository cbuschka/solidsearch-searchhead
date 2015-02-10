package de.solidsearch.searchhead.restservices.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import de.solidsearch.searchhead.utils.EsClientManager;
import de.solidsearch.shared.data.KeywordStem;
import de.solidsearch.shared.textanalysis.RoutingGenerator;
import de.solidsearch.shared.textanalysis.TextNormalizer;
import de.solidsearch.shared.utils.QWLocale;

@Component("SearchQueryManager")
@Scope(value = "prototype")
public class SearchQueryManager
{
	@Autowired
	EsClientManager esc;

	private static final Logger logger = Logger.getLogger(SearchQueryManager.class.getName());

	private int debugmode = -1;

	private String debugOutPut = "";

	private String minShouldMatch = "85%";

	private String searchScript = null;

	public final String BRANDINDEXALIASNAME = "solidsearch_brands_alias";

	public final static String SEARCHINDEXALIASNAME = "solidsearch_search_alias";
	
	public RoutingGenerator rg = new RoutingGenerator();

	public String getSearchResult(String query, int page, int debugmode, HttpServletRequest request)
	{
		// do some generals
		long timebefore = System.currentTimeMillis();

		StringBuffer resultHTML = new StringBuffer();

		this.debugmode = debugmode;

		searchScript = null;

		TransportClient client = esc.getTransportClient();

		// preparations
		short qwLocale = detectLanguage(query);

		String[] keywords = query.split("\\s|-");
		
		if (keywords.length == 0)
			return "";
		
		String mainKeyword = keywords[0];

		ArrayList<String> routings = getSearchIndexRoutings(keywords);

		SearchRequestBuilder srb = client.prepareSearch(SEARCHINDEXALIASNAME,BRANDINDEXALIASNAME);

		// DFS QUERY THEN FETCH: TFxIDF over all shards
		// later (with more data per index) we can disable this...
		srb.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		srb.setRouting(routings.toArray(new String[routings.size()]));
		
		// currently only static paging
		srb.setFrom(0);
		srb.setSize(100);

		setUpDebugModeIfNecessary(query);

		MultiMatchQueryBuilder b = QueryBuilders.multiMatchQuery(cleanupAndRewriteSearchQuery(query), "domainname^4", "brandname^5", "title_stemmed^3", "title_ngram^2", "onpagetext_stemed^2", "onpagetext_ngram").minimumShouldMatch(minShouldMatch);
		
		srb.setQuery(b);
		// avoid less relevant hits
		srb.setMinScore(0.6f);
		
		if (debugmode >= 0)
			System.out.println(srb.toString());

		SearchResponse response = srb.execute().actionGet();
		long timeAfter = System.currentTimeMillis() - timebefore;

		SearchHit[] results = response.getHits().hits();
		long totalDocs = response.getHits().getTotalHits();

		if (debugmode >= 0)
			resultHTML.append("<p style=\"font-size:70%;color:#AAAAAA;\">").append(totalDocs).append(" Treffer, ").append(timeAfter).append(" Millisekunden").append(debugOutPut).append(" </p>");

		resultHTML = generateResultHTML(reorganizeSearchResults(results), resultHTML, mainKeyword);

		logger.info("QUERY:\"" + query + "\", HITS: " + totalDocs + ", DURATION: " + timeAfter + ", ROUTINGS ASKED: " + routings.size() + ", USERAGENT: " + request.getHeader("user-agent"));

		return resultHTML.toString();
	}

	/**
	 * Method generates SERPs snippets HTML-Code
	 * 
	 * @param reviewedHits
	 * @param resultHTML
	 *            (input)
	 * @param mainKeyword
	 * @return resultHTML (output)
	 */
	private StringBuffer generateResultHTML(Map<SearchHit, Float> reviewedHits, StringBuffer resultHTML, String mainKeyword)
	{
		for (Iterator<SearchHit> iterator = reviewedHits.keySet().iterator(); iterator.hasNext();)
		{
			SearchHit hit = iterator.next();
			Map<String, Object> result = hit.getSource();

			String title = result.get("title").toString();
			String uri = result.get("uri").toString();
			String metadescription = result.get("metadescription").toString();

			if (metadescription.length() <= 0)
			{
				metadescription = generateMissingMetadescription(mainKeyword, result.get("onpagetext").toString());
			}

			title = title.length() >= 76 ? title.substring(0, 76) + " ..." : title;
			uri = uri.length() >= 155 ? uri.substring(0, 155) + " ..." : uri;
			metadescription = metadescription.length() >= 155 ? metadescription.substring(0, 155) + " ..." : metadescription;

			resultHTML.append("<a href=\"").append(uri).append("\">");
			resultHTML.append("<h3 style=\"width:600px; font-size:100%;\">").append(title).append("</br>").append("<small style=\"font-size:75%;color:#74b761;font-weight:normal;\">").append(result.get("uri")).append("</small></h3>");
			resultHTML.append("<div style=\"word-wrap:break-word; width:600px; font-size:85%; color:#595959;\">").append(metadescription).append("</br>");

			if (debugmode >= 0)
			{
				resultHTML.append("</br> rank=" + reviewedHits.get(hit) + " score=" + hit.getScore()).append(", searchscore=" + result.get("searchscore")).append(", spamscore=" + result.get("spamscore"));
				resultHTML.append(", topickeyword=" + result.get("topickeyword")).append(", brandname=" + result.get("brandname"));
				resultHTML.append(", qualityscore=" + result.get("qualityscore")).append(", varietytopicscore=" + result.get("varietytopicscore"));
				resultHTML.append(", adscripts=" + result.get("adscripts")).append(", relevantimages=" + result.get("relevantimages"));
				resultHTML.append(", domaintrustscore=" + result.get("domaintrustscore")).append(", domainqualityscore=" + result.get("domainqualityscore"));
				resultHTML.append(", domaintechscore=" + result.get("domaintechscore")).append(", readinglevel=" + result.get("readinglevel"));
			}
			resultHTML.append("</small></div></a>");
		}
		resultHTML.append("</br><div style=\"text-decoration:none; color:#6689e2;font-size:85%;text-align:right;\">");

		// for later pagination....
		// if (page > 1)
		// {
		// resultHTML.append("<a href=\"javascript:prevPage()\" style=\"text-decoration:none; color:#6689e2;font-size:85%;\"> < Seite ").append(page-1).append("</a>");
		// }
		// if ((page * 20)<totalDocs)
		// {
		// if (page != 1)
		// resultHTML.append(" / ");
		// resultHTML.append("<a href=\"javascript:nextPage()\" style=\"text-decoration:none; color:#6689e2;font-size:85%;\"> Seite ").append(page+1).append(" > </a>");
		// }

		resultHTML.append("</div></br>");

		return resultHTML;
	}

	/**
	 * Method removes invailid chars and rewrite query for synonm search.
	 * 
	 * @param query
	 * @return rewritten query
	 */
	private String cleanupAndRewriteSearchQuery(String query)
	{
		/**
		 * TODO
		 */
		return query;
	}

	/**
	 * Method reorganizes search results.
	 * 
	 * @param results
	 * @return
	 */
	private Map<SearchHit, Float> reorganizeSearchResults(SearchHit[] results)
	{
		Map<SearchHit, Float> reviewedHits = new LinkedHashMap<SearchHit, Float>();
		ArrayList<String> seenDomains = new ArrayList<String>();

		SearchHit rootdocument = null;

		boolean brandFound = false;

		// search for root and get max scores...

		float maxScore = 0;
		long maxSearchScore = 0;

		for (SearchHit hit : results)
		{
			if (hit.getType().equalsIgnoreCase("rootdocuments") && brandFound == false)
			{
				rootdocument = hit;
				reviewedHits.put(hit, 1f);
				brandFound = true;
			}

			if (hit.getScore() > maxScore)
				maxScore = hit.getScore();
			if ((Integer) hit.getSource().get("searchscore") > maxSearchScore)
				maxSearchScore = (Integer) hit.getSource().get("searchscore");
		}

		float maxScore80perc = maxScore * 0.8f;
		float maxScore60perc = maxScore * 0.6f;
		float maxScore40perc = maxScore * 0.4f;
		float maxScore20perc = maxScore * 0.2f;

		float maxSearchScore80perc = maxSearchScore * 0.8f;
		float maxSearchScore60perc = maxSearchScore * 0.6f;
		float maxSearchScore40perc = maxSearchScore * 0.4f;
		float maxSearchScore20perc = maxSearchScore * 0.2f;

		Map<SearchHit, Float> tmpHits = new HashMap<SearchHit, Float>();

		for (SearchHit hit : results)
		{
			int searchScore = (Integer) hit.getSource().get("searchscore");

			// overweigth searchscore...
			float normalizedESScoreToOne = hit.score() / maxScore * 0.55f;
			float normalzedSearchScoreToOne = searchScore / maxSearchScore * 0.45f;

			if (hit.score() < maxScore20perc)
			{
				normalzedSearchScoreToOne = 0;
			}
			else if (hit.score() < maxScore40perc)
			{
				normalzedSearchScoreToOne = normalzedSearchScoreToOne * 0.4f;
			}
			else if (hit.score() < maxScore60perc)
			{
				normalzedSearchScoreToOne = normalzedSearchScoreToOne * 0.6f;
			}
			else if (hit.score() < maxScore80perc)
			{
				normalzedSearchScoreToOne = normalzedSearchScoreToOne * 0.8f;
			}

			if (searchScore < maxSearchScore20perc)
			{
				normalizedESScoreToOne = 0;
			}
			else if (searchScore < maxSearchScore40perc)
			{
				normalizedESScoreToOne = normalizedESScoreToOne * 0.4f;
			}
			else if (searchScore < maxSearchScore60perc)
			{
				normalizedESScoreToOne = normalizedESScoreToOne * 0.6f;
			}
			else if (searchScore < maxSearchScore80perc)
			{
				normalizedESScoreToOne = normalizedESScoreToOne * 0.8f;
			}

			tmpHits.put(hit, normalizedESScoreToOne + normalzedSearchScoreToOne);
		}

		Map<SearchHit, Float> sortedHits = sortByComparator(tmpHits);

		if (debugmode != 6)
		{
			// show 3 results of the same domain
			for (int z = 0; z < 3; z++)
			{
				// remove entries from the same domain...
				for (Iterator<SearchHit> iterator = sortedHits.keySet().iterator(); iterator.hasNext();)
				{
					SearchHit hit = iterator.next();
					Map<String, Object> result = hit.getSource();

					if (!seenDomains.contains(result.get("domainname").toString()))
					{
						if (!reviewedHits.containsKey(hit))
						{
							boolean useHit = true;
							if (rootdocument != null)
							{
								if (hit.getId().equalsIgnoreCase(rootdocument.getId()))
								{
									useHit = false;
								}
							}
							if (useHit)
							{
								reviewedHits.put(hit, sortedHits.get(hit));
								seenDomains.add(result.get("domainname").toString());

								if (brandFound == true)
								{
									seenDomains.clear();
									brandFound = false;
								}
							}
						}
					}
				}
				seenDomains.clear();
			}
		}
		else
		{
			for (Iterator<SearchHit> iterator = sortedHits.keySet().iterator(); iterator.hasNext();)
			{
				SearchHit hit = iterator.next();

				boolean useHit = true;
				if (rootdocument != null)
				{
					if (hit.getId().equalsIgnoreCase(rootdocument.getId()))
					{
						useHit = false;
					}
				}
				if (useHit)
				{
					reviewedHits.put(hit, sortedHits.get(hit));
				}
			}
		}
		return reviewedHits;
	}

	/**
	 * Method generates a meta-description upon indexed text.
	 * 
	 * @param topKeyword
	 * @param onPageText
	 * @return
	 */
	private String generateMissingMetadescription(String mainKeyword, String onPageText)
	{
		int keywordIndex = onPageText.toLowerCase().indexOf(mainKeyword);

		int lastSentenceEnd = 0;

		for (int i = 0; i < keywordIndex; i++)
		{
			if (onPageText.charAt(i) == '.' || onPageText.charAt(i) == '!' || onPageText.charAt(i) == '?')
			{
				lastSentenceEnd = i;
				lastSentenceEnd++;
			}
		}

		String metaDescription = onPageText.substring(lastSentenceEnd).trim();

		return metaDescription;
	}

	/**
	 * Method detects language of given query string.
	 * 
	 * @param query
	 * @return QWLocale-String
	 */
	private short detectLanguage(String query)
	{
		/**
		 * TODO
		 */
		return QWLocale.GERMAN;
	}

	private ArrayList<String> getSearchIndexRoutings(String[] keywords)
	{
		ArrayList<String> routings = new ArrayList<String>();

		for (int i = 0; i < keywords.length; i++)
		{
			routings.add(rg.getSearchIndexRouting((keywords[i])));
			// ask max 3 indicies
			if (i >= 3)
				break;
		}
		return routings;
	}

	private void setUpDebugModeIfNecessary(String query)
	{
		switch (debugmode)
		{
		case 0:
			debugOutPut = ", DebugMode 0: Default search. MSM: 85%. Search: " + query;
			searchScript = "_score + doc['searchscore'].value";
			break;
		case 1:
			minShouldMatch = "90%";
			debugOutPut = ", DebugMode 1: Default search + MSM. MSM: 90%. Search: " + query;
			searchScript = "_score + doc['searchscore'].value";
			break;
		case 2:
			minShouldMatch = "85%";
			debugOutPut = ", DebugMode 2: Text only search. MSM: 85%. Search: " + query;
			searchScript = null;
			break;
		case 3:
			minShouldMatch = "90%";
			debugOutPut = ", DebugMode 3: Text only search. MSM: 90%. Search: " + query;
			searchScript = null;
			break;
		case 4:
			minShouldMatch = "85%";
			debugOutPut = ", DebugMode 4: QualityScore*3,Varietytopicscore*1. MSM: 85%. Search: " + query;
			searchScript = "_score +(doc['qualityscore'].value * 3) * doc['varietytopicscore'].value";
			break;
		case 5:
			minShouldMatch = "90%";
			debugOutPut = ", DebugMode 4: QualityScore*3,Varietytopicscore*1. MSM: 90%. Search: " + query;
			searchScript = "_score +(doc['qualityscore'].value * 3) * doc['varietytopicscore'].value";
			break;
		default:
			minShouldMatch = "85%";
			debugOutPut = "";
			searchScript = "_score + doc['searchscore'].value";
			break;
		}
	}

	// refactoring necessary....
	// put this to shared project

	private static Map sortByComparator(Map unsortMap)
	{

		List list = new LinkedList(unsortMap.entrySet());

		// sort list based on comparator
		Collections.sort(list, new Comparator()
		{
			public int compare(Object o1, Object o2)
			{
				return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue());
			}
		});

		// put sorted list into map again
		// LinkedHashMap make sure order in which keys were inserted
		Map sortedMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();)
		{
			Map.Entry entry = (Map.Entry) it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
}
