package org.nunn.gephiserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Props {
	
	private static final Logger LOGGER = LogManager.getLogger(Props.class);
	
	private static final String APP_CONF_DIR_PROPERTY_NAME = "configurationDirectory";
	private static final String APP_PROPS_DIRECTORY = "/var/" + Props.class.getPackage().getName();
	private static final String APP_PROPS_FILE_NAME = "application.properties";
	
	private final Properties props;
	private final Path appLocation;
	
	public static final Props INSTANCE = new Props();
	
	private Props() {
		props = new Properties();
		
		URL location = Props.class.getProtectionDomain().getCodeSource().getLocation();
		
		Path loc = null;
		try {
			loc = Paths.get(location.toURI());
			
			BasicFileAttributes attr = Files.readAttributes(loc, BasicFileAttributes.class);
			if ( ! attr.isDirectory()) {
				Path parent = loc.getParent();
				if (parent == null) {
					throw new InvalidPathException(loc.toString(), "Cannot resolve parent of non-directory path.");
				}
				loc = parent;
			}
			
			loadProperties(loc);
		}
		catch (InvalidPathException | URISyntaxException | IOException e) {
			throw new RuntimeException("Application properties file could not be loaded.", e);
		}
		
		appLocation = loc;
	}
	
	private synchronized void loadProperties(Path loc) throws IOException {
		InputStream ins = null;
		try {
			String confDir = System.getProperty(APP_CONF_DIR_PROPERTY_NAME);
			if (confDir == null) {
				confDir = APP_PROPS_DIRECTORY;
			}
			File appPropsSource = new File(confDir + "/" + APP_PROPS_FILE_NAME);
			if (appPropsSource.canRead()) {
				LOGGER.info("Found {}: {}", APP_PROPS_FILE_NAME, appPropsSource);
				ins = new FileInputStream(appPropsSource);
			}
			else {
				appPropsSource = loc.resolve(APP_PROPS_FILE_NAME).toFile();
				if (appPropsSource.canRead()) {
					LOGGER.info("Found {}: {}", APP_PROPS_FILE_NAME, appPropsSource);
					ins = new FileInputStream(appPropsSource);
				}
				else {
					LOGGER.info("Searching classpath for {}...", APP_PROPS_FILE_NAME);
					ClassLoader cl = Thread.currentThread().getContextClassLoader();
					ins = cl.getResourceAsStream(APP_PROPS_FILE_NAME);
				}
			}
			
			if (ins != null) {
				props.load(ins);
			}
			else {
				throw new FileNotFoundException(APP_PROPS_FILE_NAME + " file not found. Default location is in "
						+ APP_PROPS_DIRECTORY + " directory. Specify a custom config directory with "
						+ APP_CONF_DIR_PROPERTY_NAME + " system property.");
			}
		}
		finally {
			if (ins != null) {
				try {
					ins.close();
				}
				catch (IOException e) {
					LOGGER.debug("Close failed", e);
				}
			}
		}
	}
	
	public synchronized void reloadProperties() throws IOException {
		props.clear();
		loadProperties(appLocation);
	}
	
	public Path getAppLocation() {
		return appLocation;
	}
	
	public Map<String, String> getAllProperties() {
		Map<String, String> properties = new HashMap<>();
		Enumeration<Object> keys = props.keys();
		while (keys.hasMoreElements()) {
			String k = keys.nextElement().toString();
			properties.put(k, props.getProperty(k));
		}
		return properties;
	}
	
	public Map<String, String> getAllPropertiesMatching(Pattern pattern) {
		Map<String, String> properties = new HashMap<>();
		Enumeration<Object> keys = props.keys();
		while (keys.hasMoreElements()) {
			String k = keys.nextElement().toString();
			if (pattern.matcher(k).matches()) {
				properties.put(k, props.getProperty(k));
			}
		}
		return properties;
	}
	
	public String getPropertyAsString(String key, String defaultValue) {
		return props.getProperty(key, defaultValue);
	}
	
	public String getPropertyAsString(String key, Supplier<String> defaultValue) {
		String value = props.getProperty(key);
		if (value != null) {
			return value;
		}
		return defaultValue.get();
	}
	
	public Long getPropertyAsLong(String key, Long defaultValue) {
		return getPropertyAsLong(key, ()->{return defaultValue;});
	}
	
	public Long getPropertyAsLong(String key, Supplier<Long> defaultValue) {
		String value = props.getProperty(key);
		if (value != null) {
			try {
				return Long.valueOf(value);
			}
			catch (Exception e) {
				LOGGER.debug("Long property retrieval failure", e);
			}
		}
		return defaultValue.get();
	}
	
	public Integer getPropertyAsInteger(String key, Integer defaultValue) {
		return getPropertyAsInteger(key, ()->{return defaultValue;});
	}
	
	public Integer getPropertyAsInteger(String key, Supplier<Integer> defaultValue) {
		String value = props.getProperty(key);
		if (value != null) {
			try {
				return Integer.valueOf(value);
			}
			catch (Exception e) {
				LOGGER.debug("Integer property retrieval failure", e);
			}
		}
		return defaultValue.get();
	}
	
	public Boolean getPropertyAsBoolean(String key, Boolean defaultValue) {
		return getPropertyAsBoolean(key, ()->{return defaultValue;});
	}
	
	public Boolean getPropertyAsBoolean(String key, Supplier<Boolean> defaultValue) {
		String value = props.getProperty(key);
		if (value != null) {
			try {
				return Boolean.valueOf(value);
			}
			catch (Exception e) {
				LOGGER.debug("Boolean property retrieval failure", e);
			}
		}
		return defaultValue.get();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getPropertyAs(Class<T> type, String key, Supplier<T> defaultValue) {
		String value = props.getProperty(key);
		if (value != null) {
			try {
				Method m = type.getMethod("valueOf", String.class);
				return (T) m.invoke(null, value);
			}
			catch (Exception e) {
				LOGGER.debug("{} property retrieval failure", type.getCanonicalName(), e);
			}
		}
		return defaultValue.get();
	}
	
	public void listProperties(PrintStream out) {
		props.list(out);
	}
	
}
