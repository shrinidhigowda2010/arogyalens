package com.arogyalens.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Web configuration: CORS from {@code ALLOWED_ORIGINS}, the outbound HTTP client with explicit
 * timeouts, and the single-page-app fallback so deep links like /results load index.html.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ArogyaLensProperties properties;

    public WebConfig(ArogyaLensProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.cors().origins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "X-Device-Id")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(org.springframework.http.CacheControl.maxAge(Duration.ofDays(365)).cachePublic());
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    /** Outbound client for Google APIs with connect and read timeouts. */
    @Bean
    public RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(Math.max(5_000, properties.ai().timeoutMs())));
        return RestClient.builder().requestFactory(factory);
    }

    /** Serves real static files, and index.html for any other non-API path (client-side routes). */
    static final class SpaFallbackResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (!resourcePath.isEmpty() && !resourcePath.endsWith("/")
                    && requested.exists() && requested.isReadable()) {
                return requested;
            }
            if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")
                    || resourcePath.contains(".")) {
                return null;
            }
            Resource index = new ClassPathResource("/static/index.html");
            return index.exists() ? index : null;
        }
    }
}
