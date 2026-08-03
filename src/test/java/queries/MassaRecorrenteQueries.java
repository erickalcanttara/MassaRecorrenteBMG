package queries;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class MassaRecorrenteQueries {

    private final BDconnect bd = new BDconnect();

    final String sql = "SELECT TOP 1 * FROM CONTAS ORDER BY 1 DESC";

    public Map<String, Object> searchAccounts() {
        try (Connection conn = bd.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.next()) {
                return null;
            }
            return resultSetToMap(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao executar query: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> resultSetToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            String colName = meta.getColumnLabel(i);
            Object value = rs.getObject(i);
            row.put(colName, value);
        }
        return row;
    }

}
