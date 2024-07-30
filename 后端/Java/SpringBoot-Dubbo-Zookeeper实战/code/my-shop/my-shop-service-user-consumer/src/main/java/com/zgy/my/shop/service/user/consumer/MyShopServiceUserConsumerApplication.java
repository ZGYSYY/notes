package com.zgy.my.shop.service.user.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;

/**
 * @author ZGY
 * @date 2019/10/9 15:02
 * @description MyShopServiceUserConsumerApplication
 */
@SpringBootApplication(scanBasePackages = "com.zgy.my.shop")
@EnableHystrixDashboard
@EnableHystrix
public class MyShopServiceUserConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyShopServiceUserConsumerApplication.class, args);
    }
}
