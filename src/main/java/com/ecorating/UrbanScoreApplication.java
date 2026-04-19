package com.ecorating;

import com.ecorating.config.AppProperties;
import com.ecorating.config.DeepSeekProperties;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({DeepSeekProperties.class, AppProperties.class})
public class UrbanScoreApplication {

    public static void main(String[] args) {
        loadEnvFileIntoSystemProperties();
        SpringApplication.run(UrbanScoreApplication.class, args);
    }

    /**
     * Локальный запуск из IDE: подхватывает ключи из файла {@code .env} в рабочей директории.
     * В Docker переменные уже в {@code System.getenv} — их не перезаписываем.
     */
    private static void loadEnvFileIntoSystemProperties() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
            dotenv.entries().forEach(e -> {
                String key = e.getKey();
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, e.getValue());
                }
            });
        } catch (Exception ignored) {
            // нет .env — нормально для CI/Docker-only
        }
    }
}
