package gw.redisdemo.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "damon.cache")
@Data
public class CustomCacheProperties {
    private Map<String, CacheProperties.Redis> customCache;
}

