package com.zgy.my.shop.commons.domain;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author ZGY
 * @date 2019/10/17 15:56
 * @description Content
 */
@Data
@ToString
public class Content implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 唯一标识Id
     */
    private Long id;
    /**
     * 标题
     */
    private String title;
}
