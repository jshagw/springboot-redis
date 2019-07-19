package gw.redisdemo.services;

import gw.redisdemo.entities.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    // value的字符串，会在存到redis的key前面，格式为mycache::
    // key和keyGenerator互斥
    @Cacheable(value = "testA", keyGenerator = "cacheKeyGenerator")
    public User getUserById(Integer id) {
        System.out.println("getUserById " + id);
        return new User(id, "Neil");
    }

    // 用单引号拼接
    @Cacheable(value = "testB", key = "#root.methodName + '-' + #name")
    public User getUserByName(String name) {
        System.out.println("getUserByName " + name);
        return new User(2, name);
    }
}
