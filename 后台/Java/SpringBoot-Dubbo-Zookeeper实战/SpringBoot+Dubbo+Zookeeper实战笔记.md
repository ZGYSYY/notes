<center><h1>SpringBoot+Dubbo+Zookeeper实战笔记</h1></center>
# 技术栈区别

Spring Boot + Spring Cloud：组件多，功能完备。

Spring Boot + Dubbo + Zookeeper：组件少，功能不太完备。

# 什么是高可用

- 一直可以使用
- 能支持高并发
- 具有高性能

# 四大问题

1. 客户端如何访问这么多的服务

   API 网关

2. 服务与服务之间如何通信

   - 同步通信

     HTTP（Apache Http Client）

     RPC（Dubbo（只支持Java）、Apache Thrift、gRPC）

   - 异步通信

     消息队列（Kafka，RabbitMQ、RocketMQ）

3. 这么多服务，如何管理

   - 服务治理
     - 服务注册与发现
       - 基于客户端的服务注册与发现，Zookeeper
       - 基于服务端的服务注册与发现，Eureka

4. 服务挂了或者网络不可靠怎么办

   - 重试机制
   - 服务熔断
   - 服务降级
   - 服务限流

PS：其实最大问题就是网络不可靠。

# 什么是Zookeeper

Zookeeper是一种**分布式协调服务**，用于管理大型主机。在分布式环境中协调和管理服务是一个复杂的过程。Zookeeper通过其简单的架构和API解决了这个问题。Zookeeper允许开发人员专注于核心应用程序逻辑，而不必担心应用程序的分布式特性。

# 分布式锁应具备条件

- 在分布式系统环境下，一个方法在同一个时间内只能被一个机器的一个线程调用。
- 高可用的获取锁和释放锁。
- 高性能的获取锁和释放锁。
- 具备可重入特性
- 具备锁失效机制，防止出现死锁
- 具备非阻塞特性，即没有获取到锁立即返回获取锁失败。

# 分布式锁实现

- Zookeeper
- Memcache
- Redis
- Chubby

## 通过Redis来实现分布式锁

当有多个服务同时向Redis获取锁的时候，会遵循下面的步骤

1. 加锁：使用命令`setnx`往Redis中存放一个数据（标志位），该命令会有一个返回值，如果返回值大于0，表示加锁成功，如果返回值等于0，表示加锁失败。
2. 释放锁：使用`del`命令删除对应的数据。
3. 锁超时：为了防止某些特殊情况下，锁没有得到释放，因此需要实现一个在规定时间内，如果锁没有被释放，将自动调用`del`命令来释放锁。

# Zookeeper两大功能

1. 分布式锁。
2. 服务注册与发现。

# Zookeeper的数据模型

Zookeeper的数据模型像数据结构中的树，也很像linux中的文件系统目录。

![1568860598517](SpringBoot+Dubbo+Zookeeper实战笔记.assets/1568860598517.png)

上图中，**一个正方形代表一个节点（Znode）**。在Zookeeper中访问节点数据是通过路径来访问的，比如`/user/list`表示的是获取user下list节点数据。

Znede中包含的元素如下

- data：Znode存储的数据信息。
- ACL：记录Znode的访问权限，即哪些人或哪些IP可以访问本节点。
- stat：包含Znode的各种元数据，比如事务ID、版本号、时间戳、大小等。
- child：当前节点的子节点引用。

需要注意一点，Zookeeper是为读多写少的场景所设计。Znode并不是用来存储大规模业务数据的地方，而是用于存储少量的状态和配置信息的地方，所以规定**每个节点的数据大小不能超过1MB**。

# Zookeeper的基本操作

- 创建节点：create
- 删除节点：delete
- 判断节点是否存在：exists
- 获得一个节点的数据：getData
- 设置一个节点的数据：setData
- 获取节点下的所有子节点：getChildren

在上面的这些操作中，`getData`、`exists`、`getChildren`属于读操作，Zookeeper客户端在进行读操作的时候，可以选择是否设置`watch`选项，通过Zookeeper的**事件通知机制**，在服务端中，当被Zookeeper客户端watch的节点被修改（删除、修改）时，Zookeeper服务端会异步通知Zookeeper客户端当前数据被修改了。

# Zookeeper的事件通知机制

当用户访问某个服务的时候，会在Gateway（网关）中根据请求路径（如：user/info）来确定用户需要访问哪个服务，并确定该服务所对应的服务器IP地址，如果在网关中没有找到所访问路径的服务器IP地址，在Gateway中就会向Zookeeper服务器中发送getData(路径,whach)命令，在Zookeeper服务器获取对应路径的IP地址，并将查询的IP地址在Gateway（网关）中保存一份，这样就可以在下次请求该路径时，就不会再访问Zookeeper服务器。因为Gateway在向向Zookeeper服务器中发送getData命令的时候添加了`whach`选项，当我们的某个服务器宕机的时候，Zookeeper服务器就会异步通知Gateway，告诉Gateway哪台机器宕机了，这时Gateway就会在自己内部保存的IP地址中将那台宕机的IP地址删除掉。

# Zookeeper集群

Zookeeper集群是为了防止Zookeeper服务器宕机，导致所有服务不可用。

