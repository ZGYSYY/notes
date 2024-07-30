package com.zgy.my.shop.service.content.provider.api.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zgy.my.shop.commons.domain.Content;
import com.zgy.my.shop.commons.mapper.ContentMapper;
import com.zgy.my.shop.service.content.api.ContentService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author ZGY
 * @date 2019/10/17 16:01
 * @description ContentServiceImpl
 */
@Service(version = "${services.versions.content.v1}")
public class ContentServiceImpl implements ContentService {

    @Autowired
    private ContentMapper contentMapper;

    @Override
    public PageInfo<Content> page(Integer start, Integer length, Content content) {
        PageHelper.offsetPage(start, length);
        PageInfo<Content> pageInfo = new PageInfo<>(contentMapper.select(content));
        return pageInfo;
    }
}
