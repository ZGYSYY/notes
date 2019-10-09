package com.zgy.my.shop.service.user.api;

import com.zgy.my.shop.commons.domain.User;

import java.util.List;

/**
 * @author ZGY
 */
public interface UserService {
    /**
     * 获取所有用户列表
     * @return
     */
    List<User> selectAll();
}
