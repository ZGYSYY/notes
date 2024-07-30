package com.zgy.my.shop.api.gateway.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.rpc.RpcContext;
import com.zgy.my.shop.service.content.api.ContentConsumerService;
import com.zgy.my.shop.service.user.api.UserConsumerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author ZGY
 */
@Controller
@RequestMapping("/router")
public class RouterController {

    @Value("${services.ports.user}")
    private String userPort;

    @Value("${services.ports.content}")
    private String contentPort;

    @Reference(version = "${services.versions.user.v1}")
    private UserConsumerService userConsumerService;

    @Reference(version = "${services.versions.user.v1}")
    private ContentConsumerService contentConsumerService;

    /**
     * user 路由
     * @param path
     * @return
     */
    @GetMapping("/user")
    public String user(String path) {
        // 远程调用
        userConsumerService.info();

        return getRequest(userPort, path);
    }

    /**
     * content 路由
     * @param path
     * @return
     */
    @GetMapping("/content")
    public String content(String path) {
        // 远程调用
        contentConsumerService.info();

        return getRequest(contentPort, path);
    }

    /**
     * 获取请求地址
     * @return
     * @param path
     */
    private String getRequest(String serverPort, String path) {
        // 判断本端是否是消费端，如果是，返回true
        boolean isConsumerSide = RpcContext.getContext().isConsumerSide();
        System.out.println("isConsumerSide: " + isConsumerSide);
        if (isConsumerSide) {
            // 获取最后一次调用的提供方 ip 地址
            String serverHost = RpcContext.getContext().getRemoteHost();
            System.out.println("serverHost: " + serverHost);
            // 打印需要访问的提供者的端口号
            System.out.println("serverPort: " + serverPort);

            return String.format("redirect:http://%s:%s%s", serverHost, serverPort, path);
        }

        return null;
    }
}
