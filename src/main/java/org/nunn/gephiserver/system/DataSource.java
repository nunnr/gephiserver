package org.nunn.gephiserver.system;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nunn.gephiserver.Props;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataSource {
	
	private static final Logger LOGGER = LogManager.getLogger(DataSource.class);
	
	private final HikariDataSource ds;
	
	public DataSource(String name) {
		HikariConfig config = new HikariConfig();
		
		config.setPoolName(name);
		config.setInitializationFailFast(true);
		config.setDataSourceClassName(Props.INSTANCE.getPropertyAsString("dataSource.className", ()->{throw new RuntimeException("No dataSourceClassName set!");}));
		config.setMaximumPoolSize(Props.INSTANCE.getPropertyAsInteger("dataSource.maximumPoolSize", 5));
		config.setConnectionTimeout(Props.INSTANCE.getPropertyAsLong("dataSource.connectionTimeout", 5000L));
		
		addDataSourceProperties(config);

		ds = new HikariDataSource(config);
	}
	
	private void addDataSourceProperties(HikariConfig config) {
		Pattern pattern = Pattern.compile("^dataSourceProperty\\..+$");
		int length = "dataSourceProperty.".length();
		Map<String, String> properties = Props.INSTANCE.getAllPropertiesMatching(pattern);
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
		Connection con = null;
		try {
			con = ds.getConnection();
			DatabaseMetaData dbmd = con.getMetaData();
			return String.format("HikariCP pool %s connected to %s %s", ds.getPoolName(), dbmd.getDatabaseProductName(), dbmd.getDatabaseProductVersion());
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			if (con != null) {
				try {
					con.close();
				}
				catch (SQLException e1) {
					LOGGER.warn("Connection close failed!");
				}
			}
		}
	}
	
}