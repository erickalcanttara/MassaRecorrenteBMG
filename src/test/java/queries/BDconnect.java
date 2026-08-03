package queries;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class BDconnect {

    private final DatabaseProperties.SqlServer cfg;

    public BDconnect() {
        DatabaseProperties props = new DatabaseProperties();
        if (props.getSqlServer() == null) {
            throw new IllegalStateException("Configuração database.sql_server não encontrada no application.yaml");
        }
        this.cfg = props.getSqlServer();
    }

    private String buildJdbcUrl() {
        String host = cfg.host != null ? cfg.host : "localhost";
        System.out.println(host);
        int port = cfg.port != null ? cfg.port : 1433;
        String db = cfg.db_schema != null ? cfg.db_schema : "";
        // SQL Server JDBC URL
        return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, db);
    }

    public Connection getConnection() throws SQLException {
        String url = buildJdbcUrl();
        // carrega driver opcionalmente
        try {
            // driver padrão Microsoft
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ignored) {
            // se driver não estiver no classpath, DriverManager tenta automaticamente com URL adequado
        }
        return DriverManager.getConnection(url, cfg.username, cfg.password);
    }

}
