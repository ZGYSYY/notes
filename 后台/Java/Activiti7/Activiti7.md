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
            <version>7.1.0.M4</version>
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

**Tips**: 

- 目前比较稳定的版本是 `7.1.0.M4`，如果要在项目中使用，推荐使用这个版本。

- 目前 `7.1.0.M6` 版本已经发现 `ProcessRuntime` 的启动流程实例 API 存在问题，官网目前还没有修复。而且该版本还要强制使用 SpringSecurity 框架。

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
                <version>7.1.0.M4</version>
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

- ACT_EVT_LOG（Activiti 日志记录表）

- ACT_GE_BYTEARRAY（资源表）

    流程部署一次，所设置的相关资源会存入到该表中。

- ACT_GE_PROPERTY（历史资源表）

- ACT_HI_ACTINST（历史流程实例步骤表）

    记录了流程实例的每个步骤相关信息。

- ACT_HI_ATTACHMENT

- ACT_HI_COMMENT

- ACT_HI_DETAIL

- ACT_HI_IDENTITYLINK（历史流程实例参与人记录表）

- ACT_HI_PROCINST（历史流程实例表）

- ACT_HI_TASKINST（历史任务表）

- ACT_HI_VARINST（历史参数表）

- ACT_PROCDEF_INFO

- ACT_RE_DEPLOYMENT（流程部署表）

    流程部署一次，生成一条记录。

- ACT_RE_MODEL

- ACT_RE_PROCDEF（流程定义表）

    流程部署一次，生成一条记录。

    当基于一个 BPMN 文件，进行多次流程部署时，该表的 `KEY_` 字段不会变，`VERSION_` 字段会加一。

- ACT_RU_DEADLETTER_JOB

- ACT_RU_EVENT_SUBSCR

- ACT_RU_EXECUTION（流程实例表）

    流程实例在运行过程中，会根据流程的执行进度，在该表中生成相关节点的记录。

    在启动流程实例时会在该表中生成 2 条记录，第 1 条是`启动`节点，第 2 条才是`任务`节点。

- ACT_RU_IDENTITYLINK（流程实例参与人记录表）

    该表会记录该流程实例运行过程中，参与人与节点的关系。

- ACT_RU_INTEGRATION

- ACT_RU_JOB

- ACT_RU_SUSPENDED_JOB

- ACT_RU_TASK（当前运行任务表）

    流程实例启动后，该表会新增一条记录，记录的是当前需要完成的任务。

- ACT_RU_TIMER_JOB

- ACT_RU_VARIABLE（运行时参数表）



# IDEA BPMN2.0 插件安装

推荐安装 `Activiti BPMN visualizer` 插件。



# Activiti7 核心类

![image-20220928154701463](Activiti7.assets/image-20220928154701463.png)

## 1、流程部署 Deployment

### 1.1、基本操作

在 `resources/bpmn` 目录下新建 `Part1_Deployment.bpmn20.xml` 文件，内容如下

![image-20221120164156063](Activiti7.assets/image-20221120164156063.png)

![image-20221120164320938](Activiti7.assets/image-20221120164320938.png)

新建 `Part1_Deployment.java` 文件，内容如下

```java
package com.zgy;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022-11-20
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Part1_Deployment {

	@Autowired
	private RepositoryService repositoryService;

	/**
	 * 初始化流程部署
	 */
	@Test
	public void initDeploymentBPMN() {
		String fileName = "bpmn/Part1_Deployment.bpmn20.xml";

		Deployment deployment = repositoryService.createDeployment()
				// 设置 BPMN 文件
				.addClasspathResource(fileName)
				// 设置流程部署名称
				.name("流程部署测试 BPMN")
				.deploy();
		log.info("==========> name: [{}]", deployment.getName());
	}
}
```

### 1.2、添加 BPMN 图片

将流程图保存为图片，修改 `Part1_Deployment.java` 文件，内容如下

```java
package com.zgy;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022-11-20
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Part1_Deployment {

	@Autowired
	private RepositoryService repositoryService;

	/**
	 * 初始化流程部署
	 */
	@Test
	public void initDeploymentBPMN() {
		String fileName = "bpmn/Part1_Deployment.bpmn20.xml";
		String imgName = "bpmn/Part1_Deployment.bpmn20.png";

		Deployment deployment = repositoryService.createDeployment()
				// 设置 BPMN 文件
				.addClasspathResource(fileName)
				// 设置 BPMN 图片
				.addClasspathResource(imgName)
				// 设置流程部署名称
				.name("流程部署测试 BPMN_V2")
				.deploy();
		log.info("==========> name: [{}]", deployment.getName());
	}
}
```

**Tips**：在 Activiti7 中，已经不推荐将 BPMN 图片保存到数据库了，现在有前端插件能够将生成的 BPMN 文件进行解析并回显。

### 1.3、使用 ZIP 文件

将文件 `Part1_Deployment.bpmn20.xml` 和 `Part1_Deployment.bpmn20.png` 压缩为 ZIP 格式的压缩文件，文件名为 `Part1_Deployment.bpmn20.zip`，修改 `Part1_Deployment.java` 文件，新增内容如下

```java
/**
 * 初始化流程部署-ZIP
 */
@Test
public void initDeploymentZIP() {
    // 获取文件流
    InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("bpmn/Part1_Deployment.bpmn20.zip");

    // 转换流
    Assert.notNull(inputStream, "inputStream 为空");
    ZipInputStream zipInputStream = new ZipInputStream(inputStream);

    // 流程部署
    Deployment deployment = repositoryService.createDeployment()
        .addZipInputStream(zipInputStream)
        .name("流程部署测试 ZIP").deploy();
    log.info("==========> name: [{}]", deployment.getName());
}
```

**Tips**：压缩文件在流程部署过程中会自动解压。

### 1.4、查看流程部署

修改 `Part1_Deployment.java` 文件，新增内容如下

```java
/**
 * 获取流程部署
 */
@Test
public void getDeployments() {
	List<Deployment> list = repositoryService
			// 创建流程部署查询对象
			.createDeploymentQuery()
			// 获取所有的流程部署
			.list();
	list.forEach(dep -> {
		log.info("==========> id: [{}], name: [{}], deploymentTime: [{}]", dep.getId(), dep.getName(), dep.getDeploymentTime());
	});
}
```

### 1.5、删除流程部署

修改 `Part1_Deployment.java` 文件，新增内容如下

```java
/**
 * 删除流程部署
 */
@Test
public void delDeployment() {
	/*
	第一个参数是流程部署 id。
	第二个参数，解释如下
		true：删除历史记录和正在运行的任务记录。
		false：不删除历史记录和正在运行的任务记录。
	*/
	repositoryService.deleteDeployment("a8ba3f1a-68b9-11ed-9cb0-8286f2267041", true);
	log.info("==========> 流程部署删除成功！");
}
```

### 1.6、涉及的表

- ACT_RE_PROCDEF
- ACT_RE_DEPLOYMENT
- ACT_GE_BYTEARRAY

## 2、流程定义 ProcessDefinition

