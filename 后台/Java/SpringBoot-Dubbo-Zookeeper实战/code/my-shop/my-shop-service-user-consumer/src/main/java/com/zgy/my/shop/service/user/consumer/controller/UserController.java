package com.zgy.my.shop.service.user.consumer.controller;

import com.zgy.my.shop.commons.domain.User;
import com.zgy.my.shop.service.user.api.UserService;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * @author ZGY
 * @date 2019/10/9 15:06
 * @description UserController
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Reference(version = "${services.versions.user.v1}")
    private UserService userService;

    @GetMapping
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView("user/index");
        return modelAndView;
    }

    @GetMapping("/list")
    public List<User> userList() {
        List<User> users = userService.selectAll();
        return users;
    }
}
