package util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Opens connections to the single hotel_management database. */
public class DatabaseConnection {
    private final String url;
    private final String user;
    private final String password;

    /** Reads credentials from the environment, with optional ignored config.properties for local use. */
    public DatabaseConnection() throws IOException {
        Properties settings = new Properties();
        Path localConfig = Path.of("config.properties");
        if (Files.isRegularFile(localConfig)) {
            try (InputStream file = Files.newInputStream(localConfig)) {
                settings.load(file);
            }
        }
        url = setting("DB_URL", settings.getProperty("db.url",
                "jdbc:mysql://localhost:3306/hotel_management?serverTimezone=UTC"));
        user = setting("DB_USER", settings.getProperty("db.user", "root"));
        password = setting("DB_PASSWORD", settings.getProperty("db.password", ""));
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }

    /** Creates one connection for a database operation. */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