### 2.1、查看流程定义

新建 `Part2_ProcessDefinition.java` 文件，内容如下

```java
package com.zgy;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022-11-20
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Part2_ProcessDefinition {

	@Autowired
	private RepositoryService repositoryService;

	/**
	 * 获取流程定义
	 */
	@Test
	public void getDefinitions() {
		List<ProcessDefinition> list = repositoryService
				// 创建流程定义查询对象
				.createProcessDefinitionQuery()
				// 获取所有的流程定义
				.list();

		list.forEach(proc -> {
			log.info("==========> name: [{}], key: [{}], resourceName: [{}], deploymentId: [{}], version: [{}]", proc.getName(), proc.getKey(), proc.getResourceName(), proc.getDeploymentId(), proc.getVersion());
		});
	}
}
```

### 2.2、涉及的表

- ACT_RE_PROCDEF

## 3、流程实例 ProcessInstance

### 3.1、启动流程实例

新建 `Part3_ProcessInstance.java` 文件，内容如下

```java
package com.zgy;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022-11-20
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Part3_ProcessInstance {

	@Autowired
	private RuntimeService runtimeService;

	/**
	 * 启动（初始化）流程实例
	 */
	@Test
	public void initProcessInstance() {
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess_Part1");
		log.info("==========> 流程定义启动成功，processInstanceId: [{}]，processDefinitionVersion：[{}]", processInstance.getProcessInstanceId(), processInstance.getProcessDefinitionVersion());
	}
}
```

**Tips**：当流程定义表 `ACT_RE_PROCDEF` 中对应的 `KEY_` 存在多条记录时，会取 `VERSION_` 的值为最大的那条流程定义来启动流程实例。

### 3.2、查看流程实例

修改 `Part3_ProcessInstance.java` 文件，新增内容如下

```java
/**
 * 查看流程实例
 */
@Test
public void getProcessInstances() {
    List<ProcessInstance> list = runtimeService
        // 创建流程实例查询对象
        .createProcessInstanceQuery()
        // 获取所有的流程实例
        .list();

    list.forEach(proc -> log.info("==========> processInstanceId: [{}], processDefinitionId: [{}], isEnded: [{}], isSuspended: [{}]", proc.getProcessInstanceId(), proc.getProcessDefinitionId(), proc.isEnded(), proc.isSuspended()));
}
```

### 3.3、暂停流程实例

修改 `Part3_ProcessInstance.java` 文件，新增内容如下

```java
/**
 * 暂停流程实例
 */
@Test
public void suspendProcessInstance() {
    runtimeService.suspendProcessInstanceById("2302a770-68e1-11ed-aaa4-8286f2267041");
    log.info("==========> 暂停流程实例成功！");
}
```

### 3.4、激活流程实例

修改 `Part3_ProcessInstance.java` 文件，新增内容如下

```java
/**
 * 激活流程实例
 */
@Test
public void activateProcessInstance() {
    runtimeService.activateProcessInstanceById("2302a770-68e1-11ed-aaa4-8286f2267041");
    log.info("==========> 激活流程实例成功！");
}
```

### 3.5、删除流程实例

修改 `Part3_ProcessInstance.java` 文件，新增内容如下

```java
/**
 * 删除流程实例
 */
@Test
public void delProcessInstance() {
	/*
	第一个参数是流程实例 id
	第二个参数是删除原因
	*/
	runtimeService.deleteProcessInstance("2302a770-68e1-11ed-aaa4-8286f2267041", "原因AAA");
	log.info("==========> 删除流程实例成功！");
}
```

### 3.6、涉及的表

- ACT_RU_EXECUTION
- ACT_RU_IDENTITYLINK

## 4、任务处理 Task

### 4.1、基本操作

在 `resources/bpmn` 目录下新建 `Part4_Task.bpmn20.xml` 文件，内容如下

![image-20221120233946185](Activiti7.assets/image-20221120233946185.png)

![image-20221120234203105](Activiti7.assets/image-20221120234203105.png)

![image-20221120234327224](Activiti7.assets/image-20221120234327224.png)



对上面的内容进行流程部署，然后启动实例，新建 `Part4_Task.java` 文件，内容如下

```java
package com.zgy;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022-11-20
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Part4_Task {

	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RuntimeService runtimeService;

	/**
	 * 初始化流程部署
	 */
	@Test
	public void initDeploymentBPMN() {
		Deployment deployment = repositoryService.createDeployment()
				// 设置 BPMN 文件
				.addClasspathResource("bpmn/Part4_Task.bpmn20.xml")
				// 设置流程部署名称
				.name("流程部署测试 task")
				.deploy();
		log.info("==========> name: [{}]", deployment.getName());
	}

	/**
	 * 启动（初始化）流程实例
	 */
	@Test
	public void initProcessInstance() {
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess_Task");
		log.info("==========> 流程定义启动成功，processInstanceId: [{}]，processDefinitionVersion：[{}]", processInstance.getProcessInstanceId(), processInstance.getProcessDefinitionVersion());
	}
}
```

### 4.2、查看任务

修改 `Part4_Task.java` 文件，新增内容如下

```java
/**
 * 查看任务
 */
@Test
public void getTasks() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

### 4.3、查看我的待办任务

修改 `Part4_Task.java` 文件，新增内容如下

```java
/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			.taskAssignee("BaJie")
			// .taskAssignee("WuKong")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

### 4.4、完成任务

修改 `Part4_Task.java` 文件，新增内容如下

```java
/**
 * 完成任务
 */
@Test
public void completeTask() {
	taskService.complete("51cfe7af-6a49-11ed-a3fa-8086f2267041");
	log.info("==========> 任务已完成！");
}
```

### 4.5、拾取任务

在 `resources/bpmn` 目录下新建 `Part4_Task_claim.bpmn20.xml` 文件，内容如下

![image-20221122175336558](Activiti7.assets/image-20221122175336558.png)

![image-20221122175459349](Activiti7.assets/image-20221122175459349.png)

对上面的内容进行流程部署，然后启动实例，修改 `Part4_Task.java` 文件，修改内容如下

```java
/**
 * 初始化流程部署
 */
@Test
public void initDeploymentBPMN() {
	Deployment deployment = repositoryService.createDeployment()
			// 设置 BPMN 文件
			.addClasspathResource("bpmn/Part4_Task_claim.bpmn20.xml")
			// 设置流程部署名称
			.name("流程部署测试候选人 task")
			.deploy();
	log.info("==========> name: [{}]", deployment.getName());
}

/**
 * 启动（初始化）流程实例
 */
@Test
public void initProcessInstance() {
	ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess_claim");
	log.info("==========> 流程定义启动成功，processInstanceId: [{}]，processDefinitionVersion：[{}]", processInstance.getProcessInstanceId(), processInstance.getProcessDefinitionVersion());
}
```

拾取任务，修改 `Part4_Task.java` 文件，新增内容如下

```java
/**
 * 拾取任务
 */
@Test
public void claimTask() {
	taskService.claim("8243bf37-6a4c-11ed-a6c4-8086f2267041", "BaJie");
	log.info("==========> 拾取任务成功！");
}
```

