package com.arogyalens;

import com.arogyalens.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ArogyaLensApplication {

    public static void main(String[] args) {
        DotEnvLoader.load();
        SpringApplication.run(ArogyaLensApplication.class, args);
    }
}
