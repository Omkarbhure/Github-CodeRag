package com.example.coderag.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:#{null}}")
    private String springDatasourceUrl;

    @Value("${DATABASE_URL:#{null}}")
    private String databaseUrl;

    @Value("${POSTGRES_URL:#{null}}")
    private String postgresUrl;

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:5433}")
    private String dbPort;

    @Value("${DB_NAME:coderag}")
    private String dbName;

    @Value("${spring.datasource.username:${DB_USER:postgres}}")
    private String dbUser;

    @Value("${spring.datasource.password:${DB_PASSWORD:postgres}}")
    private String dbPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String rawUrl = springDatasourceUrl;
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            rawUrl = databaseUrl;
        }
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            rawUrl = postgresUrl;
        }

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");

        if (rawUrl != null && !rawUrl.trim().isEmpty()) {
            rawUrl = rawUrl.trim();
            log.info("Configuring DataSource from environment URL: {}", maskUrl(rawUrl));

            // Case 1: Standard postgres:// or postgresql:// URI (Render / Supabase / Railway / Heroku format)
            if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
                try {
                    String uriString = rawUrl.startsWith("postgres://")
                            ? "postgresql://" + rawUrl.substring("postgres://".length())
                            : rawUrl;

                    URI uri = URI.create(uriString);
                    String host = uri.getHost();
                    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    String path = uri.getPath();
                    String dbNameParsed = (path != null && path.length() > 1) ? path.substring(1) : "coderag";

                    String userInfo = uri.getUserInfo();
                    String user = dbUser;
                    String password = dbPassword;
                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        user = parts[0];
                        password = parts[1];
                    } else if (userInfo != null) {
                        user = userInfo;
                    }

                    String query = uri.getQuery();
                    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbNameParsed;
                    if (query != null && !query.isEmpty()) {
                        jdbcUrl += "?" + query;
                    }

                    config.setJdbcUrl(jdbcUrl);
                    config.setUsername(user);
                    config.setPassword(password);
                    log.info("Converted PostgreSQL URI to JDBC URL: {}", jdbcUrl);
                } catch (Exception e) {
                    log.warn("Failed to parse URI format, using raw jdbc prefix", e);
                    config.setJdbcUrl("jdbc:" + rawUrl);
                    config.setUsername(dbUser);
                    config.setPassword(dbPassword);
                }
            } else if (rawUrl.startsWith("jdbc:postgresql://")) {
                config.setJdbcUrl(rawUrl);
                config.setUsername(dbUser);
                config.setPassword(dbPassword);
            } else {
                config.setJdbcUrl("jdbc:postgresql://" + rawUrl);
                config.setUsername(dbUser);
                config.setPassword(dbPassword);
            }
        } else {
            // Local fallback
            String jdbcUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
            log.info("No explicit database URL provided, using host/port configuration: {}", jdbcUrl);
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }

    private String maskUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll(":[^:@]+@", ":****@");
    }
}