### 4.6、归还或交办任务

修改 `Part4_Task.java` 文件，新增内容如下

```java
/**
 * 归还或交办任务
 */
@Test
public void setTaskAssignee() {
	// 归还任务
	// taskService.setAssignee("8243bf37-6a4c-11ed-a6c4-8086f2267041", null);
	// log.info("==========> 归还任务成功！");
	// 交办任务
	taskService.setAssignee("8243bf37-6a4c-11ed-a6c4-8086f2267041", "WuKong");
	log.info("==========> 交办任务成功！");
}
```

### 4.7、涉及的表

- ACT_RU_TASK
- ACT_RU_EXECUTION
- ACT_RU_IDENTITYLINK

## 5、历史任务 HistoryService

### 5.1、根据用户名查询历史任务记录

新建 `Part5_HistoricTaskInstance.java` 文件，内容如下

```java
package com.zgy;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022-11-22
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Part5_HistoricTaskInstance {

	@Autowired
	private HistoryService historyService;

	/**
	 * 根据用户名查询历史任务记录
	 */
	@Test
	public void historicTaskInstanceByUser() {
		List<HistoricTaskInstance> list = historyService
				// 创建历史任务查询对象
				.createHistoricTaskInstanceQuery()
				// 设置查询用户
				.taskAssignee("WuKong")
				// 设置按照任务完成时间升序
				.orderByHistoricTaskInstanceEndTime().asc()
				// 获取历史任务记录
				.list();

		list.forEach(task -> log.info("==========> id: [{}], processInstanceId: [{}], name: [{}]", task.getId(), task.getProcessInstanceId(), task.getName()));
	}
}

```

### 5.2、根据流程实例查询历史任务记录

修改 `Part5_HistoricTaskInstance.java` 文件，新增内容如下

```java
/**
 * 根据流程实例查询历史任务记录
 */
@Test
public void historicTaskInstanceByPiId() {
	List<HistoricTaskInstance> list = historyService
			// 创建历史任务查询对象
			.createHistoricTaskInstanceQuery()
			// 设置查询用户
			.processInstanceId("81362e2c-6a46-11ed-8fe5-8086f2267041")
			// 设置按照任务完成时间升序
			.orderByHistoricTaskInstanceEndTime().asc()
			// 获取历史任务记录
			.list();
	list.forEach(task -> log.info("==========> id: [{}], processInstanceId: [{}], name: [{}]", task.getId(), task.getProcessInstanceId(), task.getName()));
}
```

### 5.3、涉及的表

- ACT_HI_ACTINST
- ACT_HI_IDENTITYLINK
- ACT_HI_PROCINST
- ACT_HI_TASKINST



# UEL 表达式

## 1、基本知识

### 1.1、UEL 表达式（统一表达式语言）。

### 1.2、表达式描述

![image-20221122231051362](Activiti7.assets/image-20221122231051362.png)

### 1.3、对应的数据表

- ACT_RU_VARIABLE（运行时参数表）
- ACT_HI_VARINST（历史参数表）

### 1.4、UEL 表达式的保留字

![image-20221122231439410](Activiti7.assets/image-20221122231439410.png)

### 1.5、UEL 表达式的运算符

![image-20221122231524101](Activiti7.assets/image-20221122231524101.png)

## 2、实战案例

### 2.1、启动流程实例带参数指定执行人

在 `resources/bpmn` 目录下新建 `Part6_UEL_V1.bpmn20.xml` 文件，内容如下

![image-20221122234131771](Activiti7.assets/image-20221122234131771.png)

![image-20221122234235190](Activiti7.assets/image-20221122234235190.png)

对 `Part6_UEL_V1.bpmn20.xml` 文件进行流程部署，关键代码如下

```java
/**
 * 初始化流程部署
 */
@Test
public void initDeploymentBPMN() {
	Deployment deployment = repositoryService.createDeployment()
			// 设置 BPMN 文件
			.addClasspathResource("bpmn/Part6_UEL_V1.bpmn20.xml")
			// 设置流程部署名称
			.name("流程部署测试 UEL V1")
			.deploy();
	log.info("==========> name: [{}]", deployment.getName());
}
```

新建 `Part6_UEL.java` 文件，内容如下

```java
package com.zgy;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022-11-22
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Part6_UEL {

	@Autowired
	private RuntimeService runtimeService;

	/**
	 * 启动流程实例带参数指定执行人
	 */
	@Test
	public void initProcessInstanceWithArgs() {
		Map<String, Object> variables = new HashMap<>();
		variables.put("ZhiXingRen", "WuKong");
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess_UEL_V1", variables);
		log.info("==========> 流程实例启动成功，processInstanceId: [{}]", processInstance.getProcessInstanceId());
	}
}
```

查看任务，关键代码如下

```java
/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			// .taskAssignee("BaJie")
			.taskAssignee("WuKong")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

结果如下

![image-20221122235806326](Activiti7.assets/image-20221122235806326.png)

### 2.2、完成任务带参数

在 `resources/bpmn` 目录下新建 `Part6_UEL_V2.bpmn20.xml` 文件，内容如下

![image-20221123213150134](Activiti7.assets/image-20221123213150134.png)

![image-20221123213328628](Activiti7.assets/image-20221123213328628.png)

![image-20221123213428225](Activiti7.assets/image-20221123213428225.png)

![image-20221123213515809](Activiti7.assets/image-20221123213515809.png)

![image-20221123213625559](Activiti7.assets/image-20221123213625559.png)

![image-20221123213849206](Activiti7.assets/image-20221123213849206.png)

对 `Part6_UEL_V2.bpmn20.xml` 文件进行流程部署并启动流程实例，关键代码如下

```java
@Autowired
private RepositoryService repositoryService;
@Autowired
private RuntimeService runtimeService;

/**
 * 初始化流程部署
 */
@Test
public void initDeploymentBPMN() {
	Deployment deployment = repositoryService.createDeployment()
			// 设置 BPMN 文件
			.addClasspathResource("bpmn/Part6_UEL_V2.bpmn20.xml")
			// 设置流程部署名称
			.name("流程部署测试 UEL V2")
			.deploy();
	log.info("==========> name: [{}]", deployment.getName());
}

/**
 * 启动（初始化）流程实例
 */
@Test
public void initProcessInstance() {
	// ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess_Task");
	ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess_UEL_V2");
	log.info("==========> 流程定义启动成功，processInstanceId: [{}]，processDefinitionVersion：[{}]", processInstance.getProcessInstanceId(), processInstance.getProcessDefinitionVersion());
}
```

查看任务，关键代码如下

```java
/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			.taskAssignee("BaJie")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

结果如下

![image-20221123214836480](Activiti7.assets/image-20221123214836480.png)

完成任务并带参数，修改 `Part6_UEL.java` 文件，新增内容如下

```java
/**
 * 完成任务带参数
 */
@Test
public void completeTaskWithArgs() {
	Map<String, Object> variables = new HashMap<>();
	variables.put("pay", 101);
	taskService.complete("316e922e-6b32-11ed-9ea1-8086f2267041", variables);
	log.info("==========> 已完成任务！");
}
```

