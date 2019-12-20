<center><h1>利用 Redisson 实现分布式锁详解</h1></center>

# 资料一（Redisson分布式锁浅析）

本篇主要是对以下几个方面进行了探索

- Maven配置
- RedissonLock简单示例
- 源码中使用到的Redis命令
- 源码中使用到的lua脚本语义
- 源码分析

**Maven配置**

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>2.2.12</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-annotations</artifactId>
    <version>2.6.0</version>
</dependency>
```

**RedissonLock简单示例**

redission支持4种连接redis方式，分别为单机、主从、Sentinel、Cluster 集群，项目中使用的连接方式是Sentinel。

**Sentinel配置**

```java
Config config = new Config();
config.useSentinelServers().addSentinelAddress("127.0.0.1:6479", "127.0.0.1:6489").setMasterName("master").setPassword("password").setDatabase(0);
RedissonClient redisson = Redisson.create(config);`
```

