package com.zgy.my.shop.api.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;

/**
 * @author ZGY
 */
@SpringBootApplication
@EnableHystrix
@EnableHystrixDashboard
public class MyShopApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyShopApiGatewayApplication.class, args);
    }
}
