package gw.redisdemo.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.javafx.collections.MappingChange;
import org.assertj.core.util.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Configuration
@EnableCaching
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableConfigurationProperties({CacheProperties.class, CustomCacheProperties.class})
@ConditionalOnClass({CacheProperties.Redis.class, RedisCacheConfiguration.class})
public class RedisConfig extends CachingConfigurerSupport {
    @Autowired
    private CacheProperties cacheProperties;

    /**
     *      keyGenerator为默认的key生成策略,如果要在@Cacheable注解中指定keyGenerator
     *      如: @Cacheable(value = "key", keyGenerator = "cacheKeyGenerator")
     *      就定义不同的函数名称，就不要加@Override
     */
    @Bean
    //@Override
    public KeyGenerator cacheKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getName());
            sb.append("." + method.getName() + "(");
            boolean bFirst = true;
            for (Object obj : params) {
                if (bFirst) {
                    bFirst = false;
                } else {
                    sb.append(',');
                }
                sb.append(obj.toString());
            }
            sb.append(')');
            return sb.toString();
        };
    }


//    @Bean
//    @SuppressWarnings("unchecked")
//    public RedisTemplate<String, String> redisTemplate(LettuceConnectionFactory factory) {
//        StringRedisTemplate template = new StringRedisTemplate(factory);
//        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
//        ObjectMapper om = new ObjectMapper();
//        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
//        jackson2JsonRedisSerializer.setObjectMapper(om);
//        template.setValueSerializer(jackson2JsonRedisSerializer);
//        template.afterPropertiesSet();
//        return template;
//    }
//    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory factory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(factory);
//        //以下代码为将RedisTemplate的Value序列化方式由JdkSerializationRedisSerializer更换为Jackson2JsonRedisSerializer
//        //此种序列化方式结果清晰、容易阅读、存储字节少、速度快，所以推荐更换
//        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = getJackson2JsonRedisSerializer();
//
//        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
//
//        // key采用String的序列化方式
//        template.setKeySerializer(stringRedisSerializer);
//        // hash的key也采用String的序列化方式
//        template.setHashKeySerializer(stringRedisSerializer);
//        // value序列化方式采用jackson
//        template.setValueSerializer(jackson2JsonRedisSerializer);
//        // hash的value序列化方式采用jackson
//        template.setHashValueSerializer(jackson2JsonRedisSerializer);
//
//        template.afterPropertiesSet();
//
//        return template;
//    }

    // 超时配置，不会影响直接用RedisTemplate的调用
    @Bean
    public RedisCacheManager redisCacheManager(CustomCacheProperties customCacheProperties, RedisConnectionFactory factory) {
        RedisCacheConfiguration configuration = getDefaultRedisCacheConfiguration();
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.RedisCacheManagerBuilder
                .fromCacheWriter(RedisCacheWriter.lockingRedisCacheWriter(factory))
                .cacheDefaults(configuration);

        Map<String, RedisCacheConfiguration> mapConfigs = new HashMap<>();
        Optional.ofNullable(customCacheProperties)
                .map(p -> p.getCustomCache())
                .ifPresent(customCache -> {
                    customCache.forEach((key, cache) -> {
                       RedisCacheConfiguration cfg = handleRedisCacheConfiguration(cache, configuration);
                       mapConfigs.put(key, cfg);
                    });
                });

        builder.withInitialCacheConfigurations(mapConfigs);
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private Jackson2JsonRedisSerializer getJackson2JsonRedisSerializer() {
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        return  jackson2JsonRedisSerializer;
    }

    @SuppressWarnings("unchecked")
    private RedisCacheConfiguration getDefaultRedisCacheConfiguration() {
        CacheProperties.Redis redis = cacheProperties.getRedis();
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();

        // 配置序列化的方式之后，不需要自定义RedisTemplate
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = getJackson2JsonRedisSerializer();
        config = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer));
        config = handleRedisCacheConfiguration(redis, config);
        return config;
    }

    private RedisCacheConfiguration handleRedisCacheConfiguration(CacheProperties.Redis redis, RedisCacheConfiguration config) {
        if (Objects.isNull(redis)) {
            return config;
        }

        if ( redis.getTimeToLive() != null ) {
            config = config.entryTtl(redis.getTimeToLive());
        }

        if ( redis.getKeyPrefix() != null ) {
            config = config.computePrefixWith(cacheName -> cacheName + redis.getKeyPrefix());
        }

        if (!redis.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }

        if (!redis.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }

        return config;
    }
}
