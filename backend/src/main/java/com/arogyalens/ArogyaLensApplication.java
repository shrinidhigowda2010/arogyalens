package com.arogyalens;

import com.arogyalens.config.DatabaseUrlResolver;
import com.arogyalens.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point: loads a local .env and DATABASE_URL, then starts Spring Boot. */
@SpringBootApplication
@ConfigurationPropertiesScan
@SuppressWarnings("checkstyle:HideUtilityClassConstructor") // Spring needs a proxyable class.
public class ArogyaLensApplication {

    public static void main(String[] args) {
        DotEnvLoader.load();
        DatabaseUrlResolver.apply();
        SpringApplication.run(ArogyaLensApplication.class, args);
    }
}
