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

**简单使用**

```java
RLock lock = redisson.getLock("test_lock");
try{
    boolean isLock=lock.tryLock();
    if(isLock){
        doBusiness();
    }
}catch(exception e){
}finally{
    lock.unlock();
}
```

**源码中使用到的Redis命令**

分布式锁主要需要以下redis命令，这里列举一下。在源码分析部分可以继续参照命令的操作含义。

1.EXISTS key :当 key 存在，返回1；若给定的 key 不存在，返回0。
2.GETSET key value:将给定 key 的值设为 value ，并返回 key 的旧值 (old value)，当 key 存在但不是字符串类型时，返回一个错误，当key不存在时，返回nil。
3.GET key:返回 key 所关联的字符串值，如果 key 不存在那么返回 nil。
4.DEL key [KEY …]:删除给定的一个或多个 key ,不存在的 key 会被忽略,返回实际删除的key的个数（integer）。
5.HSET key field value：给一个key 设置一个{field=value}的组合值，如果key没有就直接赋值并返回1，如果field已有，那么就更新value的值，并返回0.
6.HEXISTS key field:当key中存储着field的时候返回1，如果key或者field至少有一个不存在返回0。
7.HINCRBY key field increment:将存储在key中的哈希（Hash）对象中的指定字段field的值加上增量increment。如果键key不存在，一个保存了哈希对象的新建将被创建。如果字段field不存在，在进行当前操作前，其将被创建，且对应的值被置为0，返回值是增量之后的值
8.PEXPIRE key milliseconds：设置存活时间，单位是毫秒。expire操作单位是秒。
9.PUBLISH channel message:向channel post一个message内容的消息，返回接收消息的客户端数。

**源码中使用到的lua脚本语义**

Redisson源码中，执行redis命令的是lua脚本，其中主要用到如下几个概念。

- redis.call() 是执行redis命令.
- KEYS[1] 是指脚本中第1个参数
- ARGV[1] 是指脚本中第一个参数的值
- 返回值中nil与false同一个意思。

需要注意的是，在redis执行lua脚本时，相当于一个redis级别的锁，不能执行其他操作，类似于原子操作，也是redisson实现的一个关键点。
另外，如果lua脚本执行过程中出现了异常或者redis服务器直接宕掉了，执行redis的根据日志回复的命令，会将脚本中已经执行的命令在日志中删除。

**源码分析**

**RLOCK结构**

```java
public interface RLock extends Lock, RExpirable {
    void lockInterruptibly(long leaseTime, TimeUnit unit) throws InterruptedException;
    boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;
    void lock(long leaseTime, TimeUnit unit);
    void forceUnlock();
    boolean isLocked();
    boolean isHeldByCurrentThread();
    int getHoldCount();
    Future<Void> unlockAsync();
    Future<Boolean> tryLockAsync();
    Future<Void> lockAsync();
    Future<Void> lockAsync(long leaseTime, TimeUnit unit);
    Future<Boolean> tryLockAsync(long waitTime, TimeUnit unit);
    Future<Boolean> tryLockAsync(long waitTime, long leaseTime, TimeUnit unit);
}
```

该接口主要继承了Lock接口, 并扩展了部分方法, 比如:boolean tryLock(long waitTime, long leaseTime, TimeUnit unit)新加入的leaseTime主要是用来设置锁的过期时间, 如果超过leaseTime还没有解锁的话, redis就强制解锁. leaseTime的默认时间是30s

**RedissonLock获取锁 tryLock源码**

```java
Future<Long> tryLockInnerAsync(long leaseTime, TimeUnit unit, long threadId) {
       internalLockLeaseTime = unit.toMillis(leaseTime);
       return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_LONG,
                 "if (redis.call('exists', KEYS[1]) == 0) then " +
                     "redis.call('hset', KEYS[1], ARGV[2], 1); " +
                     "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                     "return nil; " +
                 "end; " +
                 "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
                     "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                     "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                     "return nil; " +
                 "end; " +
                 "return redis.call('pttl', KEYS[1]);",
                   Collections.<Object>singletonList(getName()), internalLockLeaseTime, getLockName(threadId));
}
```

其中
KEYS[1] 表示的是 getName() ，代表的是锁名 test_lock
ARGV[1] 表示的是 internalLockLeaseTime 默认值是30s
ARGV[2] 表示的是 getLockName(threadId) 代表的是 id:threadId 用锁对象id+线程id， 表示当前访问线程，用于区分不同服务器上的线程.
逐句分析：

```java
if (redis.call('exists', KEYS[1]) == 0) then 
         redis.call('hset', KEYS[1], ARGV[2], 1); 
         redis.call('pexpire', KEYS[1], ARGV[1]); 
         return nil;
         end;
```

