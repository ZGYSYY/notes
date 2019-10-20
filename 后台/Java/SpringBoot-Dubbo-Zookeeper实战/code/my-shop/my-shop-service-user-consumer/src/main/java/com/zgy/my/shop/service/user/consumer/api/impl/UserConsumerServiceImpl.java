package com.zgy.my.shop.service.user.consumer.api.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.rpc.RpcContext;
import com.zgy.my.shop.service.user.api.UserConsumerService;

/**
 * @author ZGY
 * UserConsumerServiceImpl
 */
@Service(version = "${services.versions.user.v1}")
public class UserConsumerServiceImpl implements UserConsumerService {

    @Override
    public void info() {
        System.out.println("调用 UserConsumerServiceImpl.info() 成功");
        boolean isConsumerSide = RpcContext.getContext().isConsumerSide();
        System.out.println("isConsumerSide: " + isConsumerSide);
    }
}
