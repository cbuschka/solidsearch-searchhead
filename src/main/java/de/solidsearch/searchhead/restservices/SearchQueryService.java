package de.solidsearch.searchhead.restservices;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import de.solidsearch.searchhead.restservices.dao.SearchQueryManager;
import de.solidsearch.searchhead.utils.HeadConfig;


@Controller
public class SearchQueryService
{	
	@Autowired
	SearchQueryManager queryManager;
	@Autowired
	HeadConfig config;
	
	// watch-out: parameter are case sensitive!!
	// Example: http://localhost:8080/searchhead/rest/search?q=hose&s=0
	// q = search query
	// s = start with search result
	// dk = debugkey
	@RequestMapping(value = "/search", method = RequestMethod.GET, produces = "text/html;charset=UTF-8")
	public @ResponseBody ResponseEntity<String> getKeywordsByProject(HttpServletRequest request, @RequestParam String q, @RequestParam Integer s, WebRequest webParam) {
		
		if (s <= 0 || s >= 100)
		{
			return new ResponseEntity<String>("invailid parameter", HttpStatus.NOT_FOUND);
		}
		
		if (webParam.getParameter("debugkey") !=  null && webParam.getParameter("debugkey").equals(config.DEBUG_KEY))
		{
			int mode = 0;
			
			if (webParam.getParameter("debugmode") != null)
			{
				try
				{
					mode = Integer.parseInt(webParam.getParameter("debugmode"));
				}
				catch (Exception e)
				{
					return new ResponseEntity<String>("invailid parameter", HttpStatus.NOT_FOUND);
				}			
			}
			
			return new ResponseEntity<String>(queryManager.getSearchResult(q,s,mode,request), HttpStatus.OK);
		}
		else
		{
			return new ResponseEntity<String>(queryManager.getSearchResult(q,s,-1,request), HttpStatus.OK);
		}
	}
	
}
