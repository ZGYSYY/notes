
<center><h1>Java 中使用 Zookeeper 客户端 Curator 详解</h1></center>


# 简介

因为最近项目需要使用Zookeeper这个中间件，提前了解一下它的客户端Curator的使用。

Curator是Netflix公司开源的一套zookeeper客户端框架，解决了很多Zookeeper客户端非常底层的细节开发工作，包括连接重连、反复注册Watcher和NodeExistsException异常等等。Patrixck Hunt（Zookeeper）以一句“Guava is to Java that Curator to Zookeeper”给Curator予高度评价。

Curator无疑是Zookeeper客户端中的瑞士军刀，它译作”馆长”或者’’管理者’’，不知道是不是开发小组有意而为之，笔者猜测有可能这样命名的原因是说明Curator就是Zookeeper的馆长(脑洞有点大：Curator就是动物园的园长)。
Curator包含了几个包：

- curator-framework：对zookeeper的底层api的一些封装。
- curator-client：提供一些客户端的操作，例如重试策略等。
- curator-recipes：封装了一些高级特性，如：Cache事件监听、选举、分布式锁、分布式计数器、分布式Barrier等。

Maven依赖（注意zookeeper版本  这里对应的是3.4.6）

```xml
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-recipes</artifactId>
    <version>2.10.0</version>
</dependency>
```

# Curator的基本Api

## 创建会话

**方式一**

```java
@Test
public void test() {
    /*
    创建重试策略对象，参数含义如下：
    - baseSleepTimeMs: 基本睡眠时间。
    - maxRetries：最大重试次数。
    */
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    /*
    创建客户端对象，参数含义如下：
    - connectString：服务器列表，格式为 `host1:port,host2:port,...`。
    - sessionTimeoutMs：会话超时时间， 默认 60000 ms。
    - connectionTimeoutMs：连接超时时间，默认 60000 ms。
    - retryPolicy：重试策略
    */
    CuratorFramework client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", 5000, 5000, retryPolicy);
    // 连接 zookeeper 服务器
    client.start();
}
```

**方式二**

```java
@Test
public void test2() {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    CuratorFramework client = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181")
        .sessionTimeoutMs(5000)
        .connectionTimeoutMs(5000)
        .retryPolicy(retryPolicy)
        .build();
    client.start();
}
```

**创建包含隔离命名空间的会话**

为了实现不同的Zookeeper业务之间的隔离，需要为每个业务分配一个独立的命名空间（NameSpace），即指定一个Zookeeper的根路径（官方术语：为Zookeeper添加“Chroot”特性）。例如（下面的例子）当客户端指定了独立命名空间为“/base”，那么该客户端对Zookeeper上的数据节点的操作都是基于该目录进行的。通过设置Chroot可以将客户端应用与Zookeeper服务端的一课子树相对应，在多个应用共用一个Zookeeper集群的场景下，这对于实现不同应用之间的相互隔离十分有意义。

```java
RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
		CuratorFramework client =
		CuratorFrameworkFactory.builder()
				.connectString(connectionInfo)
				.sessionTimeoutMs(5000)
				.connectionTimeoutMs(5000)
				.retryPolicy(retryPolicy)
				.namespace("base")
				.build();
```

## 数据节点基本操作（增删改查）

**Zookeeper的节点创建模式：**

- PERSISTENT：持久化
- PERSISTENT_SEQUENTIAL：持久化并且带序列号
- EPHEMERAL：临时
- EPHEMERAL_SEQUENTIAL：临时并且带序列号

增删改查操作如下