查看任务，关键代码如下

```java
/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			.taskAssignee("WuKong")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

结果如下

![image-20221123215413068](Activiti7.assets/image-20221123215413068.png)

### 2.3、启动流程实例带参数（实体类）

新建 `UEL_POJO.java` 文件，内容如下

```java
package com.zgy;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022-11-23
 */
@Data
public class UEL_POJO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String zhiXingRen;
}
```

在 `resources/bpmn` 目录下新建 `Part6_UEL_V3.bpmn20.xml` 文件，内容如下

![image-20221123222758711](Activiti7.assets/image-20221123222758711.png)

![image-20221123222909798](Activiti7.assets/image-20221123222909798.png)

![image-20221123223028881](Activiti7.assets/image-20221123223028881.png)

对 `Part6_UEL_V3.bpmn20.xml` 文件进行流程部署，关键代码如下

```java
@Autowired
private RepositoryService repositoryService;

/**
 * 初始化流程部署
 */
@Test
public void initDeploymentBPMN() {
	Deployment deployment = repositoryService.createDeployment()
			// 设置 BPMN 文件
			.addClasspathResource("bpmn/Part6_UEL_V3.bpmn20.xml")
			// 设置流程部署名称
			.name("流程部署测试 UEL V3")
			.deploy();
	log.info("==========> name: [{}]", deployment.getName());
}

```

启动流程实例，修改 `Part6_UEL.java` 文件，新增内容如下

```java
/**
 * 启动流程实例带参数（实体类）
 */
@Test
public void initProcessInstanceWithClassArgs() {
	UEL_POJO uel_pojo = new UEL_POJO();
	uel_pojo.setZhiXingRen("BaJie");
	Map<String, Object> variables = new HashMap<>();
	variables.put("uel_pojo", uel_pojo);
	ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess_UEL_V3", variables);
	log.info("==========> 流程实例启动成功，processInstanceId: [{}]", processInstance.getProcessInstanceId());
}
```

查看任务，关键代码如下

```java
@Autowired
private TaskService taskService;

/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			.taskAssignee("BaJie")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

结果如下

![image-20221123223517354](Activiti7.assets/image-20221123223517354.png)

### 2.4、完成任务带参数（多候选人）

继续沿用 `Part6_UEL_V3.bpmn20.xml` 文件。

完成任务，并设置下一任务的候选人。修改 `Part6_UEL.java` 文件，新增内容如下

```java
/**
 * 完成任务带参数（多候选人）
 */
@Test
public void completeTaskWithCandiDateArgs() {
	Map<String, Object> variables = new HashMap<>();
	variables.put("houXuanRen", "WuKong,TangSeng");
	taskService.complete("88685fa2-6b3a-11ed-8a09-8086f2267041", variables);
	log.info("==========> 已完成任务！");
}
```

查看任务，关键代码如下

```java
@Autowired
private TaskService taskService;

/**
 * 查看任务
 */
@Test
public void getTasks() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

结果如下

![image-20221123224440331](Activiti7.assets/image-20221123224440331.png)

### 2.5、直接设置流程/任务变量

修改 `Part6_UEL.java` 文件，新增内容如下

```java
/**
 * 直接设置流程/任务变量
 */
@Test
public void otherArgs() {
	/*
	修改流程的候选人。
	参数解释：
		第一个参数：表 ACT_RU_VARIABLE 的 EXECUTION_ID_ 字段。
		第二个参数：表 ACT_RU_VARIABLE 的 NAME_ 字段。
		第三个参数：新的值。
	*/
	// runtimeService.setVariable("8863a4af-6b3a-11ed-8a09-8086f2267041", "houXuanRen", "WuKong,ShaSeng,TangSeng");
    // log.info("==========> 设置流程变量成功！");
    
	/*
	修改任务的候选人
	参数解释：
		第一个参数：ACT_RU_VARIABLE 的 TASK_ID_ 字段。
		第二个参数：表 ACT_RU_VARIABLE 的 NAME_ 字段。
		第三个参数：新的值。
	*/
	taskService.setVariable("e3ebbb08-6b3c-11ed-99b4-8086f2267041", "houXuanRen", "WuKong,ShaSeng,TangSeng");
	log.info("==========> 设置任务变量成功！");
}
```

### 2.6、直接设置局部变量

修改 `Part6_UEL.java` 文件，新增内容如下

```java
/**
 * 直接设置局部变量
 */
@Test
public void otherLocalArgs() {
	/*
	修改流程的候选人。
	参数解释：
		第一个参数：表 ACT_RU_VARIABLE 的 EXECUTION_ID_ 字段。
		第二个参数：表 ACT_RU_VARIABLE 的 NAME_ 字段。
		第三个参数：新的值。
	*/
	runtimeService.setVariableLocal("8863a4af-6b3a-11ed-8a09-8086f2267041", "houXuanRen", "WuKong,ShaSeng");
	log.info("==========> 设置流程局部变量成功！");
	/*
	修改任务的候选人
	参数解释：
		第一个参数：ACT_RU_VARIABLE 的 TASK_ID_ 字段。
		第二个参数：表 ACT_RU_VARIABLE 的 NAME_ 字段。
		第三个参数：新的值。
	*/
	taskService.setVariableLocal("e3ebbb08-6b3c-11ed-99b4-8086f2267041", "houXuanRen", "WuKong,ShaSeng");
	log.info("==========> 设置任务局部变量成功！");
}
```

## 3、全局变量和局部变量区别

- 全局变量：整个流程实例中变量值共享。
- 局部变量：只在某个节点有效。



# BPMN2.0 流程网关

## 1、常见网关

- 并行网关：多用于多人审批的场景，必须要所有的审批都通过。
- 排他网关：多用于条件判断的场景，只要任一一个审批通过。
- 包容网关：多用于多人审批并带条件的场景，保证符合条件的审批都通过。
- 事件网关：多用于产生了某些事件的场景，对应事件审批通过。

## 2、并行网关-案例

在 `resources/bpmn` 目录下新建 `Part7_Parallel.bpmn20.xml` 文件，内容如下

![image-20221129223122586](Activiti7.assets/image-20221129223122586.png)

![image-20221129223220821](Activiti7.assets/image-20221129223220821.png)

![image-20221129223419698](Activiti7.assets/image-20221129223419698.png)

![image-20221129223509867](Activiti7.assets/image-20221129223509867.png)

对 `Part7_Parallel.bpmn20.xml` 文件进行流程部署，关键代码如下

```java
@Autowired
private RepositoryService repositoryService;

/**
 * 初始化流程部署
 */
@Test
public void initDeploymentBPMN() {
	Deployment deployment = repositoryService.createDeployment()
			// 设置 BPMN 文件
			.addClasspathResource("bpmn/Part7_Parallel.bpmn20.xml")
			// 设置流程部署名称
			.name("流程部署测试并行网关")
			.deploy();
	log.info("==========> name: [{}]", deployment.getName());
}
```

启动流程实例，关键代码如下

```java
@Autowired
private RuntimeService runtimeService;

