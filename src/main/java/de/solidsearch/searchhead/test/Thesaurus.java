package de.solidsearch.searchhead.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.de.GermanMinimalStemFilter;
import org.apache.lucene.analysis.de.GermanNormalizationFilter;
import org.apache.lucene.analysis.sinks.TokenTypeSinkFilter;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.elasticsearch.search.lookup.IndexField;

public class Thesaurus
{

	public static void main(String[] args)
	{
		try
		{
			createIndex();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

	public static final String FILES_TO_INDEX_DIRECTORY = "filesToIndex";

	public static final String FIELD_PATH = "path";
	public static final String FIELD_CONTENTS = "contents";

	private static Directory ramDirectory = new RAMDirectory();

	public static void createIndex() throws CorruptIndexException, LockObtainFailedException, IOException
	{
		IndexWriter writer = new IndexWriter(ramDirectory, new IndexWriterConfig(Version.LUCENE_47, new StandardAnalyzer(Version.LUCENE_47)));

		String csvFile = "D:/openthesaurus.txt";
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ";";

		ArrayList<String> keywords = new ArrayList<String>();

		try
		{

			br = new BufferedReader(new FileReader(csvFile));
			while ((line = br.readLine()) != null)
			{
				
				// use comma as separator
				String[] country = line.split(cvsSplitBy);
				
				StringBuffer tokens = new StringBuffer();
				
				for (int i = 0; i < country.length; i++)
				{
					// remove brackets like (hallo)
					tokens.append(country[i].replaceAll("\\(.+?\\)", "")).append(" ");
				}
				Document doc = new Document();
				
				TokenStream tokenStream = new ClassicTokenizer(Version.LUCENE_47, new StringReader(tokens.toString()));
				
				tokenStream = new LowerCaseFilter(Version.LUCENE_47,tokenStream);
//			tokenStream = new GermanNormalizationFilter(tokenStream);
//				tokenStream = new GermanMinimalStemFilter(tokenStream);	
				
				doc.add(new TextField("syn", tokenStream));
				doc.add(new StringField("tokens", tokens.toString(),Store.YES));
				writer.addDocument(doc);
			}
			writer.close();

			System.out.println(">>Search for : " + "quatsch");
			searchIndex("Quatsch");
			
			System.out.println(">>Search for : " + "vorhang");
			searchIndex("Vorhang");
			
			System.out.println(">>Search for : " + "reden");
			searchIndex("reden");
		
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (br != null)
			{
				try
				{
					br.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	
	public static void searchIndex(String searchString) throws IOException, ParseException {
		System.out.println("Searching for '" + searchString + "'");
		IndexReader indexReader = IndexReader.open(ramDirectory);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
		
		QueryParser queryParser = new QueryParser(Version.LUCENE_47,"syn", analyzer);
		

		Query query = queryParser.parse(tokenizeString(analyzer,searchString).get(0));
		TopDocs topDocs = indexSearcher.search(query,10);
		System.out.println("Number of hits: " + topDocs.totalHits);

		for ( ScoreDoc scoreDoc : topDocs.scoreDocs ) {
		    Document doc = indexSearcher.doc( scoreDoc.doc );
		    System.out.println(doc.get("tokens"));
		}
		indexReader.close();
	}
	
	  public static List<String> tokenizeString(Analyzer analyzer, String string) {
		    List<String> result = new ArrayList<String>();
		    try {
		      TokenStream stream  = analyzer.tokenStream(null, new StringReader(string));
		      stream.reset();
		      
		      stream = new LowerCaseFilter(Version.LUCENE_47,stream);
//				stream = new GermanNormalizationFilter(stream);
//				stream = new GermanMinimalStemFilter(stream);	
				
		      while (stream.incrementToken()) {
		        result.add(stream.getAttribute(CharTermAttribute.class).toString());
		      }
		      
		      stream.close();
		      
		    } catch (IOException e) {
		      // not thrown b/c we're using a string reader...
		      throw new RuntimeException(e);
		    }
		    return result;
		  }
}
