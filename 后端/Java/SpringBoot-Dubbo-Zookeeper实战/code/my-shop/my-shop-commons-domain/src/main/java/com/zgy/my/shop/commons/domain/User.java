package com.zgy.my.shop.commons.domain;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author ZGY
 * @date 2019/9/30 16:48
 * @description User
 */
@Data
@ToString
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 用户Id
     */
    private Long id;
    /**
     * 用户名称
     */
    private String username;
    /**
     * 用户密码
     */
    private String password;
    /**
     * 年龄
     */
    private Integer age;
    /**
     * 性别
     */
    private Integer sex;
    /**
     * 电话
     */
    private String phone;
}
