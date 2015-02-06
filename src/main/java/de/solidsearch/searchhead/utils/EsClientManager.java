package de.solidsearch.searchhead.utils;

import org.apache.log4j.Logger;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component("EsClientManager")
@Scope(value = "singleton")
public class EsClientManager
{	
	private static final Logger logger = Logger.getLogger(EsClientManager.class.getName()); 
	
	private TransportClient client;

	public EsClientManager()
	{

	}
	
	public TransportClient getTransportClient()
	{	
		if (client == null)
		{
			HeadConfig config = (HeadConfig) AppContext.getApplicationContext().getBean("HeadConfig");
			Builder builder = ImmutableSettings.settingsBuilder();
			
			builder.put("cluster.name", config.ESCLUSTERNAME);
			
			Settings settings = builder.build();
			client = new TransportClient(settings);
			client = client.addTransportAddress(new InetSocketTransportAddress(config.ESHOST1, 9300));
			logger.warn("A new ES-client was created...");
		}
		return client;
	}
	
}