Zookeeper集群是一个一主多从结构。在更新数据时，首先会更新主节点（这里的节点指的是服务器，不是Znode），然后再从主节点将数据复制到从节点。在读取数据的时候，直接读取任意节点。

为了保证主从节点的数据一致性问题，Zookeeper采用了ZAB协议来保证数据的一致性。

[参考：ZAB协议详解](https://dbaplus.cn/news-141-1875-1.html)

## Dcker-compose安装

[参考：Docker-compose](https://docs.docker.com/compose/install/)

# Docker中安装Zookeeper集群

在`/usr/local/`创建`docker`目录和`Zookeeper`目录

```bash
cd /usr/local
mkdir docker
cd docker
mkdir zookeeper
```

创建docker-compose.yml文件，内容如下

```bash
version: '3.1'

services:
  zoo1:
    image: zookeeper
    restart: always
    hostname: zoo1
    ports:
      - 2181:2181
    environment:
      ZOO_MY_ID: 1
      ZOO_SERVERS: server.1=0.0.0.0:2888:3888;2181 server.2=zoo2:2888:3888;2181 server.3=zoo3:2888:3888;2181

  zoo2:
    image: zookeeper
    restart: always
    hostname: zoo2
    ports:
      - 2182:2181
    environment:
      ZOO_MY_ID: 2
      ZOO_SERVERS: server.1=zoo1:2888:3888;2181 server.2=0.0.0.0:2888:3888;2181 server.3=zoo3:2888:3888;2181

  zoo3:
    image: zookeeper
    restart: always
    hostname: zoo3
    ports:
      - 2183:2181
    environment:
      ZOO_MY_ID: 3
      ZOO_SERVERS: server.1=zoo1:2888:3888;2181 server.2=zoo2:2888:3888;2181 server.3=0.0.0.0:2888:3888;2181
```

使用docker-compose创建容器并启动

```bash
docker-compose up -d
```

查看Zookeeper配置文件

```bash
# 进入容器内部
docker exec -it zookeeper_zoo1_1 /bin/bash
# 查看配置文件
cat /conf/zoo.cfg
```

配置文件内容如下：

```bash
# Zookeeper数据文件目录
dataDir=/data
# Zookeeper日志文件目录
dataLogDir=/datalog
# 这个时间是作为Zookeeper服务器与服务器或客户端与服务器之间维持心跳的间隔，也就是每隔tickTime时间就会发送一个心跳，单位为毫秒。
tickTime=2000
# 配置Zookeeper的主节点在初始化连接时最长能忍受从节点服务器多少个心跳时间间隔数，时间为initLimit*tickTime，单位为毫秒。
initLimit=5
# 配置Leader与Follower之间发送消息，请求和应答时间长度，最长不能超过syncLimit*tickTime的时间长度，，单位为毫秒。
syncLimit=2
autopurge.snapRetainCount=3
autopurge.purgeInterval=0
# 限制连接到Zookeeper的客户端数量，限制并发连接的数量，它通过IP来区分不同的客户端。此配置选项可以用来阻止某些类别的DOS攻击。将它设置为0或忽略而不进行设置将会取消对并发连接的限制。
maxClientCnxns=60
standaloneEnabled=true
admin.enableServer=true
# Server.A=B:C:D;E详解
# A：一个数字，表示第几号服务器。
# B：集群中当前服务器的IP地址，也可以是计算机名称。
# C：表示这个服务器与集群中loader服务器交换信息的端口。
# D：表示万一集群中loader服务器宕机了，需要一个端口来进行重新loader选举，而这个端口就是用来执行选举时，服务器相互通信的端口。
# E：对Zookeepercline端提供服务的端口
server.1=0.0.0.0:2888:3888;2181
server.2=zoo2:2888:3888;2181
server.3=zoo3:2888:3888;2181
```

测试

```bash
# 进入容器内部
docker exec -it zookeeper_zoo3_1 /bin/bash
# 进入/apache-zookeeper-3.5.5-bin/bin目录
cd /apache-zookeeper-3.5.5-bin/bin
# 启动Zookeeper客户端并连接Zookeeper服务端
# -server localhost:2181：Zookeeper服务器地址为localhost，端口号为2181
./zkCli.sh -server localhost:2181
# 创建一条数据
create /test "hello zookeeper"
# 获取创建的数据
get /test
# 删除数据
delete /test
```

**参考文档如下**：

> [什么是Zookeeper？](../什么是ZooKeeper/什么是ZooKeeper.md)

# Dubbo

Apache Dubbo是一款高性能、轻量级的开源Java RPC分布式服务框架，它提供了三大核心能力：

1. 面向接口的远程调用
2. 智能容错和负载均衡
3. 服务自动注册和发现

[Dubbo官方网站](http://dubbo.apache.org/zh-cn/docs/user/references/protocol/dubbo.html)

[Dubbo github](https://github.com/apache/dubbo)

## Dubbo五大角色

1. Provider：暴露服务的服务提供者。
2. Consumer：调用远程服务的服务消费者。
3. Registry：服务注册与发现的注册中心。
4. Monitor：统计服务的调用次数和调用时间的监控中心。
5. Container：服务运行容器。

调用关系说明：

- 服务容器`Container`负责启动、加载、运行服务提供者。
- 服务提供者`Provider`在启动时，向注册中心注册自己提供的服务。
- 服务消费者`Consumer`在启动时，向注册中心订阅自己所需的服务。
- 注册中心`Registry`返回服务提供者地址列表给消费者，如果有变更，注册中心将基于长连接推送变更数据给消费者。
- 服务消费者`Consumer`从提供者地址列表中，基于软负载均衡算法，选一台提供者进行调用，如果调用失败，再选另一台调用。
- 服务消费者`Consumer`和提供者`Provider`，在内存中累计调用次数和调用时间，定时每分钟发送一次统计数据到监控中心`Monitor`。

## Dubbo管理控制台

[Dubbo管理控制台安装教程](https://github.com/apache/dubbo-admin)

## Dubbo负载均衡

在application.properties中添加如下配置：

```properties
# 开启服务提供者负载均衡
# random：随机
# roundrobin：轮询
# leastactive：最少活跃调用数
# consistenthash：一致性Hash
dubbo.provider.loadbalance=roundrobin
```

## Kryo高速序列化

服务提供者和服务消费者中添加如下依赖

```xml
<dependency>
    <groupId>de.javakaffee</groupId>
    <artifactId>kryo-serializers</artifactId>
</dependency>
```

在application.properties中添加如下配置

```properties
dubbo.protocol.serialization=kryo
```

要让Kryo和FST完全发挥出高性能，最好将那些需要被序列化的类注册到dubbo系统中，例如我们可以实现如下回调接口：

```java
public class SerializationOptimizerImpl implements SerializationOptimizer {

    @Override
    public Collection<Class> getSerializableClasses() {
        List<Class> classes = new LinkedList<>();
        // classes.add(需要被序列化的类.class);
        return classes;
    }
}
```

在application.properties中添加如下配置

```properties
# SerializationOptimizerImpl应该是全限定名，比如：com.zgy.my.shop.commons.dubbo.config.SerializationOptimizerImpl
dubbo.protocol.optimizer=SerializationOptimizerImpl
```

**ps**：在对一个类做序列化的时候，可能还级联引用到很多类，比如java集合类。针对这种情况，Kryo已经自动将JDK中常用的类，进行了注册，所以不需要重复注册它们（当然重复注册也没有任何影响），包括：

```java
GregorianCalendar  
InvocationHandler  
BigDecimal  
BigInteger  
Pattern  
BitSet  
URI  
UUID  
HashMap  
ArrayList  
LinkedList  
HashSet  
TreeSet  
Hashtable  
Date  
Calendar  
ConcurrentHashMap  
SimpleDateFormat  
Vector  
BitSet  
StringBuffer  
StringBuilder  
Object  
Object[]  
String[]  
byte[]  
char[]  
int[]  
float[]  
double[]  
```

由于注册被序列化的类仅仅是出于性能优化的目的，所以即使你忘记了注册某些类也没有关系。事实上，即使不注册任何类，Kryo和FST的性能依然普遍优于hessian和dubbo序列化。

## Hystrix熔断器和仪表盘

### 熔断器简介

在微服务架构中，根据业务来拆分一个个业务，服务与服务之间可以通过RPC相互调用。为了保证其高可用，单个服务会集群部署。由于网络原因和自身原因，服务并不能保证100%可用，如果单个服务出现问题，调用这个服务就会出现线程阻塞，此时若有大量的请求涌入，Servlet容器的线程资源会被消耗完，导致服务瘫痪。由于服务与服务之间存在依赖性，从而导致故障传播，会对整个微服务系统造成灾难性的严重后果，这就是服务故障的“雪崩”效应。

为了解决这个问题，业界提出了熔断器模型。Netflix开源了Hystrix组件，实现了熔断机制，SpringCloud对这一组件进行了整合。在微服务架构中，一个请求需要调用多个服务是非常常见的，如下图：

![1570593048878](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570593048878.png)

当较底层的服务如果出现故障，会导致连锁故障。当对特定的服务的调用的不可用达到一个阀值（Hystrix是5秒20次）熔断器将会被打开。

![1570593185010](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570593185010.png)

熔断器打开后，为了避免连锁故障，通过`fallback`方法可以直接返回一个固定值。

### 使用熔断器

1. 在pom.xml中引入如下依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
</dependency>
```

2. 在Springboot的启动类上添加`@EnableHystrix`注解

3. 在Service中对应的方法上添加`@HystrixCommand`注解，在调用方法上增加`@HystrixCommand`配置，此时调用会经过Hystrix代理

```java
@Service(version = "${services.versions.user.v1}")
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
	// 两个HystrixProperty配置表示：修改阀值为2秒10次
    @HystrixCommand(commandProperties = {
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "10"),
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "2000")
    },fallbackMethod = "selectAllError")
    @Override
    public List<User> selectAll() {
        return userMapper.selectAll();
    }
    
    /**
     * 获取用户失败时，Hystrix回调方法
     * @return
     */
    public List<User> selectAllError() {
        return Collections.emptyList();
    }
}
```

### 使用Hystrix-dashboard熔断器仪表盘

1. 在pom.xml中引入如下依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix-dashboard</artifactId>
</dependency>
```

2. 在Springboot的启动类上添加`@EnableHystrixDashboard`注解

3. 创建hystrix.stream的Servlet配置，添加一个`HystrixDashboardConfiguration`配置类，内容如下：

```java
/**
 * @author ZGY
 * @date 2019/10/8 10:43
 * @description HystrixDashboardConfiguration
 */
@Configuration
public class HystrixDashboardConfiguration {

    @Bean
    public ServletRegistrationBean getServlet() {
        HystrixMetricsStreamServlet streamServlet = new HystrixMetricsStreamServlet();
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(streamServlet);
        registrationBean.setLoadOnStartup(1);
        registrationBean.addUrlMappings("/hystrix.stream");
        registrationBean.setName("HystrixMetricsStreamServlet");
        return registrationBean;
    }
}
```

4. 最后访问测试

# Docker安装Nexus3

[docker安装Nexus3教程](https://hub.docker.com/r/sonatype/nexus3)

docker快速运行命令如下

```bash
docker run -d -p 8081:8081 --name nexus -v /usr/local/docker/nexus-data:/nexus-data --restart=always sonatype/nexus3
```

# Docker安装Gitlab

[Docker安装Gitlab教程](https://docs.gitlab.com/omnibus/docker/README.html)

docker快速运行命令如下

```bash
sudo docker run --detach \
  --hostname gitlab.example.com \
  --publish 5443:443 --publish 580:80 --publish 522:22 \
  --name gitlab \
  --restart always \
  --volume /usr/local/docker/gitlab/config:/etc/gitlab \
  --volume /usr/local/docker/gitlab/logs:/var/log/gitlab \
  --volume /usr/local/docker/gitlab/data:/var/opt/gitlab \
  gitlab/gitlab-ce:latest
```

# 部署CI/CD

## 持续集成的基本概念

持续集成指的是，频繁的（一天多次）将代码集成到主干。它的好处有两个：

1. 快速发现错误。没完成一点更新，就集成到主干，可以快速发现错误，定位错误也比较容易。
2. 防止分支大幅偏离主干。如果不是经常集成，主干又在不断更新，会导致以后集成的难度变大，甚至难以集成。

Martin Fowler说过，“持续集成并不能消除bug，而是让它们非常容易发现和改正。”

持续集成强调开发人员提交新代码之后，立即进行构建、（单元）测试。根据测试结果，我们可以确定新代码和原有代码是否正确的集成在一起。

与持续集成相关的，还有两个概念，分别是**持续交付**和**持续部署**。

**Pipeline**

Pipeline（管道），一次Pipeline其实相当于一次构建任务，里面可以包含多个流程，如安装依赖、运行测试、编译、部署测试服务器等流程。

任何提交或者Merge Request的合并都可以出发Pipeline，如下图所示：

![1570772364348](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570772364348.png)

**Stages**

Stages（阶段），表示构建阶段，就是Pipeline中提到的流程。我们可以在一次Pipeline中定义多个Stages，这些Stages会有以下特点：

- 所有的Stages会按照顺序运行，即当一个Stage运行后，下一个Stage才会开始。
- 只有当所有的Stages都完成后，该构建任务（Pipeline）才会成功。
- 如何任何一个Stage失败，那么后面的Stage不会执行，该构建任务（Pipeline）失败。

因此，Stages和Pipeline的关系如下所示：

![1570772669463](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570772669463.png)

**Jobs**

Jobs（任务），表示构建工作，表示某个Stage里面执行的工作。我们可以在Stages里面定义多个Jobs，这些Jobs会有以下特点：

- 相同Stage中的Jobs会并行执行。
- 相同Stage中的Jobs都执行成功时，该Stage才会成功。
- 如果任何一个Job失败，那么该Stage失败，即构建任务（Pipeline）失败。

所以，Jobs和Stages的关系图如下所示：

![1570772927370](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570772927370.png)



## 在Docker中安装GitLab-Runner实现持续集成

拉取镜像

```bash
docker pull gitlab/gitlab-runner
```

创建容器并运行

```bash
docker run -d --name gitlab-runner --restart always \
  -v /srv/gitlab-runner/config:/etc/gitlab-runner \
  -v /var/run/docker.sock:/var/run/docker.sock \
  gitlab/gitlab-runner:latest
```

容器如果创建成功，并且允许成功，注册gitlab-runner

```bash
docker exec -it 容器id gitlab-runner register
```

根据提示，配置相关数据

![1570765020973](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570765020973.png)

刷新GitLab页面，查看GitLab Runner是否配置成功，成功结果如下：

![1570765137988](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570765137988.png)

**Note**：参考链接

> [docker安装gitLab runner](https://docs.gitlab.com/runner/install/docker.html)
>
> [docker中注册gitLab runner](https://docs.gitlab.com/runner/register/index.html#docker)

在Java项目的根目录下创建`.gitlab-ci.yml`文件，内容如下：

```yml
stages:
  - test

test:
  stage: test
  script:
    - echo "Hello GitLab Runner"
```

然后提交代码到gitLab，在gitLab中找到提交的项目，找到左侧菜单中的`CI/CD`选项，选择`流水线`，查看构建结果，如果出现下面内容，则持续集成成功

![1570767291223](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570767291223.png)

持续集成成功后，可以点击【已通过】按钮，查看详情，如图所示：

![1570767512310](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570767512310.png)

![1570767551518](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570767551518.png)

![1570767621802](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570767621802.png)

还可以进入gitLab runner服务器，进入【home】目录下，找到提交的代码，如图所示：

![1570767711003](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570767711003.png)



**Note**：如果在安装gitLab时，端口映射不是默认的80端口，需要在gitLab runner的配置文件中，添加如下内容：

```toml
clone_url = "ip地址和端口号"
```

##  在Docker中安装GitLab-runner实现持续集成和自动部署

该知识点中，利用gitlab runner基础镜像，重新构建一个新镜像来实现自动部署功能，该镜像中新增如下软件支持：

- jdk
- maven
- docker-cli（docker in docker）

创建gitlab-runner容器，并后台运行

```bash
docker run -d --name="gitlab-runner" gitlab/gitlab-runner
```

进入容器内部

```bash
docker exec -it 容器id /bin/bash
```

查看linux版本，因为gitlab-runner镜像是基于ubuntu镜像创建的镜像，所以需要知道linux版本，用于后面配置apt软件源（配置apt软件源时，不同的版本配置是不一样的，如果配置错误，会导致很多软件包安装失败，超级坑）

```bash
cat /etc/issue
```

通过上面的命令，了解到gitlab-runner是基于**Ubuntu 18.04.3 LTS**镜像创建的镜像，接下来就是配置apt软件源，访问https://opsx.alibaba.com/mirror，操作如下图：

![1570847867973](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570847867973.png)

得到下图结果

![1570847907101](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570847907101.png)

根据自己Ubuntu版本来选择配置，这里我使用的是Ubuntu18.04

```bash
# 备份apt源配置文件
cp /etc/apt/sources.list /etc/apt/sources.list.back
# 编辑apt源配置文件,并保存配置文件
vim /etc/apt/sources.list
```

sources.list配置如下：

```bash
deb http://mirrors.aliyun.com/ubuntu/ bionic main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ bionic-security main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ bionic-updates main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ bionic-proposed main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ bionic-backports main restricted universe multiverse
```

```bash
# 更新软件数据库
apt-get update -y
# 升级已安装软件
apt-get upgrade -y
```

退出容器，将容器外部的【jdk-8u221-linux-x64.tar.gz】拷贝到容器内部

```bash
docker cp /usr/local/soft/jdk-8u221-linux-x64.tar.gz 容器id:/usrl/local/share/
```

进入容器内部

```bash
docker exec -it 容器id /bin/bash
```

安装JDK

```bash
# 在容器内部，进入/usr/local/share/目录
cd /usr/local/share/
# 解压jdk-8u221-linux-x64.tar.gz
tar -zxvf jdk-8u221-linux-x64.tar.gz
# 删除jdk-8u221-linux-x64.tar.gz
rm -rf jdk-8u221-linux-x64.tar.gz
# 配置JAVA_HOME环境变量
vim /etc/profile
```

在【/etc/profile】中添加如下配置，保存并退出：

```bash
export JAVA_HOME=/usr/local/share/jdk1.8.0_221/
export PATH=$PATH:$JAVA_HOME/bin
```

```bash
# 让环境变量生效
source /etc/profile
# 测试JDK环境变量是否配置成功
java -version
```

出现下图所示，表示JDK环境变量配置成功

![1570849596410](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570849596410.png)

> **ps**：解决每次登录docker容器内部，配置的环境变量失效问题
>
> ```bash
> # 进入当前用户家目录
> cd ~
> # 在【.bashrc】文件中追加如下内容，然后保存并退出
> ```
>
> ```bash
> source /etc/profile
> ```

退出容器，将容器外部的【apache-maven-3.6.2-bin.tar.gz】拷贝到容器内部

```bash
docker cp /usr/local/soft/apache-maven-3.6.2-bin.tar.gz 容器id:/usrl/local/share/
```

进入容器内部

```bash
docker exec -it 容器id /bin/bash
```

安装Maven

```bash
# 在容器内部，进入/usr/local/share/目录
cd /usr/local/share/
# 解压apache-maven-3.6.2-bin.tar.gz
tar -zxvf apache-maven-3.6.2-bin.tar.gz
# 删除apache-maven-3.6.2-bin.tar.gz
rm -rf apache-maven-3.6.2-bin.tar.gz
# 配置MAVEN_HOME环境变量
vim /etc/profile
```

在【/etc/profile】中添加如下配置，保存并退出：

```bash
export MAVEN_HOME=/usr/local/share/apache-maven-3.6.2/
export PATH=$PATH:$JAVA_HOME/bin:$MAVEN_HOME/bin
```

```bash
# 让环境变量生效
source /etc/profile
# 测试Maven环境变量是否配置成功
mvn -v
```

出现下图所示，表示JDK环境变量配置成功

![1570851219994](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570851219994.png)

> **ps**：如果想让maven获取jar包速度快些，可以配置maven仓库的地址，该步骤比较简单，所以没有记录。

安装docker-cli，进入容器内部，执行以下命令

```bash
# 安装一些通用工具
apt-get install \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg-agent \
    software-properties-common
# 添加GPG key
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
# 验证GPG key
apt-key fingerprint 0EBFCD88
# 添加docker稳定软件源，add-apt-repository其实就是在/etc/apt/source.list文件中追加一条内容
add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
   $(lsb_release -cs) \
   stable"
# 因为是在docker中安装docker，所以只需安装docker-ce-cli(docker客户端)即可，docker in docker模# 式，内部docker和外部docker是兄弟关系，内部docker实际上操作的也是外部docker的内容
apt-get install docker-ce-cli
```

退出容器，将容器外部的docker-compose拷贝到容器内部

```bash
docker cp /usr/local/soft/docker-compose 容器id:/usrl/local/share/
```

进入容器内部

```bash
docker exec -it 容器id /bin/bash
# 进入docker-compose所在目录
cd /usrl/local/share/
# 给docker-compose添加执行权限
chmod +x docker-compose
# 创建软连接，使其可以在任何地方使用
ln -s /usrl/local/share/docker-compose /usr/bin/docker-compose
# 回到家目录
cd ~
# 测试docker-compose是否安装成功
docker-compose version
```

docker-compose安装成功的结果如下图所示：

![1570900624549](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1570900624549.png)

以上步骤之后，退出容器，将当前容器制作为镜像

```bash
docker commit -m '镜像描述' -a '镜像作者' 镜像id 镜像名:TAG
```

基于上面创建的镜像，创建容器，并运行

```bash
docker run -d --name my-gitlab-runner --restart always \
  -v /srv/gitlab-runner/config:/etc/gitlab-runner \
  -v /var/run/docker.sock:/var/run/docker.sock \
  创建的镜像名:TAG
```

> **Note**：【docker.sock】文件的挂载，很关键，只有挂载该文件到容器内，容器内部的docker-cli（docker客户端）才能使用容器外部的docker服务，这就是一种**Docker in Docker** 模式。

**到这里，我们可以基于上面创建的容器实现自动部署功能**

实现自动部署的基本流程如下：

在上面持续集成中，我们知道，当一个项目持续集成成功后，会在gitlab-runner服务器的【/home/gitlab-runner/builds/???/?】目录下（？：表示不确定的目录），看到我们上传到gitlab的项目代码。

利用Maven对该项目进行打包。

利用docker的Dockerfile将打好的包放入一个java运行环境中，然后在运行即可。

> **ps**：上述步骤中，我们可以使用docker-compose来简化容器的启动和关闭，这意味着要创建一个【docker-compose.yml】文件。当我们需要对某个服务进行监听是否启动时（如果某个服务启动了，我这个服务才能启动成功）的情况时，可以使用github上一个叫【[dockerize](https://github.com/jwilder/dockerize)】的软件来实现，具体实现看代码中Dockerfile的配置，不做过多描述。
>
> 上面的步骤需要掌握docker-compose的基本使用和Dockerfile文件的编写，才能实现。

**单台宿主机，不同容器之间能够互相通信的方法**

让需要互相通信的容器，使用同一块docker虚拟网卡即可。

> **ps**1：创建docker虚拟网卡的命令为：docker network create 网卡名
>
> 查看docker虚拟网卡的命令为：docker network ls
>
> 删除无用docker虚拟网卡命令为：docker network prune，不询问删除可以加一个`-f`。

> **ps**2：容器内的服务是能够访问访问同宿主机同一局域网的ip的，如果不能访问，应该是iptables问题，建议重启服务。

## 使用 Jenkins实现持续交付

### 什么是Jenkins

Jenkins是一个开源软件项目，是基于Java开发的一种持续集成工具，用于监控持续重复的工作，旨在提供一个开放易用的软件平台，使软件的持续集成变成可能。

官方网站： https://jenkins.io/zh/

### 基于Docker安装Jenkins

创建Jenkins目录

```bash
cd /usr/local
mkdir docker
cd docker
mkdir jenkins
```

进入 jenkins 目录,创建 docker-compose.yml 文件，保存并退出 vim ，内容如下：

```yaml
version: '3.6'
services:
  jenkins:
    restart: always
    image: jenkins
    container_name: jenkins
    ports:
      # 发布端口
      - 8080:8080
      # 基于 JNLP 的 Jenkins 代理通过 TCP 端口 50000 与 Jenkins Master 进行通信
      - 50000:50000
    environment:
      TZ: Asia/Shanghai
    volumes:
      - ./data:/var/jenkins_home
```

创建并启动容器

```bash
docker-compose up -d
```

查看容器是否启动成功

```bash
docker ps
```

如果有容器名为 jenkins 的容器，表示启动成功。

> **ps**：查看 jenkins 容器的日志发现如下情况
>
> ![1571107849310](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571107849310.png)
>
> 说明 Jenkins 用户没有数据卷目录 /usr/local/docker/jenkins/data 的权限，需要执行下面的命令来解决问题
>
> ```bash
> # 1000是Jenkins的默认用户id
> chown -R 1000 /usr/local/docker/jenkins/data
> # 关闭容器
> docker-compose down
> # 创建并启动容器
> docker-compose up -d
> ```

访问Jenkins， [http://192.168.1.173:8080](http://192.168.1.173:8080/)，得到如下图

![1571108514453](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571108514453.png)

查看 Jenkins 日志，获取到初始密码，然后填入 Jenkins 页面的输入框中，点击继续按钮

![1571108637701](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571108637701.png)

![1571108773750](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571108773750.png)

![1571108884490](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571108884490.png)

安装如下几个必须的插件

![1571108975665](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571108975665.png)

![1571109014668](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571109014668.png)

点击安装按钮，开始安装，等待插件的安装，如果有些插件安装失败，就点击 重新安装 多装几次。如果有些插件始终无法安装就跳过。

![1571112101164](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571112101164.png)

> **ps**：Jenkins安装插件慢的解决办法如下：
>
> 首先启动Jenkins服务，Jenkins会创建一个 data 目录，进入目录中中，找到  hudson.model.UpdateCenter.xml 配置文件，打开该配置文件，将里面插件源地址修改为国内的插件源地址，比如： http://mirrors.tuna.tsinghua.edu.cn/jenkins/updates/update-center.json，然后重启 Jenkins 服务，安装默认的插件，注意该步骤在第一次启动 Jenkins 服务创建了 data 目录后做，因为这个时候默认的插件还没有安装，我这里选择在输入初始密码的时候做该步骤。
>
> 主机 Jenkins 镜像选取，官方那个已经自己提出不在维护，所以要使用  jenkins/jenkins:lts  镜像，之前没有注意看文档，被坑了很久。

创建用户名和密码

![1571110136395](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571110136395.png)

![1571110172901](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571110172901.png)

Jenkins 中文配置需要下载的插件 Locale plugin、Localization: Chinese (Simplified)、Localization Support Plugin 如果联网无法下载，需要离线安装。找到设置，配置如下

![1571130779318](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571130779318.png)

### 配置Jenkins

#### 安装 JDK 和 Maven

将下载好的 apache-maven-3.6.2-bin.tar.gz 和 jdk-8u221-linux-x64.tar.gz 软件包，上传到 Jenkins 挂载的 /usr/local/docker/jenkins/data 目录下，并解压，解压成功后删除两个软件包。

#### 配置 JDK 和 Maven

配置 JDK，步骤如下图

![1571132174797](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571132174797.png)

![1571132216370](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571132216370.png)

![1571132542958](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571132542958.png)

![1571132936771](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571132936771.png)

#### 安装动态参数插件

在插件管理中安装 Extended Choice Parameter 插件，如果安装失败请离线安装。

安装完后重启 Jenkins 服务。

### 持续交付-创建第一个任务

Jenkins 的持续交付流程与 gitlab-runner 的持续集成差不多，但 gitlab-runner 已经是默认配置好勒 git，所以 Jenkins 需要额外配置一个 gitlab 的 SSH 登录。按照之前 gitlab-runner 的持续集成流程，Jenkins 的持续交付流程大致如下（其实原理还是挺简单的，但对于刚刚接触 Jenkins 的人来说，理解起来可能还是有一点难度的）：

- 拉取代码
- 打包构建
- 上传镜像
- 运行容器
- 维护清理

**配置 Jenkins 的 gitLab SSH 免密登录**

进入 Jenkins 容器内部

```bash
docker exec -it 容器id /bin/bash
```

生成 SSH KEY

```bash
ssh-keygen -t rsa -C "邮箱地址"
```

查看公钥

```bash
cat /var/jenkins_home/.ssh/id_rsa.pub
```

复制公钥到 gitLab， 设置 SSH 密钥

进入 gitLab 的设置页面

![1571192174648](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571192174648.png)

![1571192253812](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571192253812.png)

回到家目录

```bash
cd ~
```

随便克隆一个项目

```bash
# 注意这里的ssh前缀，新版的 gitlab 默认不会添加前缀，导致克隆项目总是叫输入密码，没有权限，坑的一批
git clone ssh://git@192.168.1.170:522/my-shop/my-shop-dependencies.git
```

![1571196223479](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571196223479.png)

进入到 /var/jenkins_home/.ssh/ 目录查看是否有一个 known_hosts 的文件，如果有，SSH 就配置成功。

删除刚才克隆的 my-shop-dependencies 项目。

**配置 Publish over SSH**

进入 Jenkins 系统配置中，找到如下配置项

![1571196504530](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571196504530.png)

![1571196874189](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571196874189.png)

![1571197277508](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571197277508.png)

![1571197334202](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571197334202.png)

最后点击保存。

访问 gitLab，给 my-shop-dependencies 项目新建标签（也可以是其它项目）

![1571197500152](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571197500152.png)

点击保存按钮。

**创建 Maven Project**

在 Jenkins 中创建一个基于 Maven 的任务

![1571197656750](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571197656750.png)

![1571199198370](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571199198370.png)

> **注意**：如果没有上面图中2这个选项，是因为没有对应的插件，需要安装  Maven Integration Plugin  插件，然后重启 Jenkins 服务后就有了。

![1571199494118](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571199494118.png)

![1571207953683](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571207953683.png)

点击保存，然后点击 立即构建。

![1571208058022](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571208058022.png)

接着就等待构建成功吧。

构建成功后，交互式进入 Jenkins 容器，进入到家目录中的 workspace 目录下，可以看到从 gitLab 上拉取下来的项目代码。

![1571208350336](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571208350336.png)

Jenkins 自动将我们的项目代码安装到了本地仓库，在当前家目录下的 .m2 目录中可以看到。

![1571208510014](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571208510014.png)

上面步骤完成后，再次访问 Jenkins，并对刚才创建的 Maven Object 进行配置

![1571208633933](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571208633933.png)

![1571208800335](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571208800335.png)

![1571209262470](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571209262470.png)

 Groovy Script 内容如下：

```
def ver_keys = ['bash','-c','cd /var/jenkins_home/workspace/my-shop-dependencies; git pull>/dev/null; git remote prune origin>/dev/null; git tag -l |sort -r |head -10']
ver_keys.execute().text.tokenize('\n')
```

![1571209367623](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571209367623.png)

![1571209581005](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571209581005.png)

上面图中内容如下：

```
echo $RELEASE_VERSION
cd /var/jenkins_home/workspace/my-shop-dependencies
git checkout $RELEASE_VERSION
git pull origin  $RELEASE_VERSION
mvn clean install
```

最后点击保存。

![1571209667683](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571209667683.png)

![1571209739039](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571209739039.png)

![1571209748657](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571209748657.png)

![1571209802143](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571209802143.png)

![1571209837381](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571209837381.png)

![1571213354951](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571213354951.png)

**实现自动部署功能**

![1571215606918](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571215606918.png)

找到  Post Steps 配置项，操作如下

![1571215720190](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571215720190.png)

![1571215949178](SpringBoot+Dubbo+Zookeeper%E5%AE%9E%E6%88%98%E7%AC%94%E8%AE%B0.assets/1571215949178.png)

保存配置，开始重新构建。

查看目标服务器上的服务是否部署成功。

# API网关

网关的思路是在访问某个 api 时，不是直接去访问 ip 地址，而是根据某个**前缀**获取到对应服务的 ip 地址，然后再根据 ip 地址访问 api。

具体实现如下：

1. 将服务消费者转变成既是服务提供者（用于网关调用该接口），也是服务消费者（用于调用其他服务提供者）。

2. 新建项目中，将网关变成一个服务消费者，在 Controller 中去调用步骤 1 中服务提供者提供的接口服务，当去调用步骤 1 中的服务提供者接口时，相当于网关项目发送了一个 RPC 请求，在 Dubbo 中，可以使用Dubbo提供的**上下文信息（RpcContext）**来获取最后一次调用的提供方 IP 地址 ，然后重定向到对应的提供方 ip 地址即可。

详细实现可以看代码中的 my-shop-api-gateway 项目。

> 注意：RpcContext.getContext().isConsumerSide() 或者其它方法的调用如果在 Dubbo2.7.x 后将会报空指针异常，因为 Dubbo2.7.x 在调用远程接口（RPC请求）后把 RpcContext 中的内容清空了，这是一个巨坑，我在这里研究了好久，最后解决办法是降低 Dubbo 版本到 2.6.x 就没有问题。
>
> github已有人反馈这个问题，地址为：https://github.com/apache/dubbo/issues/4390

# 分布式文件系统FastDFS

## 什么是FastDFS

DFS：分布式文件系统。

FastDFS：阿里开发的分布式文件系统。

FastDFS是一个开源的轻量级分布式文件系统，它对文件进行管理，功能包括：文件存储、文件同步、文件访问（文件上传、文件下载）等，解决了大容量存储和负载均衡问题。特别适合以文件为载体的在线服务，如相册网站、视频网站等。

FastDFS为互联网量身定制，充分考虑冗余备份、负载均衡、线性扩容等机制，并注重高可用、高性能等指标，使用FastDFS很容易搭建一套高性能的文件服务器集群，提供文件上传和下载等服务。

**FastDFS服务端有两大角色**

1. 跟踪器（tracker）：主要做调度工作，在访问上起负载均衡的作用。
2. 存储节点（storage）：存储文件。

**集群、高可用和负载均衡的区别**

- 集群：需要做到数据同步。
- 高可用：需要做到崩溃恢复。
- 负载均衡：一个应用部署多台，数据没有同步，没有做崩溃恢复。

**在搭建FastDFS时，为什么需要用到Nginx？**

因为FastDFS服务端的跟踪器可以部署多台，但是FastDFS的HTTP服务较为简单，无法提供负载均衡等高性能服务，所以需要使用Nginx来做负载均衡弥补上述的缺陷。

## Docker中安装FastDFS

FastDFS github 地址： https://github.com/happyfish100/fastdfs

FastDFS github 安装教程： https://github.com/happyfish100/fastdfs/wiki 

**以下安装方式是基于 Docker 安装 FastDFS 的教程**

**环境准备**

libfastcommon.tar.gz

fastdfs-5.11.tar.gz

nginx-1.13.6.tar.gz

fastdfs-nginx-module.v1.16.tar.gz

## 使用FastDFS的Java客户端

## Docker中安装Nginx

## 使用Nginx解决跨域问题

# Solr全文检索

## 什么是Solr

## Docker安装Solr

## Solr中使用分词器——IKAnalyzer

## Solr的基本操作

## SpringBoot整合Solr