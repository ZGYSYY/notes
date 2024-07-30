package com.zgy.my.shop.service.content.consumer.api.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.zgy.my.shop.service.content.api.ContentConsumerService;

/**
 * @author ZGY
 */
@Service(version = "${services.versions.content.v1}")
public class ContentConsumerServiceImpl implements ContentConsumerService {

    @Override
    public void info() {

    }
}
