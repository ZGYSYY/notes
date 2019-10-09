package com.zgy.my.shop.service.user.provider.api.impl;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.zgy.my.shop.commons.domain.User;
import com.zgy.my.shop.commons.mapper.UserMapper;
import com.zgy.my.shop.service.user.api.UserService;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * @author ZGY
 * @date 2019/10/8 12:02
 * @description UserServiceImpl
 */
@Service(version = "${services.versions.user.v1}")
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @HystrixCommand(commandProperties = {
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "10"),
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "2000")
    }, fallbackMethod = "selectAllError")
    @Override
    public List<User> selectAll() {
        return userMapper.selectAll();
    }

    /**
     * 获取用户失败时，Hystrix回调方法
     * @return
     */
    public List<User> selectAllError() {
        return Collections.emptyList();
    }
}
