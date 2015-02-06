package de.solidsearch.searchhead.utils;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration("HeadConfig")
@PropertySource("classpath:searchhead.properties")
public class HeadConfig implements Serializable
{	

	private static final long serialVersionUID = 4237553687871076384L;

	@Value("${es.clustername}")
	public String ESCLUSTERNAME;
	@Value("${es.host1}")
	public String ESHOST1;
	@Value("${manager.url1")
	public String MANAGERURL1;
	@Value("${manager.remoteKey}")
	public String MANAGER_REMOTEKEY;
	@Value("${debug.key}")
	public String DEBUG_KEY;
	
}
