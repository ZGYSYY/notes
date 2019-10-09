package com.zgy.my.shop.commons.dubbo.config;

import org.apache.dubbo.common.serialize.support.SerializationOptimizer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author ZGY
 * @date 2019/10/9 11:18
 * @description SerializationOptimizerImpl
 */
public class SerializationOptimizerImpl implements SerializationOptimizer {

    @Override
    public Collection<Class> getSerializableClasses() {
        List<Class> classes = new LinkedList<>();
        // classes.add(需要被序列化的类.class);
        return classes;
    }
}
