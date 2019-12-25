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

一个异步创建节点的例子如下：

```java
private static final Logger LOGGER = LoggerFactory.getLogger(Test04App.class);

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
    Stat stat = client.checkExists().forPath("/study");
    if (stat != null) {
        client.delete().deletingChildrenIfNeeded().forPath("/study");
    }
    LOGGER.info("清除上一次测试的数据成功！");
}

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

    // 不让程序结束，否则看不到回调方法的调用
    for (;;);
}
```

<b>注意：</b>如果#inBackground()方法不指定executor，那么会默认使用Curator的EventThread去进行异步处理。

# Curator 高级特性

<b>提醒：</b>强烈推荐使用ConnectionStateListener监控连接的状态，当连接状态为LOST，curator-recipes下的所有Api将会失效或者过期，尽管后面所有的例子都没有使用到ConnectionStateListener。

## 缓存

Zookeeper原生支持通过注册Watcher来进行事件监听，但是开发者需要反复注册(Watcher只能单次注册单次使用)。Cache是Curator中对事件监听的包装，可以看作是对事件监听的本地缓存视图，能够自动为开发者处理反复注册监听。Curator提供了三种Watcher(Cache)来监听结点的变化。

### Path Cache

Path Cache用来监控一个ZNode的子节点. 当一个子节点增加， 更新，删除时， Path Cache会改变它的状态， 会包含最新的子节点， 子节点的数据和状态，而状态的更变将通过PathChildrenCacheListener通知。

实际使用时会涉及到四个类：

- PathChildrenCache
- PathChildrenCacheEvent
- PathChildrenCacheListener
- ChildData

通过下面的构造函数创建Path Cache:

```java
public PathChildrenCache(CuratorFramework client, String path, boolean cacheData)
```

想使用cache，必须调用它的start方法，使用完后调用close方法。 可以设置StartMode来实现启动的模式。

StartMode有下面几种：

1. NORMAL：正常初始化。
2. BUILD_INITIAL_CACHE：在调用start()之前会调用rebuild()。
3. POST_INITIALIZED_EVENT： 当Cache初始化数据后发送一个PathChildrenCacheEvent.Type#INITIALIZED事件。

`public void addListener(PathChildrenCacheListener listener)`可以增加listener监听缓存的变化。

`getCurrentData()`方法返回一个List`<ChildData>`对象，可以遍历所有的子节点。

设置/更新、移除其实是使用client (CuratorFramework)来操作, 不通过PathChildrenCache操作，案例如下代码所示：

```java
package com.zgy.test;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author ZGY
 * @date 2019/12/25 10:31
 * @description Test05App, Curator 高级特性案例
 */
public class Test05App {

    private static final Logger LOGGER = LoggerFactory.getLogger(Test05App.class);

    @Test
    public void test() throws Exception {
        // 创建客户端 CuratorFramework 对象
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .connectString("127.0.0.1:2181")
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(5000)
                .build();
        // 连接 zookeeper服务器
        client.start();
        // 创建一个 PathChildrenCache 对象来监听对应路径下的的子节点
        PathChildrenCache pathChildrenCache = new PathChildrenCache (client, "/example/cache", true);
        // 开始监听子节点变化
        pathChildrenCache.start();
        // 当子节点数据变化时需要处理的逻辑
        pathChildrenCache.getListenable().addListener((clientFramework, event) -> {
            LOGGER.info("事件类型为：{}", event.getType());
            if (null != event.getData()) {
                LOGGER.info("节点路径为：{}，节点数据为：{}", event.getData().getPath(), new String(event.getData().getData()));
            }
        });

        // 创建节点
        client.create().creatingParentsIfNeeded().forPath("/example/cache/test01", "01".getBytes());
        TimeUnit.MILLISECONDS.sleep(10);

        // 创建节点
        client.create().creatingParentsIfNeeded().forPath("/example/cache/test02", "02".getBytes());
        TimeUnit.MILLISECONDS.sleep(10);

        // 修改数据
        client.setData().forPath("/example/cache/test01", "01_V2".getBytes());
        TimeUnit.MILLISECONDS.sleep(10);

        // 遍历缓存中的数据
        for (ChildData childData : pathChildrenCache.getCurrentData()) {
            LOGGER.info("获取childData对象数据, Path: [{}], Data: [{}]", childData.getPath(), new String(childData.getData()));
        }

        // 删除数据
        client.delete().forPath("/example/cache/test01");
        TimeUnit.MILLISECONDS.sleep(10);

        // 删除数据
        client.delete().forPath("/example/cache/test02");
        TimeUnit.MILLISECONDS.sleep(10);

        // 关闭监听
        pathChildrenCache.close();

        // 删除测试用的数据，如果存在子节点，一并删除
        client.delete().deletingChildrenIfNeeded().forPath("/example");

        // 断开与 zookeeper 的连接
        client.close();
        LOGGER.info("程序执行完毕");
    }
}
```

<b>注意：</b>如果new PathChildrenCache(client, PATH, true)中的参数cacheData值设置为false，则示例中的event.getData().getData()、data.getData()将返回null，cache将不会缓存节点数据。

<b>注意：</b>示例中的TimeUnit.MILLISECONDS.sleep(10)可以注释掉，但是注释后事件监听的触发次数会不全，这可能与PathCache的实现原理有关，不能太过频繁的触发事件！

### Node Cache

Node Cache与Path Cache类似，Node Cache只是监听某一个特定的节点。它涉及到下面的三个类：

