package com.zgy.my.shop.service.content.api;

import com.github.pagehelper.PageInfo;
import com.zgy.my.shop.commons.domain.Content;

/**
 * 内容接口
 * @author ZGY
 */
public interface ContentService {
    /**
     * 分页查询
     * @param start
     * @param length
     * @param content
     * @return
     */
    PageInfo<Content> page(Integer start, Integer length, Content content);
}
