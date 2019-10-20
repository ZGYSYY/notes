package com.zgy.my.shop.service.content.consumer.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.github.pagehelper.PageInfo;
import com.zgy.my.shop.commons.domain.Content;
import com.zgy.my.shop.service.content.api.ContentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author ZGY
 * @date 2019/10/17 16:30
 * @description ContentController
 */
@RestController
@RequestMapping("/content")
public class ContentController {

    @Reference(version = "${services.versions.content.v1}")
    private ContentService contentService;

    @GetMapping
    public ModelAndView index() {
        ModelAndView view = new ModelAndView("content/index");

        return view;
    }

    /**
     *
     * @param start
     * @param length
     * @return
     */
    @GetMapping("page")
    public PageInfo<Content> page(@RequestParam("start") Integer start, @RequestParam("length") Integer length) {
        PageInfo<Content> page = contentService.page(start, length, new Content());

        return page;
    }
}
