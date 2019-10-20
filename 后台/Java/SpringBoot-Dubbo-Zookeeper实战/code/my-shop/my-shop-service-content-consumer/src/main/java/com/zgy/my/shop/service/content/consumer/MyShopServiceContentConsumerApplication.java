package com.zgy.my.shop.service.content.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;

/**
 * @author ZGY
 * @date 2019/10/17 16:27
 * @description MyShopServiceContentConsumerApplication
 */
@SpringBootApplication(scanBasePackages = "com.zgy.my.shop")
@EnableHystrixDashboard
@EnableHystrix
public class MyShopServiceContentConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyShopServiceContentConsumerApplication.class, args);
    }
}
