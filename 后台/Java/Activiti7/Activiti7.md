# 目录

[TOC]

# Activiti7 介绍

Activiti 项目是基于 Apache License 许可的开源项目。

Activiti 是一个用 Java 编写的开源工作流引擎。并且遵循 BPMN2.0 标准。

Activiti7 能轻易的与 SpringBoot 项目进行整合。

Activiti7 以 SpringSecurity 作为用户与角色的默认安全机制。 

## 1、工作流常见业务场景

### 1.1、线性审批

![image-20220928163552371](Activiti7.assets/image-20220928163552371.png)

### 1.2、会签审批

![image-20220928163633673](Activiti7.assets/image-20220928163633673.png)

### 1.3、条件流程

![image-20220928163733030](Activiti7.assets/image-20220928163733030.png)



# BPMN2.0 标准

BPNM：业务流程建模标注（Business Process Model And Notation）。

BPNM 图最终是以 XML 文件的格式保存的。



# SpringBoot 与 Activiti7 整合

## 1、Maven 引入依赖

关键依赖如下：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.activiti.dependencies</groupId>
            <artifactId>activiti-dependencies</artifactId>
            <version>7.1.0.M6</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.activiti</groupId>
        <artifactId>activiti-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

完整依赖 pom.xml 如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.zgy</groupId>
    <artifactId>activiti7-lab</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    <description>Activiti7 案例</description>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>2.2.5.RELEASE</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <dependency>
                <groupId>org.activiti.dependencies</groupId>
                <artifactId>activiti-dependencies</artifactId>
                <version>7.1.0.M6</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.22</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.7.22</version>
        </dependency>

        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.28</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>

        <dependency>
            <groupId>org.activiti</groupId>
            <artifactId>activiti-spring-boot-starter</artifactId>
        </dependency>
    </dependencies>
</project>
```

## 2、源代码

application.yml 内容如下

```properties
server:
  port: 9090

spring:
  datasource:
    username: developer
    password: 123456
    url: jdbc:mysql:///activiti7_lab
    driver-class-name: com.mysql.cj.jdbc.Driver
  activiti:
    history-level: full # 创建历史表等级，
                        # none：不记录历史流程，性能高，流程结束后不可读取；
                        # activity：归档流程实例和活动实例，流程变量不同步；
                        # audit：默认值，在 activiti 基础上同步变量值，保存表单属性；
                        # full：性能较差，记录所有实例和变量细节变化，最完整的历史记录，如果需要日后跟踪详细可以开启 full（一般不建议开启）。
    db-history-used: true # 创建历史表
    check-process-definitions: false # 关闭自动部署，通常建议设置为 false，如果设置为 true，需要与 process-definition-location-prefix 和 process-definition-location-suffixes 属性一同使用，作用是项目启动的时候，在指定的位置寻找流程描述文件，并自动部署。
    deployment-mode: never-fail # 关闭 SpringAutoDeployment，解决每次启动项目的时候会发现在 ACT_RE_DEPLOYMENT 表中自动加上一个名为 SpringAutoDeployment 工作流记录
    database-schema-update: true # 创建 activiti 相关的表，
                                 # true：默认值，activiti 会对数据库中所有表进行更新操作，如果表不存在，则自动创建；
                                 # flase：activiti 在启动时，会对比数据库表中保存的版本，如果没有表或者版本不匹配，将抛出异常；
                                 # create_drop：在 activiti 启动时创建表，在关闭时删除表（必须手动关闭引擎，才能删除表）；
                                 # drop-create：在 activiti 启动时删除原来的旧表，然后在创建新表（不需要手动关闭引擎）。
```

UserDetailsServiceImpl.java 内容如下

```java
package com.zgy.service.impl;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022-8-22
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	@Override
	public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
		return null;
	}
}
```

Activiti7LabApplication.java 内容如下

```java
package com.zgy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022-8-22
 */
@Slf4j
@SpringBootApplication
@RestController
public class Activiti7LabApplication {

	public static void main(String[] args) {
		SpringApplication.run(Activiti7LabApplication.class, args);
		log.info("==========> 启动成功");
	}
}
```

启动项目后，会自动在数据库 `activiti7_lab` 下生成下面 **25** 张表，说明整合 Activiti7 成功

![image-20221114234843090](Activiti7.assets/image-20221114234843090.png)

## 3、25 张表的含义解释

- ACT_EVT_LOG

- ACT_GE_BYTEARRAY
- ACT_GE_PROPERTY
- ACT_HI_ACTINST
- ACT_HI_ATTACHMENT
- ACT_HI_COMMENT
- ACT_HI_DETAIL
- ACT_HI_IDENTITYLINK
- ACT_HI_PROCINST
- ACT_HI_TASKINST
- ACT_HI_VARINST
- ACT_PROCDEF_INFO
- ACT_RE_DEPLOYMENT
- ACT_RE_MODEL
- ACT_RE_PROCDEF
- ACT_RU_DEADLETTER_JOB
- ACT_RU_EVENT_SUBSCR
- ACT_RU_EXECUTION
- ACT_RU_IDENTITYLINK
- ACT_RU_INTEGRATION
- ACT_RU_JOB
- ACT_RU_SUSPENDED_JOB
- ACT_RU_TASK
- ACT_RU_TIMER_JOB
- ACT_RU_VARIABLE



# IDEA BPMN2.0 插件安装

推荐安装 `Activiti BPMN visualizer` 插件。



# Activiti7 核心类

![image-20220928154701463](Activiti7.assets/image-20220928154701463.png)

## 1、流程部署 Deployment



## 2、流程定义 ProcessDefinition

## 3、流程实例 ProcessInstance

## 4、任务处理 Task

## 5、历史任务 HistoryService



# UEL 表达式



# BPMN2.0 流程网关



# Activiti7 新特性

## 1、API 新特性-ProcessRuntime

## 2、API 新特性-TaskRuntime

## 3、SpringSecurity 集成

Activiti7 的 Maven 依赖包中默认就引入了 SpringSecurity 相关依赖，因此不需要额外引入 SpringSecurity 相关依赖。

在 Activiti7 中，用户必须要拥有 `ROLE_ACTIVITI_USER` 角色才能执行相关的操作。

## 4、BPMN-JS 整合

