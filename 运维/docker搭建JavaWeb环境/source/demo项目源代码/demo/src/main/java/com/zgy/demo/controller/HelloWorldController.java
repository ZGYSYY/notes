package com.zgy.demo.controller;

import com.zgy.demo.entity.po.UserPO;
import com.zgy.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/user")
public class HelloWorldController {

    @Autowired
    UserService userService;

    @GetMapping("/{id}")
    public ModelAndView index(@PathVariable int id){
        ModelAndView view = new ModelAndView("hello_world/index");
        UserPO user = userService.findByUserId(id);
        if(user != null){
            view.addObject("user",user);
        }

        return view;
    }

    @GetMapping()
    public ModelAndView register(){
        ModelAndView view = new ModelAndView("hello_world/register");

        return view;
    }

    @PostMapping()
    public Object addUser(UserPO user){
        if(user != null){
            UserPO result = userService.save(user);
            return result;
        }else{
            return "注册失败";
        }
    }
}
