package org.nunn.gephiserver.server.system;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nunn.gephiserver.server.Props;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataSource {
	
	private static final Logger LOGGER = LogManager.getLogger(DataSource.class);
	
	private final HikariDataSource ds;
	private final int cursorFetchSize;
	private final int databaseMajorVersion;
	private final int databaseMinorVersion;
	private final String databaseProductName;
	private final String databaseProductVersion;
	
	public DataSource(String name) {
		this.cursorFetchSize = Props.INSTANCE.getPropertyAsInteger("cursorFetchSize", 50);
		
		HikariConfig config = new HikariConfig();
		
		config.setPoolName(name);
		config.setDataSourceClassName(Props.INSTANCE.getPropertyAsString("dataSource.className", ()->{throw new RuntimeException("No dataSourceClassName set!");}));
		config.setMaximumPoolSize(Props.INSTANCE.getPropertyAsInteger("dataSource.maximumPoolSize", 5));
		config.setConnectionTimeout(Props.INSTANCE.getPropertyAsLong("dataSource.connectionTimeout", 5000L));
		
		addDataSourceProperties(config);

		ds = new HikariDataSource(config);
		
		try (Connection con = ds.getConnection()) {
			DatabaseMetaData dbmd = con.getMetaData();
			this.databaseMajorVersion = dbmd.getDatabaseMajorVersion();
			this.databaseMinorVersion = dbmd.getDatabaseMinorVersion();
			this.databaseProductName = dbmd.getDatabaseProductName();
			this.databaseProductVersion = dbmd.getDatabaseProductVersion();
		}
		catch (SQLException e) {
			throw new RuntimeException("Failed to get database connection on initialisation.", e);
		}
	}
	
	private void addDataSourceProperties(HikariConfig config) {
		int length = "dataSourceProperty.".length();
		Map<String, String> properties = Props.INSTANCE.getAllPropertiesMatching(Pattern.compile("^dataSourceProperty\\..+$"));
		for (Entry<String, String> property : properties.entrySet()) {
			String shortName = property.getKey().substring(length);
			config.addDataSourceProperty(shortName, property.getValue());
		}
	}
	
	public Connection getConnection() throws SQLException {
		return ds.getConnection();
	}
	
	public void close() {
		LOGGER.info("Closing data source pool {}.", ds.getPoolName());
		ds.close();
	}
	
	public void deregisterJdbcDrivers() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		
		while (drivers.hasMoreElements()) {
		    Driver driver = drivers.nextElement();
		    
		    if (driver.getClass().getClassLoader() == cl) {
		        // This driver was registered by the webapp's ClassLoader, so deregister it:
		        try {
		            LOGGER.info("Deregistering JDBC driver {}", driver);
		            DriverManager.deregisterDriver(driver);
		        }
		        catch (SQLException ex) {
		        	LOGGER.error("Error deregistering JDBC driver {}", driver, ex);
		        }
		    }
		    else {
		        // driver was not registered by the webapp's ClassLoader and may be in use elsewhere
		    	LOGGER.debug("Not deregistering JDBC driver {} as it does not belong to this webapp's ClassLoader", driver);
		    }
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s %s connected to %s %s", ds.getClass().getSimpleName(), ds.getPoolName(), databaseProductName, databaseProductVersion);
	}
	
	public List<Map<String, Object>> convertResultSetToList(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		List<Map<String, Object>> list = new ArrayList<>();

		while (rs.next()) {
			HashMap<String, Object> row = new HashMap<>(columns);
			for (int i = 1; i <= columns; i++) {
				row.put(md.getColumnName(i), rs.getObject(i));
			}
			list.add(row);
		}

		return list;
	}

	public Map<String, Object> convertResultSetToSingleMap(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		Map<String, Object> row = new HashMap<>(columns);

		if (rs.next()) {
			for (int i = 1; i <= columns; i++) {
				row.put(md.getColumnName(i), rs.getObject(i));
			}
		}

		return row;
	}

	public int getCursorFetchSize() {
		return cursorFetchSize;
	}

	public int getDatabaseMajorVersion() {
		return databaseMajorVersion;
	}

	public int getDatabaseMinorVersion() {
		return databaseMinorVersion;
	}

	public String getDatabaseProductName() {
		return databaseProductName;
	}

	public String getDatabaseProductVersion() {
		return databaseProductVersion;
	}

}