```java
@Before
public void before() throws Exception {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    CuratorFramework client = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181")
        .sessionTimeoutMs(5000)
        .connectionTimeoutMs(5000)
        .retryPolicy(retryPolicy)
        .build();
    client.start();
    // 删除一个节点，并且递归删除其所有的子节点
    client.delete().deletingChildrenIfNeeded().forPath("/study");
    LOGGER.info("清除上一次测试的数据成功！");
}

/**
 * 基本操作
 * @throws Exception
 */
@Test
public void test3() throws Exception {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    CuratorFramework client = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181")
        .sessionTimeoutMs(5000)
        .connectionTimeoutMs(5000)
        .retryPolicy(retryPolicy)
        .namespace("study")
        .build();
    client.start();
    /**
     * 创建节点
     */
    // 创建一个节点，初始内容为空
    client.create().forPath("/name");
    // 创建一个节点，附带初始化内容
    client.create().forPath("/name2", "创建一个节点，附带初始化内容".getBytes(Charset.forName("utf-8")));
    // 创建一个节点，指定创建模式（临时节点），内容为空
    client.create().withMode(CreateMode.EPHEMERAL).forPath("/name3");
    // 创建一个节点，指定创建模式（临时节点），附带初始化内容
    client.create().withMode(CreateMode.EPHEMERAL).forPath("/name4", "创建一个节点，指定创建模式（临时节点），附带初始化内容".getBytes(Charset.forName("utf-8")));
    // 创建一个节点，指定创建模式（临时节点），附带初始化内容，并且自动递归创建父节点
    client.create().creatingParentContainersIfNeeded().withMode(CreateMode.EPHEMERAL).forPath("/parent/name5", "创建一个节点，指定创建模式（临时节点），附带初始化内容，并且自动递归创建父节点".getBytes(Charset.forName("utf-8")));

    /**
     * 更新数据节点数据
     */
    // 更新一个节点的数据内容
    client.setData().forPath("/name2", "更新一个节点的数据内容".getBytes(Charset.forName("utf-8")));
    // 更新一个节点的数据内容，强制指定版本进行更新
    client.setData().withVersion(0).forPath("/name", "更新一个节点的数据内容，强制指定版本进行更新".getBytes(Charset.forName("utf-8")));

    /**
     * 读取节点
     */
    // 读取一个节点的数据内容
    String s = new String(client.getData().forPath("/name2"), Charset.forName("utf-8"));
    LOGGER.info("读取一个节点的数据内容, s: [{}]", s);
    // 读取一个节点的数据内容，同时获取到该节点的stat
    Stat stat = new Stat();
    s = new String(client.getData().storingStatIn(stat).forPath("/name"), Charset.forName("utf-8"));
    LOGGER.info("读取一个节点的数据内容，同时获取到该节点的stat, s: [{}], stat: [{}]", s, stat);

    /**
     * 删除节点
     */
    // 删除一个节点
    client.delete().forPath("/name");
    // 删除一个节点，并且递归删除其所有的子节点
    client.delete().deletingChildrenIfNeeded().forPath("/parent");

    /**
     * 检查节点是否存在，不存在时对象为 null
     */
    stat = client.checkExists().forPath("/name5");
    LOGGER.info("检查节点是否存在, stat: [{}]", stat);
}
```

## 事务

CuratorFramework的实例包含inTransaction( )接口方法，调用此方法开启一个ZooKeeper事务. 可以复合create, setData, check, and/or delete 等操作然后调用commit()作为一个原子操作提交。一个例子如下：

```java
@Test
public void test4() throws Exception {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    CuratorFramework client = CuratorFrameworkFactory.builder()
        .connectString("127.0.0.1:2181")
        .sessionTimeoutMs(5000)
        .connectionTimeoutMs(5000)
        .retryPolicy(retryPolicy)
        .namespace("study")
        .build();
    client.start();

    // 事务操作，保证原子性
    client.inTransaction()
        .create().withMode(CreateMode.EPHEMERAL).forPath("/name", "aaaaa".getBytes())
        .and().setData().forPath("/name", "bbb".getBytes())
        .and().commit();
}
```

## 异步接口

上面提到的创建、删除、更新、读取等方法都是同步的，Curator提供异步接口，引入了BackgroundCallback接口用于处理异步接口调用之后服务端返回的结果信息。BackgroundCallback接口中一个重要的回调值为CuratorEvent，里面包含事件类型、响应吗和节点的详细信息。

**CuratorEventType**

| 事件类型 | 对应CuratorFramework实例的方法 |
| :------: | :----------------------------: |
|  CREATE  |           #create()            |
|  DELETE  |           #delete()            |
|  EXISTS  |         #checkExists()         |
| GET_DATA |           #getData()           |
| SET_DATA |           #setData()           |
| CHILDREN |         #getChildren()         |
|   SYNC   |      #sync(String,Object)      |
| GET_ACL  |           #getACL()            |
| SET_ACL  |           #setACL()            |
| WATCHED  |       #Watcher(Watcher)        |
| CLOSING  |            #close()            |

**响应码(#getResultCode())**

| 响应码 |                   意义                   |
| :----: | :--------------------------------------: |
|   0    |              OK，即调用成功              |
|   -4   | ConnectionLoss，即客户端与服务端断开连接 |
|  -110  |        NodeExists，即节点已经存在        |
|  -112  |        SessionExpired，即会话过期        |

一个异步创建节点的例子如下（然而并没有调用回调方法，原因不知道，也没有报错）：

```java
/**
 * 异步操作
 * @throws Exception
 */
@Test
public void test5() throws Exception {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    CuratorFramework client = CuratorFrameworkFactory.builder()
        .connectString("127.0.0.1:2181")
        .sessionTimeoutMs(5000)
        .connectionTimeoutMs(5000)
        .retryPolicy(retryPolicy)
        .namespace("study")
        .build();
    client.start();

    Executor executor = Executors.newFixedThreadPool(2);
    client.create().creatingParentContainersIfNeeded().withMode(CreateMode.EPHEMERAL).inBackground(new BackgroundCallback() {
        @Override
        public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
            LOGGER.info("开始调用回调方法，WatchedEvent: [{}], ResultCode: [{}]", event.getType(), event.getResultCode());
        }
    }, executor).forPath("/name");
}
```

**注意<![CDATA[：]]>**如果#inBackground()方法不指定executor，那么会默认使用Curator的EventThread去进行异步处理。