- NodeCache - Node Cache实现类
- NodeCacheListener - 节点监听器
- ChildData - 节点数据

<b>注意：</b>使用cache，依然要调用它的start()方法，使用完后调用close()方法。

getCurrentData()将得到节点当前的状态，通过它的状态可以得到当前的值。

示例代码如下：

```java
package com.zgy.test;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author ZGY
 * @date 2019/12/25 11:19
 * @description Test06App, Curator 高级特性 Node Cache
 */
public class Test06App {

    private static final Logger LOGGER = LoggerFactory.getLogger(Test06App.class);

    @Test
    public void test() throws Exception {
        // 创建客户端 CuratorFramework 对象
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(5000)
                .connectString("127.0.0.1:2181")
                .sessionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();

        // 连接 zookeeper服务器
        client.start();

        // 创建一个 NodeCache 对象来监听指定节点
        NodeCache nodeCache = new NodeCache(client, "/example/cache");
        // 当节点数据变化时需要处理的逻辑
        nodeCache.getListenable().addListener(() -> {
            ChildData currentData = nodeCache.getCurrentData();
            if (null != currentData) {
                LOGGER.info("节点数据：Path[{}], Data: [{}]",currentData.getPath() , new String(currentData.getData()));
            } else {
                LOGGER.info("节点被删除！");
            }
        });
        // 开始监听子节点变化
        nodeCache.start();

        // 创建节点
        client.create().creatingParentsIfNeeded().forPath("/example/cache", "test01".getBytes());
        TimeUnit.MILLISECONDS.sleep(100);

        // 修改数据
        client.setData().forPath("/example/cache", "test01_V1".getBytes());
        TimeUnit.MILLISECONDS.sleep(100);

        // 获取节点数据
        String s = new String(client.getData().forPath("/example/cache"));
        LOGGER.info("数据s：[{}]", s);

        // 删除节点
        client.delete().forPath("/example/cache");
        TimeUnit.MILLISECONDS.sleep(100);

        // 删除测试用的数据，如果存在子节点，一并删除
        client.delete().deletingChildrenIfNeeded().forPath("/example");

        // 关闭监听
        nodeCache.close();

        // 断开与 zookeeper 的连接
        client.close();

        LOGGER.info("程序执行完毕！");

        // 为了查看打印日志，不加这段代码看不到节点监听处理逻辑
        for (;;);
    }
}
```

<b>注意：</b>示例中的TimeUnit.MILLISECONDS.sleep(100)可以注释，但是注释后事件监听的触发次数会不全，这可能与NodeCache的实现原理有关，不能太过频繁的触发事件！

<b>注意：</b>NodeCache只能监听一个节点的状态变化。

### Tree Cache

Tree Cache可以监控整个树上的所有节点，类似于PathCache和NodeCache的组合，主要涉及到下面四个类：

- TreeCache - Tree Cache实现类
- TreeCacheListener - 监听器类
- TreeCacheEvent - 触发的事件类
- ChildData - 节点数据

示例代码如下：

```java
package com.zgy.test;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author ZGY
 * @date 2019/12/25 11:45
 * @description Test07App, Curator 高级特性 Tree Cache
 */
public class Test07App {

    private static final Logger LOGGER = LoggerFactory.getLogger(Test07App.class);

    @Test
    public void test() throws Exception {
        // 创建客户端 CuratorFramework 对象
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(5000)
                .connectString("127.0.0.1:2181")
                .sessionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();

        // 连接 zookeeper服务器
        client.start();

        // 创建一个 NodeCache 对象来监听指定节点下的所有节点
        TreeCache treeCache = new TreeCache(client, "/example/cache");
        // 当指定节点下的某个节点数据变化时需要处理的逻辑
        treeCache.getListenable().addListener((curatorFramework, event) -> {
            LOGGER.info("事件类型：{}， 路径：{}，数据：{}",
                    event.getType(),
                    event.getData() == null? null:event.getData().getPath(),
                    event.getData() == null? null:new String(event.getData().getData()));
        });

        // 开始监听指定节点下的所有节点变化
        treeCache.start();

        // 创建节点
        client.create().creatingParentsIfNeeded().forPath("/example/cache", "test01".getBytes());
        TimeUnit.MILLISECONDS.sleep(100);

        // 修改数据
        client.setData().forPath("/example/cache", "test01_V2".getBytes());
        TimeUnit.MILLISECONDS.sleep(100);

        // 修改数据
        client.setData().forPath("/example/cache", "test01_V3".getBytes());
        TimeUnit.MILLISECONDS.sleep(100);

        // 删除节点
        client.delete().forPath("/example/cache");
        TimeUnit.MILLISECONDS.sleep(100);

        // 删除测试用的数据，如果存在子节点，一并删除
        client.delete().deletingChildrenIfNeeded().forPath("/example");

        // 关闭监听
        treeCache.close();

        // 断开与 zookeeper 的连接
        client.close();

        LOGGER.info("程序执行完毕！");

        // 为了查看打印日志，不加这段代码看不到节点监听处理逻辑
        for (;;);
    }
}
```

<b>注意：</b>在此示例中没有使用TimeUnit.MILLISECONDS.sleep(100)，但是事件触发次数也是正常的。

<b>注意：</b>TreeCache在初始化(调用start()方法)的时候会回调TreeCacheListener实例一个事TreeCacheEvent，而回调的TreeCacheEvent对象的Type为INITIALIZED，ChildData为null，此时event.getData().getPath()很有可能导致空指针异常，这里应该主动处理并避免这种情况。