/**
 * 启动（初始化）流程实例
 */
@Test
public void initProcessInstance() {
	ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess_Parallel");
	log.info("==========> 流程定义启动成功，processInstanceId: [{}]，processDefinitionVersion：[{}]", processInstance.getProcessInstanceId(), processInstance.getProcessDefinitionVersion());
}
```

查看任务，关键代码如下

```java
@Autowired
private TaskService taskService;

/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			.taskAssignee("BaJie")
			// .taskAssignee("WuKong")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

结果如下

![image-20221129224956051](Activiti7.assets/image-20221129224956051.png)

新建 `Part7_Gateway.java` 文件，内容如下

```java
package com.zgy;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.TaskService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/11/29
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Part7_Gateway {

	@Autowired
	private TaskService taskService;

	/**
	 * 完成任务
	 */
	@Test
	public void completeTask() {
		taskService.complete("8266026b-6ff4-11ed-9514-8286f2267041");
		log.info("==========> 任务已完成！");
	}
}
```

执行 `completeTask()` 方法，然后查看任务，关键代码如下

```java
@Autowired
private TaskService taskService;

/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			// .taskAssignee("BaJie")
			.taskAssignee("WuKong")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

结果如下

![image-20221129225546661](Activiti7.assets/image-20221129225546661.png)

再次查看任务，关键代码如下

```java
@Autowired
private TaskService taskService;

/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			// .taskAssignee("BaJie")
			// .taskAssignee("WuKong")
			.taskAssignee("TangSeng")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

结果如下

![image-20221129230009979](Activiti7.assets/image-20221129230009979.png)

## 3、排他网关-案例

在 `resources/bpmn` 目录下新建 `Part7_Exclusive.bpmn20.xml` 文件，内容如下

<img src="Activiti7.assets/image-20221129231153422.png" alt="image-20221129231153422"  />

![image-20221129233219372](Activiti7.assets/image-20221129233219372.png)

![image-20221129231412094](Activiti7.assets/image-20221129231412094.png)

![image-20221129231500373](Activiti7.assets/image-20221129231500373.png)

![image-20221129231548651](Activiti7.assets/image-20221129231548651.png)

![image-20221129231636214](Activiti7.assets/image-20221129231636214.png)

对 `Part7_Exclusive.bpmn20.xml` 文件进行流程部署，关键代码如下

```java
@Autowired
private RepositoryService repositoryService;

/**
 * 初始化流程部署
 */
@Test
public void initDeploymentBPMN() {
	Deployment deployment = repositoryService.createDeployment()
			// 设置 BPMN 文件
			.addClasspathResource("bpmn/Part7_Exclusive.bpmn20.xml")
			// 设置流程部署名称
			.name("流程部署测试排他网关")
			.deploy();
	log.info("==========> name: [{}]", deployment.getName());
}
```

启动流程实例，关键代码如下

```java
@Autowired
private RuntimeService runtimeService;

/**
 * 启动（初始化）流程实例
 */
@Test
public void initProcessInstance() {
	ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess_Exclusive");
	log.info("==========> 流程定义启动成功，processInstanceId: [{}]，processDefinitionVersion：[{}]", processInstance.getProcessInstanceId(), processInstance.getProcessDefinitionVersion());
}
```

查看任务，关键代码如下

```java
@Autowired
private TaskService taskService;

/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			.taskAssignee("BaJie")
			// .taskAssignee("WuKong")
			// .taskAssignee("TangSeng")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

结果如下

![image-20221129233535084](Activiti7.assets/image-20221129233535084.png)

完成任务，修改 `Part7_Gateway.java` 文件的 `completeTask()` 方法，修改内容如下

```java
/**
 * 完成任务
 */
@Test
public void completeTask() {
	Map<String, Object> variables = new HashMap<>();
	variables.put("day", 100);
	taskService.complete("42c7d496-6ffb-11ed-9eaf-8286f2267041", variables);
	log.info("==========> 任务已完成！");
}
```

查看任务，关键代码如下

```java
@Autowired
private TaskService taskService;

/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			// .taskAssignee("BaJie")
			.taskAssignee("WuKong")
			// .taskAssignee("TangSeng")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

执行 `void getTasksByAssignee()` 结果是 WuKong 没有对应的任务需要处理。

查看任务，关键代码如下

```java
@Autowired
private TaskService taskService;

/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			// .taskAssignee("BaJie")
			// .taskAssignee("WuKong")
			.taskAssignee("TangSeng")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

执行 `void getTasksByAssignee()` 结果如下

![image-20221129234426985](Activiti7.assets/image-20221129234426985.png)

## 4、包容网关-案例

在 `resources/bpmn` 目录下新建 `Part7_Inclusive.bpmn20.xml` 文件，内容如下

![image-20221130000417363](Activiti7.assets/image-20221130000417363.png)

![image-20221129235638990](Activiti7.assets/image-20221129235638990.png)

![image-20221129235728148](Activiti7.assets/image-20221129235728148.png)

![image-20221129235819417](Activiti7.assets/image-20221129235819417.png)

![image-20221129235908460](Activiti7.assets/image-20221129235908460.png)

![image-20221129235954548](Activiti7.assets/image-20221129235954548.png)

![image-20221130000044680](Activiti7.assets/image-20221130000044680-16697376460192.png)

![image-20221130000142983](Activiti7.assets/image-20221130000142983.png)

对 `Part7_Inclusive.bpmn20.xml` 文件进行流程部署并启动流程实例，关键代码如下

```java
@Autowired
private RepositoryService repositoryService;
@Autowired
private RuntimeService runtimeService;

/**
 * 初始化流程部署
 */
@Test
public void initDeploymentBPMN() {
	Deployment deployment = repositoryService.createDeployment()
			// 设置 BPMN 文件
			.addClasspathResource("bpmn/Part7_Inclusive.bpmn20.xml")
			// 设置流程部署名称
			.name("流程部署测试包含网关")
			.deploy();
	log.info("==========> name: [{}]", deployment.getName());
}


/**
 * 启动（初始化）流程实例
 */
@Test
public void initProcessInstance() {
	ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess_Inclusive");
	log.info("==========> 流程定义启动成功，processInstanceId: [{}]，processDefinitionVersion：[{}]", processInstance.getProcessInstanceId(), processInstance.getProcessDefinitionVersion());
}
```

先执行 `void initDeploymentBPMN()` 再执行 `void initProcessInstance()`。

查看 BaJie 的待办任务，关键代码如下

```java
@Autowired
private TaskService taskService;

/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			.taskAssignee("BaJie")
			// .taskAssignee("WuKong")
			// .taskAssignee("TangSeng")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

执行 `void getTasksByAssignee()` 结果如下

![image-20221130000940438](Activiti7.assets/image-20221130000940438.png)

让 BaJie 完成任务内，修改 `Part7_Gateway.java` 文件，修改内容如下

