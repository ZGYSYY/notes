package com.zgy.my.shop.service.content.provider;

import com.alibaba.dubbo.container.Main;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tk.mybatis.spring.annotation.MapperScan;

/**
 * @author ZGY
 * @date 2019/10/17 15:45
 * @description MyShopServiceContentProviderApplication
 */
@SpringBootApplication(scanBasePackages = "com.zgy.my.shop")
@EnableTransactionManagement
@MapperScan("com.zgy.my.shop.commons.mapper")
@EnableHystrix
@EnableHystrixDashboard
public class MyShopServiceContentProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyShopServiceContentProviderApplication.class, args);
        Main.main(args);
    }
}
