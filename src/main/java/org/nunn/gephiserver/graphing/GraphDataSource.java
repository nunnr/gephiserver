package org.nunn.gephiserver.graphing;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nunn.gephiserver.system.DataSource;

public class GraphDataSource {
	
	private static final Logger LOGGER = LogManager.getLogger(GraphDataSource.class);
	
	private final DataSource dataSource;
	
	public GraphDataSource() {
		this.dataSource = new DataSource("GephiServerPool");
	}
	
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}
	
	public void close() {
		dataSource.close();
	}

	public void deregisterJdbcDrivers() {
		dataSource.deregisterJdbcDrivers();
	}
	
	@Override
	public String toString() {
		return dataSource.toString();
	}
	
	public void checkSchema() {
		LOGGER.info("Checking database schema.");
		Connection con = null;
		try {
			con = getConnection();
			
			Set<String> gephiTableNames = new HashSet<>();
			
			ResultSet rs = con.getMetaData().getTables(null, "gephi", "%", new String[]{"TABLE"});
			while (rs.next()) {
				String tableName = rs.getString("TABLE_NAME");
				gephiTableNames.add(tableName);
			}
			
			if ( ! gephiTableNames.containsAll(Arrays.asList("graph", "node", "edge"))) {
				LOGGER.warn("Tables missing from gephi schema!");
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			if (con != null) {
				try {
					con.close();
				}
				catch (SQLException e) {
					LOGGER.error("Error closing database connection.", e);
				}
			}
		}
	}
	
}