```java
/**
 * 完成任务
 */
@Test
public void completeTask() {
	Map<String, Object> variables = new HashMap<>();
	variables.put("day", 1);
	taskService.complete("bef787b3-6fff-11ed-a5ee-8286f2267041", variables);
	log.info("==========> 任务已完成！");
}
```

执行 `void completeTask()`。

查看 ShangSeng 的待办任务，关键代码如下

```java
@Autowired
private TaskService taskService;

/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			// .taskAssignee("BaJie")
			.taskAssignee("ShangSeng")
			// .taskAssignee("WuKong")
			// .taskAssignee("TangSeng")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

执行 `void getTasksByAssignee()` ，结果如下

![image-20221130001353209](Activiti7.assets/image-20221130001353209.png)

查看 WuKong 的待办任务，关键代码如下

```java
@Autowired
private TaskService taskService;

/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			// .taskAssignee("BaJie")
			// .taskAssignee("ShangSeng")
			.taskAssignee("WuKong")
			// .taskAssignee("TangSeng")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

执行 `void getTasksByAssignee()` ，结果如下

![image-20221130001545078](Activiti7.assets/image-20221130001545078.png)

查看 TangSeng 的待办任务，关键代码如下

```java
@Autowired
private TaskService taskService;

/**
 * 查看我的待办任务
 */
@Test
public void getTasksByAssignee() {
	List<Task> list = taskService
			// 创建任务查询对象
			.createTaskQuery()
			// 设置要查询的处理人
			// .taskAssignee("BaJie")
			// .taskAssignee("ShangSeng")
			// .taskAssignee("WuKong")
			.taskAssignee("TangSeng")
			// 获取所有的任务
			.list();
	list.forEach(task -> log.info("==========> id: [{}], name: [{}], assignee: [{}]", task.getId(), task.getName(), task.getAssignee()));
}
```

执行 `void getTasksByAssignee()` 结果是 TangSeng 没有对应的任务需要处理。



# Activiti7 新特性

Activiti7 的新特性要基于 SpringSecurity 框架使用，如果项目中没有使用 SpringSecurity 框架，又要使用 Activiti7 就用 [Activiti7 核心类](# Activiti7 核心类) 的相关内容。

## 1、准备工作

新建 `SecurityUtil.java` 文件，内容如下

```java
package com.zgy.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/11/30
 */
@Slf4j
@Component
public class SecurityUtil {

	@Autowired
	private UserDetailsService userDetailsService;

	/**
	 * 模拟用户登录
	 *
	 * @param username 用户名
	 */
	public void logInAs(String username) {
		UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		if (Objects.isNull(userDetails)) {
			throw new IllegalStateException("User " + username + " doesn't exist, please provide a valid user");
		}
		log.info("==========> 用户: [{}] 登录成功！", username);
		SecurityContextHolder.setContext(new SecurityContextImpl(new Authentication() {
			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				return userDetails.getAuthorities();
			}

			@Override
			public Object getCredentials() {
				return userDetails.getPassword();
			}

			@Override
			public Object getDetails() {
				return userDetails;
			}

			@Override
			public Object getPrincipal() {
				return userDetails;
			}

			@Override
			public boolean isAuthenticated() {
				return true;
			}

			@Override
			public void setAuthenticated(boolean b) throws IllegalArgumentException {

			}

			@Override
			public String getName() {
				return userDetails.getUsername();
			}
		}));
	}
}
```

新建 `DemoApplicationConfiguration.java` 文件，内容如下

```java
package com.zgy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/11/30
 */
@Slf4j
@Configuration
public class DemoApplicationConfiguration {

	@Bean
	public UserDetailsService myUserDetailsService() {
		InMemoryUserDetailsManager detailsManager = new InMemoryUserDetailsManager();
		String[][] usersGroupsAndRoles = {
				{"admin", "123456", "ROLE_ACTIVITI_USER"},
				{"BaJie", "123456", "ROLE_ACTIVITI_USER"},
				{"ShaSeng", "123456", "ROLE_ACTIVITI_USER"},
				{"WuKong", "123456", "ROLE_ACTIVITI_USER"},
				{"TangSeng", "123456", "ROLE_ACTIVITI_USER"},
		};

		for (String[] user : usersGroupsAndRoles) {
			List<String> authoritiesStrings = Arrays.asList(Arrays.copyOfRange(user, 2, user.length));
			log.info("==========> 用户: {} 的角色有: {}", user[0], authoritiesStrings);
			detailsManager.createUser(new User(user[0], passwordEncoder().encode(user[1]), authoritiesStrings.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())));
		}

		return detailsManager;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
```

修改 `UserDetailsServiceImpl.java` 文件，修改内容如下

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
// @Service
public class UserDetailsServiceImpl implements UserDetailsService {

	@Override
	public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
		return null;
	}
}
```

**Tips**: 在 Activiti7 中，用户必须要拥有 `ROLE_ACTIVITI_USER` 角色才能执行相关的操作。

## 2、API 新特性-ProcessRuntime

ProcessRuntime 包含流程定义和流程实例相关的 API。

在 `resources/bpmn` 目录下新建 `Part8_ProcessRuntime.bpmn20.xml` 文件，内容如下

![image-20221130144933331](Activiti7.assets/image-20221130144933331.png)

![image-20221130145024611](Activiti7.assets/image-20221130145024611.png)

对 `Part8_ProcessRuntime.bpmn20.xml` 文件进行流程部署，关键代码如下

```java
@Autowired
private RepositoryService repositoryService;

/**
 * 初始化流程部署
 */
@Test
public void initDeploymentBPMN() {
	Deployment deployment = repositoryService.createDeployment()
			// 设置 BPMN 文件
			.addClasspathResource("bpmn/Part8_ProcessRuntime.bpmn20.xml")
			// 设置流程部署名称
			.name("流程部署测试 processRuntime")
			.deploy();
	log.info("==========> name: [{}]", deployment.getName());
}
```

执行 `initDeploymentBPMN`。

新建 `Part8_ProcessRuntime.java` 文件，内容如下

```java
package com.zgy;

