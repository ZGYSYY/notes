package com.zgy.my.shop.commons.domain;

import lombok.Data;
import lombok.ToString;

/**
 * @author ZGY
 * @date 2019/9/30 16:48
 * @description User
 */
@Data
@ToString
public class User {
    private Integer id;
    private String username;
    private Integer age;
    private String phone;
}