- if (redis.call(‘exists’, KEYS[1]) == 0) 如果锁名称不存在
- then redis.call(‘hset’, KEYS[1], ARGV[2],1) 则向redis中添加一个key为test_lock的set，并且向set中添加一个field为线程id，值=1的键值对，表示此线程的重入次数为1
- redis.call(‘pexpire’, KEYS[1], ARGV[1]) 设置set的过期时间，防止当前服务器出问题后导致死锁，return nil; end;返回nil 结束

```java
if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then 
         redis.call('hincrby', KEYS[1], ARGV[2], 1); 
         redis.call('pexpire', KEYS[1], ARGV[1]);
         return nil; 
         end;
```

- if (redis.call(‘hexists’, KEYS[1], ARGV[2]) == 1) 如果锁是存在的，检测是否是当前线程持有锁，如果是当前线程持有锁
- then redis.call(‘hincrby’, KEYS[1], ARGV[2], 1)则将该线程重入的次数++
- redis.call(‘pexpire’, KEYS[1], ARGV[1]) 并且重新设置该锁的有效时间
- return nil; end;返回nil，结束

```java
return redis.call('pttl', KEYS[1]);
```

- 锁存在, 但不是当前线程加的锁，则返回锁的过期时间

**RedissonLock解锁 unlock源码**

```java
@Override
    public void unlock() {
        Boolean opStatus = commandExecutor.evalWrite(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN,
                        "if (redis.call('exists', KEYS[1]) == 0) then " +
                            "redis.call('publish', KEYS[2], ARGV[1]); " +
                            "return 1; " +
                        "end;" +
                        "if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) then " +
                            "return nil;" +
                        "end; " +
                        "local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1); " +
                        "if (counter > 0) then " +
                            "redis.call('pexpire', KEYS[1], ARGV[2]); " +
                            "return 0; " +
                        "else " +
                            "redis.call('del', KEYS[1]); " +
                            "redis.call('publish', KEYS[2], ARGV[1]); " +
                            "return 1; "+
                        "end; " +
                        "return nil;",
                        Arrays.<Object>asList(getName(), getChannelName()), LockPubSub.unlockMessage, internalLockLeaseTime, getLockName(Thread.currentThread().getId()));
        if (opStatus == null) {
            throw new IllegalMonitorStateException("attempt to unlock lock, not locked by current thread by node id: "
                    + id + " thread-id: " + Thread.currentThread().getId());
        }
        if (opStatus) {
            cancelExpirationRenewal();
        }
    }
```

其中
KEYS[1] 表是的是getName() 代表锁名test_lock
KEYS[2] 表示getChanelName() 表示的是发布订阅过程中使用的Chanel
ARGV[1] 表示的是LockPubSub.unLockMessage 是解锁消息，实际代表的是数字 0，代表解锁消息
ARGV[2] 表示的是internalLockLeaseTime 默认的有效时间 30s
ARGV[3] 表示的是getLockName(thread.currentThread().getId())，是当前锁id+线程id
语义分析:

```java
if (redis.call('exists', KEYS[1]) == 0) then
         redis.call('publish', KEYS[2], ARGV[1]);
         return 1;
         end;
```

- if (redis.call(‘exists’, KEYS[1]) == 0) 如果锁已经不存在(可能是因为过期导致不存在，也可能是因为已经解锁)
- then redis.call(‘publish’, KEYS[2], ARGV[1]) 则发布锁解除的消息
- return 1; end 返回1结束

```java
if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) then 
         return nil;
         end;
```

- if (redis.call(‘hexists’, KEYS[1], ARGV[3]) == 0) 如果锁存在，但是若果当前线程不是加锁的线
- then return nil;end 则直接返回nil 结束

```java
local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1);
if (counter > 0) then
         redis.call('pexpire', KEYS[1], ARGV[2]); 
         return 0;
else
         redis.call('del', KEYS[1]);
         redis.call('publish', KEYS[2], ARGV[1]);
         return 1;
end;
```

- local counter = redis.call(‘hincrby’, KEYS[1], ARGV[3], -1) 如果是锁是当前线程所添加，定义变量counter，表示当前线程的重入次数-1,即直接将重入次数-1
- if (counter > 0)如果重入次数大于0，表示该线程还有其他任务需要执行
- then redis.call(‘pexpire’, KEYS[1], ARGV[2]) 则重新设置该锁的有效时间
- return 0 返回0结束
- else redis.call(‘del’, KEYS[1]) 否则表示该线程执行结束，删除该锁
- redis.call(‘publish’, KEYS[2], ARGV[1]) 并且发布该锁解除的消息
- return 1; end;返回1结束

```java
return nil;
```

- 其他情况返回nil并结束

```java
if (opStatus == null) {
            throw new IllegalMonitorStateException("attempt to unlock lock, not locked by current thread by node id: "
                    + id + " thread-id: " + Thread.currentThread().getId());
        }
```