import lombok.extern.slf4j.Slf4j;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.builders.StartProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.SuspendProcessPayload;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/11/30
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Part8_ProcessRuntime {

	@Autowired
	private ProcessRuntime processRuntime;

	/**
	 * 获取流程实例
	 */
	@Test
	public void getProcessInstance() {
		Page<ProcessInstance> page = processRuntime.processInstances(Pageable.of(0, 100));
		log.info("==========> 流程实例数量为: {}", page.getTotalItems());
		List<ProcessInstance> instanceList = page.getContent();
		for (ProcessInstance instance : instanceList) {
			log.info("==========> id: [{}], name: [{}], processDefinitionKey: [{}], startDate: [{}], status: [{}]",
					instance.getId(), instance.getName(), instance.getProcessDefinitionKey(), instance.getStartDate(), instance.getStatus());
		}
	}

	/**
	 * 启动流程实例
	 */
	@Test
	public void startProcessInstance() {
		securityUtil.logInAs("BaJie");
		ProcessInstance processInstance = processRuntime.start(ProcessPayloadBuilder
				.start()
				.withProcessDefinitionKey("myProcess_ProcessRuntime")
				// .withName("流程实例名称")
				// .withBusinessKey("自定义 bKey")
				// .withVariable("", "")
				.build());
		log.info("==========> 流程实例启动成功，id: [{}], processDefinitionKey: [{}], status: [{}]", processInstance.getId(), processInstance.getProcessDefinitionKey(), processInstance.getStatus());
	}

	/**
	 * 删除流程实例
	 */
	@Test
	public void delProcessInstance() {
		processRuntime.delete(ProcessPayloadBuilder
				.delete()
				.withProcessInstanceId("a10bdfc2-7083-11ed-bcdd-8286f2267041")
				.build());
		log.info("==========> 流程实例删除成功！");
	}

	/**
	 * 挂起流程实例
	 */
	@Test
	public void suspendProcessInstance() {
		processRuntime.suspend(ProcessPayloadBuilder
				.suspend()
				.withProcessInstanceId("42c40402-6ffb-11ed-9eaf-8286f2267041")
				.build());
		log.info("==========> 流程实例挂起成功！");
	}

	/**
	 * 激活流程实例
	 */
	@Test
	public void resumeProcessInstance() {
		processRuntime.resume(ProcessPayloadBuilder
				.resume()
				.withProcessInstanceId("42c40402-6ffb-11ed-9eaf-8286f2267041")
				.build());
		log.info("==========> 流程实例激活成功！");
	}

	/**
	 * 获取流程实例参数
	 */
	@Test
	public void getVariables() {
		List<VariableInstance> variables = processRuntime.variables(ProcessPayloadBuilder
				.variables()
				.withProcessInstanceId("42c40402-6ffb-11ed-9eaf-8286f2267041")
				.build());

		for (VariableInstance variable : variables) {
			log.info("==========> taskId: [{}], processInstanceId: [{}], name: [{}]", variable.getTaskId(), variable.getProcessInstanceId(), variable.getName());
		}
	}
}
```

**Tips**: 

- 启动流程实例在 `activiti-dependencies` 的 `7.1.0.M6` 版本下，存在BUG，暂时还没有被修复。因此，启动流程实例，要么降到 `7.1.0.M4` 版本，要么使用 [3.1、启动流程实例](# 3.1、启动流程实例) 的方法来实现。
- `7.1.0.M6` 与 `7.1.0.M4` 数据库表结构是不同的，降到 `7.1.0.M4` 版本后，项目将启动不起来。
- `securityUtil.logInAs("BaJie")` 在 `ProcessRuntime` API 上没有实际效果。

## 3、API 新特性-TaskRuntime

TaskRuntime 包含任务相关 API。

新建 `Part9_TaskRuntime.java` 文件，内容如下

```java
package com.zgy;

import com.zgy.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.runtime.TaskRuntime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/11/30
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Part9_TaskRuntime {

	@Autowired
	private SecurityUtil securityUtil;
	@Autowired
	private TaskRuntime taskRuntime;

	/**
	 * 获取当前登录用户任务
	 */
	@Test
	public void getTasks() {
		securityUtil.logInAs("WuKong");
		Page<Task> page = taskRuntime.tasks(Pageable.of(0, 100));
		List<Task> taskList = page.getContent();
		for (Task task : taskList) {
			log.info("==========> id: [{}], name: [{}], status: [{}], createDate: [{}]", task.getId(), task.getName(), task.getStatus(), task.getCreatedDate());
			if (Objects.isNull(task.getAssignee())) {
				log.info("==========> 当前登录用户并非是该任务的执行人，而是候选人，因此需要进行任务拾取");
			} else {
				log.info("==========> 当前用户{}是该任务的执行人", task.getAssignee());
			}
			log.info("========================================");
		}
	}

	/**
	 * 让当前登录用户完成任务
	 */
	@Test
	public void completeTask() {
		securityUtil.logInAs("WuKong");
		Task task = taskRuntime.task("e3ebbb08-6b3c-11ed-99b4-8086f2267041");
		if (Objects.isNull(task.getAssignee())) {
			log.info("==========> 当前登录用户并非是该任务的执行人，而是候选人，因此需要进行任务拾取");
			taskRuntime.claim(TaskPayloadBuilder
					.claim()
					.withTaskId(task.getId())
					.build());
			log.info("==========> 任务拾取成功！");
		}
		taskRuntime.complete(TaskPayloadBuilder
				.complete()
				.withTaskId(task.getId())
				.build());
		log.info("==========> 任务已完成！");
	}
}
```

## 4、SpringSecurity 集成

Activiti7 的 Maven 依赖包中默认就引入了 SpringSecurity 相关依赖，因此不需要额外引入 SpringSecurity 相关依赖。

### 4.1、最简实现

删除 `UserDetailsServiceImpl.java` 文件，修改 `DemoApplicationConfiguration.java` 文件，修改内容如下

```java
package com.zgy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/11/30
 */
@Slf4j
// @Configuration
public class DemoApplicationConfiguration {

	@Bean
	public UserDetailsService myUserDetailsService() {
		InMemoryUserDetailsManager detailsManager = new InMemoryUserDetailsManager();
		String[][] usersGroupsAndRoles = {
				{"admin", "123456", "ROLE_ACTIVITI_USER"},
				{"BaJie", "123456", "ROLE_ACTIVITI_USER"},
				{"ShaSeng", "123456", "ROLE_ACTIVITI_USER"},
				{"WuKong", "123456", "ROLE_ACTIVITI_USER"},
				{"TangSeng", "123456", "ROLE_ACTIVITI_USER"},
		};

		for (String[] user : usersGroupsAndRoles) {
			List<String> authoritiesStrings = Arrays.asList(Arrays.copyOfRange(user, 2, user.length));
			log.info("==========> 用户: {} 的角色有: {}", user[0], authoritiesStrings);
			detailsManager.createUser(new User(user[0], passwordEncoder().encode(user[1]), authoritiesStrings.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())));
		}

		return detailsManager;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
```

新建 `HelloController.java` 文件，内容如下

```java
package com.zgy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/11/30
 */
@RestController
public class HelloController {

	@GetMapping("/hello")
	public ResponseEntity<String> hello() {
		return ResponseEntity.ok("Activiti7 入门到放弃！");
	}
}
```

新建 `SecurityConfig.java` 文件，内容如下

```java
package com.zgy.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/12/1
 */
@Configuration
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
```

新建 `MyUserDetailsService.java` 文件，内容如下

```java
package com.zgy.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/11/30
 */
@Slf4j
@Component
public class MyUserDetailsService implements UserDetailsService {

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = new User(
				// 登录用户名
				username,
				// 密码一定要加密，否则无法认证成功
				passwordEncoder.encode("123456"),
				// 角色列表
				AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_ACTIVITI_USER")
		);

