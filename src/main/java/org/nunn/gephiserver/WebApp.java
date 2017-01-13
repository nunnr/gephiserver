package org.nunn.gephiserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nunn.gephiserver.graphing.GraphBuilder;

@WebListener
public class WebApp implements ServletContextListener {
	
	private static final String LOGO =
			"   ______           __    ________                         \n"
		+   "  / ____/__  ____  / /_  /_/  ___/__  ______   _____  _____\n"
		+   " / / __/ _ |/ __ |/ __ |/ /|__ |/ _ |/ ___/ | / / _ |/ ___/\n"
		+   "/ /_/ /  __/ /_/ / / / / /___/ /  __/ /   | |/ /  __/ /    \n"
		+   "|____/|___/ ____/_/ /_/_/_____/|___/_/    |___/|___/_/     \n"
		+   "         /_/                                               \n";
	
	private static final Logger LOGGER = LogManager.getLogger(WebApp.class);
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		if (LOGGER.isInfoEnabled()) {
			StringBuilder stringBuilder = new StringBuilder(512);
			stringBuilder.append("Gephi Server is starting...\n");
			if (LOGGER.isDebugEnabled()) {
				stringBuilder.append(LOGO);
				try {
					ServletContext servletContext = sce.getServletContext();
					InputStream inputStream = servletContext.getResourceAsStream("/META-INF/MANIFEST.MF");
					Manifest manifest = new Manifest(inputStream);
					Attributes attributes = manifest.getMainAttributes();
					stringBuilder.append("\nApplication manifest:");
					attributes
						.entrySet()
						.stream()
						.sorted((o1, o2) -> o1.getKey().toString().compareToIgnoreCase(o2.getKey().toString()))
						.forEachOrdered((entry) -> {stringBuilder.append("\n\t").append(entry.getKey()).append(": ").append(entry.getValue());});
				}
				catch (IOException e) {
					LOGGER.error("Error reading application manifest.", e);
				}
			}
			LOGGER.info(stringBuilder.toString());
		}
		
		GraphBuilder.INSTANCE.checkDatasource();
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		GraphBuilder.INSTANCE.destroy();
	}
	
}
