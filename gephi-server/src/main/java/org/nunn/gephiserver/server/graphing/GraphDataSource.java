package org.nunn.gephiserver.server.graphing;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nunn.gephiserver.server.Props;
import org.nunn.gephiserver.server.system.DataSource;

public class GraphDataSource {
	
	private final DataSource dataSource;

	private final String catalog;
	private final String schema;
	
	private final String listGraphs;
	private final String selectGraphByID;
	private final String selectNodeByGraphID;
	private final String selectEdgeByGraphID;
	
	public GraphDataSource() {
		this.catalog = Props.INSTANCE.getPropertyAsString("graphCatalog", (String) null);
		
		this.schema = Props.INSTANCE.getPropertyAsString("graphSchema", "gephi");
		if (this.schema.isEmpty()) {
			throw new IllegalArgumentException("Application property [graphSchema] for database schema name is required");
		}
		
		String catschema = this.catalog != null && ! this.catalog.isEmpty() ? this.catalog + "." + this.schema : this.schema;
		
		this.listGraphs = "select pk_id, title"
							+ " from " + catschema + ".graph";
		this.selectGraphByID = "select pk_id, title, creator, directed, up_weight, down_weight, url_base"
								+ " from " + catschema + ".graph"
								+ " where pk_id = ?";
		this.selectNodeByGraphID = "select pk_num, pk_graph, title, tag"
									+ " from " + catschema + ".node"
									+ " where pk_graph = ?";
		this.selectEdgeByGraphID = "select pk_graph, pk_num, source_node, target_node, val"
									+ " from " + catschema + ".edge"
									+ " where pk_graph = ?";
		
		this.dataSource = new DataSource("gephiserver." + catschema);
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
	
	public void checkSchema(Connection con) throws SQLException {
		DatabaseMetaData dbmd = con.getMetaData();
		
		try (ResultSet rsSchema = dbmd.getSchemas(catalog, schema)) {
			if ( ! rsSchema.next()) {
				// Some drivers (e.g. MySQL) return database name as the only schema, and schemas as catalogs
				try (ResultSet rsCatalog = dbmd.getCatalogs()) {
					boolean catFound = false;
					while (rsCatalog.next()) {
						if (schema.equals(rsCatalog.getString("TABLE_CAT"))) {
							catFound = true;
							break;
						}
					}
					if ( ! catFound) {
						throw new RuntimeException("Schema " + schema + " missing!");
					}
				}
			}
		}
		
		Set<String> gephiTableNames = new HashSet<>();
		try (ResultSet rs = dbmd.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
			while (rs.next()) {
				String tableName = rs.getString("TABLE_NAME");
				gephiTableNames.add(tableName);
			}
		}
		if ( ! gephiTableNames.containsAll(Arrays.asList("graph", "node", "edge"))) {
			throw new RuntimeException("Tables missing from " + schema + " schema!");
		}
	}

	public Map<String, Object> getGraphByID(Connection con, Integer id) throws SQLException {
		Map<String, Object> result;
		try (PreparedStatement ps = con.prepareStatement(selectGraphByID)) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				result = dataSource.convertResultSetToSingleMap(rs);
			}
		}
		return result;
	}
	
	@FunctionalInterface
	public static interface NodeComsumer { 
		public boolean push(Integer num, String name, String tag);
	}
	
	public void populateNodesForGraph(Connection con, Integer graphId, NodeComsumer consumer) throws SQLException {
		boolean originalAutoCommit = con.getAutoCommit();
		con.setAutoCommit(false);
		
		try (PreparedStatement ps = con.prepareStatement(selectNodeByGraphID)) {
			ps.setInt(1, graphId);
			ps.setFetchSize(dataSource.getCursorFetchSize());
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					if ( ! consumer.push(rs.getInt("pk_num"), rs.getString("title"), rs.getString("tag"))) {
						break;
					}
				}
			}
		}
		finally {
			con.setAutoCommit(originalAutoCommit);
		}
	}
	
	@FunctionalInterface
	public static interface EdgeComsumer { 
		public boolean push(Integer num, Integer source, Integer target, Float val);
	}
	
	public void populateEdgesForGraph(Connection con, Integer graphId, EdgeComsumer consumer) throws SQLException {
		boolean originalAutoCommit = con.getAutoCommit();
		con.setAutoCommit(false);
		
		try (PreparedStatement ps = con.prepareStatement(selectEdgeByGraphID)) {
			ps.setInt(1, graphId);
			ps.setFetchSize(dataSource.getCursorFetchSize());
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					if ( ! consumer.push(rs.getInt("pk_num"), rs.getInt("source_node"), rs.getInt("target_node"), rs.getFloat("val"))) {
						break;
					}
				}
			}
		}
		finally {
			con.setAutoCommit(originalAutoCommit);
		}
	}

	public Map<Integer, String> listGraphs(Connection con) throws SQLException {
		Map<Integer, String> result = new HashMap<>();
		try (PreparedStatement ps = con.prepareStatement(listGraphs)) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					result.put(rs.getInt("pk_id"), rs.getString("title"));
				}
			}
		}
		return result;
	}
	
}