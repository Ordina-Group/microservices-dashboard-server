package be.ordina.msdashboard.caching;

import be.ordina.msdashboard.model.Node;
import be.ordina.msdashboard.services.RedisService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CachingConfigTest {

    @InjectMocks
    private CachingConfig cachingConfig;

    @Mock
    private CachingProperties cachingProperties;

    @Test
    @SuppressWarnings("unchecked")
    public void testCacheManager() {
        RedisTemplate<String, Node> redisTemplate = mock(RedisTemplate.class);

        when(cachingProperties.getDefaultExpiration()).thenReturn(30L);
        when(cachingProperties.getRedisCachePrefix()).thenReturn("prefix");

        RedisCacheManager redisCacheManager = cachingConfig.cacheManager(redisTemplate);

        assertThat(redisCacheManager).isNotNull();
    }

    @Test
    public void testCacheCleaningBeanNonEvict() {
        RedisService redisService = mock(RedisService.class);

        when(cachingProperties.isEvict()).thenReturn(false);

        CacheCleaningBean cacheCleaningBean = cachingConfig.cacheCleaningBean(redisService);

        assertThat(cacheCleaningBean).isNotNull();

        verify(redisService, times(0)).evictHealthsCache();
        verify(redisService, times(0)).evictIndexesCache();
    }

    @Test
    public void testCacheCleaningBeanEvict() {
        RedisService redisService = mock(RedisService.class);

        when(cachingProperties.isEvict()).thenReturn(true);

        CacheCleaningBean cacheCleaningBean = cachingConfig.cacheCleaningBean(redisService);

        assertThat(cacheCleaningBean).isNotNull();

        verify(redisService, times(1)).evictHealthsCache();
        verify(redisService, times(1)).evictIndexesCache();
    }

}
