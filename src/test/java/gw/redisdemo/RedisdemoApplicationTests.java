package gw.redisdemo;

import gw.redisdemo.services.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisdemoApplicationTests {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Test
    public void contextLoads() {
        userService.getUserById(1);
        userService.getUserById(1);
        userService.getUserByName("KK");
        userService.getUserByName("KK");

        ValueOperations<String, String> stringRedis = redisTemplate.opsForValue();
        stringRedis.set("name1", "test", 50, TimeUnit.SECONDS);
        stringRedis.set("name2", "test2");
        System.out.println(stringRedis.get("name1"));
        System.out.println(stringRedis.get("name2"));
    }

}