脚本执行结束之后，如果返回值不是0或1，即当前线程去解锁其他线程的加锁时，抛出异常。

**RedissonLock强制解锁源码**

```java
@Override
    public void forceUnlock() {
        get(forceUnlockAsync());
    }
    Future<Boolean> forceUnlockAsync() {
        cancelExpirationRenewal();
        return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN,
                "if (redis.call('del', KEYS[1]) == 1) then "
                + "redis.call('publish', KEYS[2], ARGV[1]); "
                + "return 1 "
                + "else "
                + "return 0 "
                + "end",
                Arrays.<Object>asList(getName(), getChannelName()), LockPubSub.unlockMessage);
    }
```

以上是强制解锁的源码,在源码中并没有找到forceUnlock()被调用的痕迹(也有可能是我没有找对),但是forceUnlockAsync()方法被调用的地方很多，大多都是在清理资源时删除锁。此部分比较简单粗暴，删除锁成功则并发布锁被删除的消息，返回1结束，否则返回0结束。

**总结**

这里只是简单的一个redisson分布式锁的测试用例，并分析了执行lua脚本这部分，如果要继续分析执行结束之后的操作，需要进行netty源码分析 ，redisson使用了netty完成异步和同步的处理。

# # 资料二（Redlock：Redis分布式锁最牛逼的实现）

**普通实现**

说道Redis分布式锁大部分人都会想到：`setnx+lua`，或者知道`set key value px milliseconds nx`。后一种方式的核心实现命令如下：

```java
- 获取锁（unique_value可以是UUID等）
SET resource_name unique_value NX PX 30000

- 释放锁（lua脚本中，一定要比较value，防止误解锁）
if redis.call("get",KEYS[1]) == ARGV[1] then
    return redis.call("del",KEYS[1])
else
    return 0
end
```

这种实现方式有3大要点（也是面试概率非常高的地方）：

1. set命令要用`set key value px milliseconds nx`；
2. value要具有唯一性；
3. 释放锁时要验证value值，不能误解锁；

事实上这类琐最大的缺点就是它加锁时只作用在一个Redis节点上，即使Redis通过sentinel保证高可用，如果这个master节点由于某些原因发生了主从切换，那么就会出现锁丢失的情况：

1. 在Redis的master节点上拿到了锁；
2. 但是这个加锁的key还没有同步到slave节点；
3. master故障，发生故障转移，slave节点升级为master节点；
4. 导致锁丢失。

正因为如此，Redis作者antirez基于分布式环境下提出了一种更高级的分布式锁的实现方式：**Redlock**。笔者认为，Redlock也是Redis所有分布式锁实现方式中唯一能让面试官高潮的方式。

**Redlock实现**

antirez提出的redlock算法大概是这样的：

在Redis的分布式环境中，我们假设有N个Redis master。这些节点**完全互相独立，不存在主从复制或者其他集群协调机制**。我们确保将在N个实例上使用与在Redis单实例下相同方法获取和释放锁。现在我们假设有5个Redis master节点，同时我们需要在5台服务器上面运行这些Redis实例，这样保证他们不会同时都宕掉。

为了取到锁，客户端应该执行以下操作:

- 获取当前Unix时间，以毫秒为单位。
- 依次尝试从5个实例，使用相同的key和**具有唯一性的value**（例如UUID）获取锁。当向Redis请求获取锁时，客户端应该设置一个网络连接和响应超时时间，这个超时时间应该小于锁的失效时间。例如你的锁自动失效时间为10秒，则超时时间应该在5-50毫秒之间。这样可以避免服务器端Redis已经挂掉的情况下，客户端还在死死地等待响应结果。如果服务器端没有在规定时间内响应，客户端应该尽快尝试去另外一个Redis实例请求获取锁。
- 客户端使用当前时间减去开始获取锁时间（步骤1记录的时间）就得到获取锁使用的时间。**当且仅当从大多数**（N/2+1，这里是3个节点）**的Redis节点都取到锁，并且使用的时间小于锁失效时间时，锁才算获取成功**。
- 如果取到了锁，key的真正有效时间等于有效时间减去获取锁所使用的时间（步骤3计算的结果）。
- 如果因为某些原因，获取锁失败（没有在至少N/2+1个Redis实例取到锁或者取锁时间已经超过了有效时间），客户端应该在**所有的Redis实例上进行解锁**（即便某些Redis实例根本就没有加锁成功，防止某些节点获取到锁但是客户端没有得到响应而导致接下来的一段时间不能被重新获取锁）。

**Redlock源码**

redisson已经有对redlock算法封装，接下来对其用法进行简单介绍，并对核心源码进行分析（假设5个redis实例）。

