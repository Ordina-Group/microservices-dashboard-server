/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.ordina.msdashboard.config;

import be.ordina.msdashboard.cache.CacheProperties;
import be.ordina.msdashboard.config.RedisConfiguration.RedisOrMockCondition;
import be.ordina.msdashboard.nodes.stores.RedisStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cloud.client.discovery.noop.NoopDiscoveryClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.DefaultRedisCachePrefix;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCachePrefix;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

import static redis.embedded.util.OS.WINDOWS;
import static redis.embedded.util.OSDetector.getOS;

/**
 * Auto-configuration for redis specific beans.
 *
 * @author Andreas Evers
 * @author Tim Ysewyn
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties
@Conditional(RedisOrMockCondition.class)
@AutoConfigureBefore(WebConfiguration.class)
@AutoConfigureAfter({ RedisAutoConfiguration.class, NoopDiscoveryClientAutoConfiguration.class})
@SuppressWarnings("SpringJavaAutowiringInspection")
public class RedisConfiguration {

    @ConfigurationProperties("spring.cache")
    @Bean
    public CacheProperties cacheProperties() {
        return new CacheProperties();
    }

    @Bean(name = {"nodeStore", "nodeCache"})
    public RedisStore nodeStore(final RedisConnectionFactory factory) {
        return new RedisStore(redisTemplate(factory), factory);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(final RedisConnectionFactory factory){
        RedisTemplate<String, Object> virtualNodeTemplate = new RedisTemplate<>();
        virtualNodeTemplate.setConnectionFactory(factory);
        virtualNodeTemplate.setKeySerializer(new StringRedisSerializer());
        virtualNodeTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return virtualNodeTemplate;
    }

    @Bean
    @ConditionalOnProperty("redis.mock")
    public InMemoryRedis inMemoryRedis() {
        return new InMemoryRedis();
    }

    private class InMemoryRedis {

        @Value("${spring.redis.port:6379}")
        private int redisPort;

        private RedisServer redisServer;

        @PostConstruct
        public void startRedis() throws IOException {
            if (WINDOWS == getOS()) {
                redisServer = RedisServer.builder().setting("maxheap 512Mb").port(redisPort).build();
            } else {
                redisServer = new RedisServer(redisPort);
            }
            redisServer.start();
        }

        @PreDestroy
        public void stopRedis() {
            redisServer.stop();
        }
    }

    @Bean
    public RedisCacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
        RedisCacheManager cacheManager = new RedisCacheManager(redisTemplate);
        cacheManager.setDefaultExpiration(cacheProperties().getDefaultExpiration());
        RedisCachePrefix redisCachePrefix = new DefaultRedisCachePrefix();
        redisCachePrefix.prefix(cacheProperties().getRedisCachePrefix());
        cacheManager.setCachePrefix(redisCachePrefix);
        return cacheManager;
    }

    @Bean
    public KeyGenerator simpleKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getName());
            sb.append(method.getName());
            for (Object obj : params) {
                sb.append(obj.toString());
            }
            return sb.toString();
        };
    }

    static class RedisOrMockCondition extends AnyNestedCondition {

        RedisOrMockCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty("spring.redis.host")
        static class SpringRedisCondition {}

        @ConditionalOnProperty("redis.mock")
        static class MockRedisCondition {}

    }
}
