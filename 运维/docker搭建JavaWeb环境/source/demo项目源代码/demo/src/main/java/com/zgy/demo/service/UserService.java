package com.zgy.demo.service;

import com.zgy.demo.dao.UserDao;
import com.zgy.demo.entity.po.UserPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    // SpringBoot JPA 默认CRUD实现
    public UserPO save(UserPO po) {
        UserPO user = userDao.save(po);

        return user;
    }

    public List<UserPO> list() {
        return userDao.findAll();
    }

    public void delete(int id) {
        userDao.delete(id);
    }

    public UserPO findByUserId(int id){
        return userDao.findByUserId(id);
    }
}
