package missive.server.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Database {

    private static Database instance;
    private final String url;
    private final String user;
    private final String password;

    private Database(String host, int port, String dbName, String user, String password) {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        this.user = user;
        this.password = password;
    }

    public static Database getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Database not initialized, call init() first");
        }
        return instance;
    }

    public static void init(Properties props) {
        String host     = props.getProperty("db.host", "localhost");
        int    port     = Integer.parseInt(props.getProperty("db.port", "3306"));
        String dbName   = props.getProperty("db.name", "missive");
        String user     = props.getProperty("db.user", "missive_user");
        String password = props.getProperty("db.password", "missive_pass");
        instance = new Database(host, port, dbName, user, password);
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static Properties loadServerProperties() throws IOException {
        Properties props = new Properties();
        // check local file first, then classpath
        try (FileInputStream fis = new FileInputStream("server.properties")) {
            props.load(fis);
        }
        return props;
    }
}
