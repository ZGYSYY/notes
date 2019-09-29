package com.zgy.demo.dao;

import com.zgy.demo.entity.po.UserPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;

public interface UserDao extends JpaRepository<UserPO,Serializable>{
    UserPO findByUserId(int id);
}