		return user;
	}
}
```

启动服务，访问 http://localhost:9090/hello，效果如下

![image-20221201100055831](Activiti7.assets/image-20221201100055831.png)

输入账号：user，密码：123456，效果如下

![image-20221201100435311](Activiti7.assets/image-20221201100435311.png)

### 4.2、集成数据库

修改 `pom.xml` 文件，新增内容如下

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.1.4</version>
    <exclusions>
        <exclusion>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.9</version>
</dependency>
```

新建 `UserInfo.java` 文件，内容如下

```java
package com.zgy.pojo;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/12/1
 */
@Data
public class UserInfo implements UserDetails {
	/**
	 * 唯一标识id
	 */
	private Long id;

	/**
	 * 姓名
	 */
	private String name;

	/**
	 * 用户名
	 */
	private String username;

	/**
	 * 地址
	 */
	private String address;

	/**
	 * 密码
	 */
	private String password;

	/**
	 * 角色
	 */
	private String roles;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		if (StringUtils.isBlank(roles)) {
			return Collections.emptyList();
		}

		return Arrays.stream(roles.split(",")).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
```

修改 `MyUserDetailsService.java` 文件，修改内容如下

```java
package com.zgy.security;

import com.zgy.mapper.UserInfoMapper;
import com.zgy.pojo.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/11/30
 */
@Slf4j
@Component
public class MyUserDetailsService implements UserDetailsService {

	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private UserInfoMapper userInfoMapper;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		/*User user = new User(
				// 登录用户名
				username,
				// 密码一定要加密，否则无法认证成功
				passwordEncoder.encode("123456"),
				// 角色列表
				AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_ACTIVITI_USER")
		);

		return user;*/

		UserInfo userInfo = userInfoMapper.selectByUsername(username);
		if (Objects.isNull(userInfo)) {
			throw new UsernameNotFoundException("用户不存在");
		}

		return userInfo;
	}
}
```

新建 `UserInfoMapper.java` 文件，内容如下

```java
package com.zgy.mapper;

import com.zgy.pojo.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/12/1
 */
@Mapper
public interface UserInfoMapper {

	@Select("SELECT * FROM user_info WHERE username = #{username}")
	UserInfo selectByUsername(String username);
}
```

创建 `user_info` 表，SQL 脚本如下

```sql
CREATE TABLE `user_info` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `username` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `address` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '地址',
  `password` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `roles` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色列表',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户信息表';

INSERT INTO `activiti7_lab`.`user_info`(`id`, `name`, `username`, `address`, `password`, `roles`) VALUES (1, '八戒', 'BaJie', '高老庄', '$2a$10$Henx4gLA2PZPAEwPWKhsB.4XMSINYLCBAuIlm2Yz9mevfXdEq2pEK', 'ROLE_ACTIVITI_USER');
INSERT INTO `activiti7_lab`.`user_info`(`id`, `name`, `username`, `address`, `password`, `roles`) VALUES (2, '悟空', 'WuKong', '花果山', '$2a$10$Henx4gLA2PZPAEwPWKhsB.4XMSINYLCBAuIlm2Yz9mevfXdEq2pEK', 'ROLE_ACTIVITI_USER');
INSERT INTO `activiti7_lab`.`user_info`(`id`, `name`, `username`, `address`, `password`, `roles`) VALUES (3, '沙僧', 'ShangSeng', '流沙河', '$2a$10$Henx4gLA2PZPAEwPWKhsB.4XMSINYLCBAuIlm2Yz9mevfXdEq2pEK', 'ROLE_ACTIVITI_USER');
INSERT INTO `activiti7_lab`.`user_info`(`id`, `name`, `username`, `address`, `password`, `roles`) VALUES (4, '唐僧', 'TangSeng', '东土大唐', '$2a$10$Henx4gLA2PZPAEwPWKhsB.4XMSINYLCBAuIlm2Yz9mevfXdEq2pEK', 'ROLE_ACTIVITI_USER,ROLE_ADMIN');
```

启动服务，访问 http://localhost:9090/hello，效果如下

![image-20221201100055831](Activiti7.assets/image-20221201100055831.png)

输入账号：BaJie，密码：123456，效果如下

![image-20221201100435311](Activiti7.assets/image-20221201100435311.png)

### 4.3、SpringSecurity 配置相关

新建 `LoginSuccessHandler.java` 文件，内容如下

```java
package com.zgy.security;

import cn.hutool.json.JSONUtil;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/12/1
 */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
		HashMap<String, Object> result = new HashMap<>();
		result.put("code", 200);
		result.put("data", "用户 " + authentication.getName() + " 登录成功");
		response.getWriter().write(JSONUtil.toJsonStr(result));
	}
}
```

新建 `LoginFailureHandler.java` 文件，内容如下

```java
package com.zgy.security;

import cn.hutool.json.JSONUtil;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/12/1
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
		response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
		HashMap<String, Object> result = new HashMap<>();
		result.put("code", 500);
		result.put("data", "登录失败，错误信息为：" + e.getMessage());
		response.getWriter().write(JSONUtil.toJsonStr(result));
	}
}
```

新建 `SecurityController.java` 文件，内容如下

```java
package com.zgy.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/12/2
 */
@RestController
@RequestMapping
public class SecurityController {

	@GetMapping("/login.html")
	public ResponseEntity<Map<String, Object>> login() {
		Map<String, Object> result = new HashMap<>();
		result.put("code", 200);
		result.put("data", "这个是登录页面。。。。");
		return ResponseEntity.ok(result);
	}
}
```

修改 `SecurityConfig.java` 文件，修改内容如下

```java
package com.zgy.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * <p>
 *
 * @author ZhangGuoYuan
 * @since 2022/12/1
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private LoginSuccessHandler loginSuccessHandler;
	@Autowired
	private LoginFailureHandler loginFailureHandler;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.
				// 表单登录设置
				formLogin()
				// 设置登录请求路径，当未鉴权时会跳转的页面
				.loginPage("/login.html").permitAll()
				// 自定义登录的处理路径，默认是 /login，表单在提交时，action 的值要和这个值一致
				.loginProcessingUrl("/login.do").permitAll()
				// 设置登录成功后的处理逻辑
				.successHandler(loginSuccessHandler)
				// 设置登录失败后的处理逻辑
				.failureHandler(loginFailureHandler)
				.and()
				// 权限设置
				.authorizeRequests()
				// 设置任意请求都不用鉴权
				.anyRequest().authenticated()
				.and()
				// 退出登录设置
				.logout()
				// 不用鉴权
				.permitAll()
				.and()
				// 禁用 csrf
				.csrf().disable()
				// 请求头设置
				.headers()
				// 禁用 X-Frame-Options
				.frameOptions().disable();
	}
}
```

启动服务，使用浏览器访问 http://localhost:9090/hello，效果如下

![image-20221202234918547](Activiti7.assets/image-20221202234918547.png)

使用 Postman 访问 http://localhost:9090/login.do 输入一个错误的密码，效果如下

![image-20221202235407677](Activiti7.assets/image-20221202235407677.png)

使用 Postman 访问 http://localhost:9090/login.do 输入一个正确的密码，效果如下

![image-20221202235437806](Activiti7.assets/image-20221202235437806.png)

## 5、BPMN-JS 整合