POM依赖

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.3.2</version>
</dependency>
```

**用法**

首先，我们来看一下redission封装的redlock算法实现的分布式锁用法，非常简单，跟重入锁（ReentrantLock）有点类似：

```java
Config config = new Config();
config.useSentinelServers().addSentinelAddress("127.0.0.1:6369","127.0.0.1:6379", "127.0.0.1:6389")
        .setMasterName("masterName")
        .setPassword("password").setDatabase(0);
RedissonClient redissonClient = Redisson.create(config);
// 还可以getFairLock(), getReadWriteLock()
RLock redLock = redissonClient.getLock("REDLOCK_KEY");
boolean isLock;
try {
    isLock = redLock.tryLock();
    // 500ms拿不到锁, 就认为获取锁失败。10000ms即10s是锁失效时间。
    isLock = redLock.tryLock(500, 10000, TimeUnit.MILLISECONDS);
    if (isLock) {
        //TODO if get lock success, do something;
    }
} catch (Exception e) {
} finally {
    // 无论如何, 最后都要解锁
    redLock.unlock();
}
```

**唯一ID**

实现分布式锁的一个非常重要的点就是set的value要具有唯一性，redisson的value是怎样保证value的唯一性呢？答案是**UUID+threadId**。入口在redissonClient.getLock("REDLOCK_KEY")，源码在Redisson.java和RedissonLock.java中：

```java
protected final UUID id = UUID.randomUUID();
String getLockName(long threadId) {
    return id + ":" + threadId;
}
```

**获取锁**

获取锁的代码为redLock.tryLock()或者redLock.tryLock(500, 10000, TimeUnit.MILLISECONDS)，两者的最终核心源码都是下面这段代码，只不过前者获取锁的默认租约时间（leaseTime）是LOCK_EXPIRATION_INTERVAL_SECONDS，即30s：

```java
<T> RFuture<T> tryLockInnerAsync(long leaseTime, TimeUnit unit, long threadId, RedisStrictCommand<T> command) {
    internalLockLeaseTime = unit.toMillis(leaseTime);
    // 获取锁时向5个redis实例发送的命令
    return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, command,
              // 首先分布式锁的KEY不能存在，如果确实不存在，那么执行hset命令（hset REDLOCK_KEY uuid+threadId 1），并通过pexpire设置失效时间（也是锁的租约时间）
              "if (redis.call('exists', KEYS[1]) == 0) then " +
                  "redis.call('hset', KEYS[1], ARGV[2], 1); " +
                  "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                  "return nil; " +
              "end; " +
              // 如果分布式锁的KEY已经存在，并且value也匹配，表示是当前线程持有的锁，那么重入次数加1，并且设置失效时间
              "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
                  "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                  "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                  "return nil; " +
              "end; " +
              // 获取分布式锁的KEY的失效时间毫秒数
              "return redis.call('pttl', KEYS[1]);",
              // 这三个参数分别对应KEYS[1]，ARGV[1]和ARGV[2]
                Collections.<Object>singletonList(getName()), internalLockLeaseTime, getLockName(threadId));
}
```

获取锁的命令中，

- **KEYS[1]**就是Collections.singletonList(getName())，表示分布式锁的key，即REDLOCK_KEY；
- **ARGV[1]**就是internalLockLeaseTime，即锁的租约时间，默认30s；
- **ARGV[2]**就是getLockName(threadId)，是获取锁时set的唯一值，即UUID+threadId。

**释放锁**

释放锁的代码为redLock.unlock()，核心源码如下：

```java
protected RFuture<Boolean> unlockInnerAsync(long threadId) {
    // 向5个redis实例都执行如下命令
    return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN,
            // 如果分布式锁KEY不存在，那么向channel发布一条消息
            "if (redis.call('exists', KEYS[1]) == 0) then " +
                "redis.call('publish', KEYS[2], ARGV[1]); " +
                "return 1; " +
            "end;" +
            // 如果分布式锁存在，但是value不匹配，表示锁已经被占用，那么直接返回
            "if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) then " +
                "return nil;" +
            "end; " +
            // 如果就是当前线程占有分布式锁，那么将重入次数减1
            "local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1); " +
            // 重入次数减1后的值如果大于0，表示分布式锁有重入过，那么只设置失效时间，还不能删除
            "if (counter > 0) then " +
                "redis.call('pexpire', KEYS[1], ARGV[2]); " +
                "return 0; " +
            "else " +
                // 重入次数减1后的值如果为0，表示分布式锁只获取过1次，那么删除这个KEY，并发布解锁消息
                "redis.call('del', KEYS[1]); " +
                "redis.call('publish', KEYS[2], ARGV[1]); " +
                "return 1; "+
            "end; " +
            "return nil;",
            // 这5个参数分别对应KEYS[1]，KEYS[2]，ARGV[1]，ARGV[2]和ARGV[3]
            Arrays.<Object>asList(getName(), getChannelName()), LockPubSub.unlockMessage, internalLockLeaseTime, getLockName(threadId));

}
```