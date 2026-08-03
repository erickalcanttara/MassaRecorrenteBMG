package queries;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class DatabaseProperties {

    public static class SqlServer {
        public String host;
        public Integer port;
        public String username;
        public String password;
        public String db_schema;
        public String dialect;
    }

    private SqlServer sqlServer;

    public DatabaseProperties() {
        loadFromClasspath("application.yaml");
    }

    @SuppressWarnings("unchecked")
    private void loadFromClasspath(String resource) {
        Yaml yaml = new Yaml();
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalStateException(resource + " não encontrado no classpath");
            }
            Map<String, Object> root = yaml.load(is);
            Object databaseObj = root.get("database");
            if (databaseObj instanceof Map) {
                Map<String, Object> database = (Map<String, Object>) databaseObj;
                Object sqlServerObj = database.get("sql_server");
                if (sqlServerObj instanceof Map) {
                    Map<String, Object> ss = (Map<String, Object>) sqlServerObj;
                    SqlServer s = new SqlServer();
                    s.host = getString(ss, "host");
                    s.port = getInt(ss, "port");
                    s.username = getString(ss, "username");
                    s.password = getString(ss, "password");
                    s.db_schema = getString(ss, "db_schema");
                    s.dialect = getString(ss, "dialect");
                    this.sqlServer = s;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao carregar configurações do YAML: " + e.getMessage(), e);
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : v.toString();
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException ex) { return null; }
    }

    public SqlServer getSqlServer() {
        return sqlServer;
    }
}
