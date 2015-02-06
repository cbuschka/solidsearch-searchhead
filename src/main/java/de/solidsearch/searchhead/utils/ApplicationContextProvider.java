package de.solidsearch.searchhead.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component("contextApplicationContextProvider")
public class ApplicationContextProvider implements ApplicationContextAware
{

	public void setApplicationContext(ApplicationContext ctx) throws BeansException
	{
		// Wiring the ApplicationContext into a static method
		AppContext.setApplicationContext(ctx);
	}
}
