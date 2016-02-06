package org.nunn.gephiserver.system;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlTools {

	public static List<Map<String, Object>> convertResultSetToList(ResultSet rs) throws SQLException {
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

	public static Map<String, Object> convertResultSetToSingle(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		Map<String, Object> row = new HashMap<String, Object>(columns);

		if (rs.next()) {
			for (int i = 1; i <= columns; i++) {
				row.put(md.getColumnName(i), rs.getObject(i));
			}
		}

		return row;
	}

}
