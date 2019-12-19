<center><h1>Disruptor 详解</h1></center>

# 资料一

**背景**

高并发是指通过设计保证系统能够同时并行处理很多请求。虽然我在工作中经常听到高并发，QPS之类的术语。其实，我对高并发也是一知半解，知道Java里面可以用Lock，Synchronized，ArrayBlockingQueue之类的来进行高并发的处理。我个人觉得，高并发领域更多的依靠的是经验的累积。今天想跟大家分享的是一个高性能的并发框架Disruptor。

**Disruptor概述**

Disruptor是一个异步并发处理框架。是由LMAX公司开发的一款高效的无锁内存队列。它使用无锁的方式实现了一个环形队列，非常适合于实现生产者和消费者模式，比如事件和消息的发布。

Disruptor最大特点是高性能，其LMAX架构可以获得每秒6百万订单，用1微秒的延迟获得吞吐量为100K+。

**一个官网的简单的demo**

1.maven依赖
maven引入Disruptor的jar包，Disruptor版本为3.2.1

```xml
<dependency> 
  <groupId>com.lmax</groupId>  
  <artifactId>disruptor</artifactId>  
  <version>3.2.1</version> 
</dependency>
```

2.创建数据实体类LongEvent

```java
//代表数据的类
public class LongEvent {
    private long value;

    public void set(long value) {
        this.value = value;
    }
}
```

3.创建工厂类LongEventFactory

```java
//产生LongEvent的工厂类，它会在Disruptor系统初始化时，构造所有的缓冲区中的对象实例（预先分配空间）
public class LongEventFactory implements EventFactory<LongEvent> {
    public LongEvent newInstance() {
        return new LongEvent();
    }
}
```

4.创建消费者类LongEventHandler

```java
//消费者实现为WorkHandler接口，是Disruptor框架中的类
public class LongEventHandler implements EventHandler<LongEvent> {
    //onEvent()方法是框架的回调用法
    public void onEvent(LongEvent event, long sequence, boolean endOfBatch) {
        System.out.println("Event: " + event);
    }
}
```

5.创建生产者类LongEventProducer

```java
//消费者实现为WorkHandler接口，是Disruptor框架中的类
public class LongEventProducer {
    //环形缓冲区,装载生产好的数据；
    private final RingBuffer<LongEvent> ringBuffer;

    public LongEventProducer(RingBuffer<LongEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    //将数据推入到缓冲区的方法：将数据装载到ringBuffer
    public void onData(ByteBuffer bb) {
        long sequence = ringBuffer.next();

        // Grab the next sequence //获取下一个可用的序列号
        try {
            LongEvent event = ringBuffer.get(sequence);

            // Get the entry in the Disruptor //通过序列号获取空闲可用的LongEvent

            // for the sequence
            event.set(bb.getLong(0));

            // Fill with data //设置数值
        }
        finally {
            ringBuffer.publish(sequence);

            //数据发布，只有发布后的数据才会真正被消费者看见
        }
    }
}
```

6.创建测试类 LongEventMain

```java
public class LongEventMain {

    public static void main(String[] args) throws Exception {
        // 创建线程池
        Executor executor = Executors.newCachedThreadPool();
        // 事件工厂
        LongEventFactory factory = new LongEventFactory();
        // ringBuffer 的缓冲区的大小是1024
        int bufferSize = 1024;
        // 创建一个disruptor, ProducerType.MULTI:创建一个环形缓冲区支持多事件发布到一个环形缓冲区
        Disruptor<LongEvent> disruptor = new Disruptor<>(factory, bufferSize, executor, ProducerType.MULTI, new BlockingWaitStrategy());
        // 创建一个消费者
        disruptor.handleEventsWith(new LongEventHandler());
        // 启动并初始化disruptor
        disruptor.start();
        // 获取已经初始化好的ringBuffer
        RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();

        // 生产数据
        LongEventProducer producer = new LongEventProducer(ringBuffer);
        ByteBuffer bb = ByteBuffer.allocate(8);
        for (long l = 0; true; l++) {
            bb.putLong(0, l);
            producer.onData(bb);
            Thread.sleep(1000);
        }

    }

}
```

7.demo结果输出

![image-20191218180407076](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/image-20191218180407076.png)

**Disruptor的一些核心介绍**

**1.RingBuffer**

RingBuffer是其核心，生产者向RingBuffer中写入元素，消费者从RingBuffer中消费元素。

随着你不停地填充这个buffer（可能也会有相应的读取），这个序号会一直增长，直到绕过这个环。

槽的个数是2的N次方更有利于基于二进制的计算机进行计算。（注：2的N次方换成二进制就是1000，100，10，1这样的数字， sequence & （array length－1） = array index，比如一共有8槽，3&（8－1）=3，HashMap就是用这个方式来定位数组元素的，这种方式比取模的速度更快。）

会预先分配内存,可以做到完全的内存复用。在系统的运行过程中，不会有新的空间需要分配或者老的空间需要回收。因此，可以大大减少系统分配空间以及回收空间的额外开销。

关于RingBuffer可以直观的看一下下面的这幅图片（网上copy的），表示取到编号为4的数据。

![image-20191218180458585](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/image-20191218180458585.png)

**2.消费者等待策略**

BlockingWaitStrategy：这是默认的策略。使用锁和条件进行数据的监控和线程的唤醒。因为涉及到线程的切换，是最节省CPU，但在高并发下性能表现最糟糕的一种等待策略。

SleepingWaitStrategy:会自旋等待数据，如果不成功，才让出cpu，最终进行线程休眠，以确保不占用太多的CPU数据，因此可能产生比较高的平均延时。比较适合对延时要求不高的场合，好处是对生产者线程的影响最小。典型的应用场景是异步日志。

YieldingWaitStrategy:用于低延时的场合。消费者线程不断循环监控缓冲区变化，在循环内部，会使用Thread.yield()让出cpu给别的线程执行时间。

BusySpinWaitStrategy:开启的是一个死循环监控，消费者线程会尽最大努力监控缓冲区变化，因此，CPU负担比较大

**3.Disruptor的应用场景**

Disruptor号称能够在一个线程里每秒处理 6 百万订单,实际上我也没有测试过。Disruptor实际上内部是使用环形队列来实现的，所以一般来说，在消费者和生产者的场景中都可以考虑使用Disruptor。比如像日志处理之类的。实际上，我个人觉得Disruptor就像是Java里面的ArrayBlockingQueue的替代者，因为Disruptor可以提供更高的并发度和吞吐量。从下面这幅官网的图就可以直观的感受到Disruptor和ArrayBlockingQueue之间的效率的对比。(注意这是一个对数对数尺度，不是线性的。)

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640.webp)

# 资料二

**Padding Cache Line，体验高速缓存的威力**

我们先来看看 Disruptor 里面一段神奇的代码。这段代码里，Disruptor 在 RingBufferPad 这个类里面定义了 p1，p2 一直到 p7 这样 7 个 long 类型的变量。

```java
abstract class RingBufferPad
{
    protected long p1, p2, p3, p4, p5, p6, p7;
}
```

我在看到这段代码的第一反应是，变量名取得不规范，p1-p7 这样的变量名没有明确的意义啊。不过，当我深入了解了 Disruptor 的设计和源代码，才发现这些变量名取得恰如其分。因为这些变量就是没有实际意义，只是帮助我们进行**缓存行填充**（Padding Cache Line），使得我们能够尽可能地用上 CPU 高速缓存（CPU Cache）。那么缓存行填充这个黑科技到底是什么样的呢？我们接着往下看。

如果访问内置在 CPU 里的 L1 Cache 或者 L2 Cache，访问延时是内存的 1/15 乃至 1/100。而内存的访问速度，其实是远远慢于 CPU 的。想要追求极限性能，需要我们尽可能地多从 CPU Cache 里面拿数据，而不是从内存里面拿数据。

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721121761.webp)

CPU Cache 装载内存里面的数据，不是一个一个字段加载的，而是加载一整个缓存行。举个例子，如果我们定义了一个长度为 64 的 long 类型的数组。那么数据从内存加载到 CPU Cache 里面的时候，不是一个一个数组元素加载的，而是一次性加载固定长度的一个缓存行。

我们现在的 64 位 Intel CPU 的计算机，缓存行通常是 64 个字节（Bytes）。一个 long 类型的数据需要 8 个字节，所以我们一下子会加载 8 个 long 类型的数据。也就是说，一次加载数组里面连续的 8 个数值。这样的加载方式使得我们遍历数组元素的时候会很快。因为后面连续 7 次的数据访问都会命中缓存，不需要重新从内存里面去读取数据。

但是，在我们不使用数组，而是使用单独的变量的时候，这里就会出现问题了。在 Disruptor 的 RingBuffer（环形缓冲区）的代码里面，定义了一个 RingBufferFields 类，里面有 indexMask 和其他几个变量，用来存放 RingBuffer 的内部状态信息。

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721170367.webp)

CPU 在加载数据的时候，自然也会把这个数据从内存加载到高速缓存里面来。不过，这个时候，高速缓存里面除了这个数据，还会加载这个数据前后定义的其他变量。这个时候，问题就来了。Disruptor 是一个多线程的服务器框架，在这个数据前后定义的其他变量，可能会被多个不同的线程去更新数据、读取数据。这些写入以及读取的请求，会来自于不同的 CPU Core。于是，为了保证数据的同步更新，我们不得不把 CPU Cache 里面的数据，重新写回到内存里面去或者重新从内存里面加载数据。

而我们刚刚说过，这些 CPU Cache 的写回和加载，都不是以一个变量作为单位的。这些动作都是以整个 Cache Line 作为单位的。所以，当 INITIAL_CURSOR_VALUE 前后的那些变量被写回到内存的时候，这个字段自己也写回到了内存，这个常量的缓存也就失效了。当我们要再次读取这个值的时候，要再重新从内存读取。这也就意味着，读取速度大大变慢了。

```java
......

abstract class RingBufferPad
{
    protected long p1, p2, p3, p4, p5, p6, p7;
}


abstract class RingBufferFields<E> extends RingBufferPad
{
    ......
    private final long indexMask;
  private final Object[] entries;
  protected final int bufferSize;
  protected final Sequencer sequencer;
    ......
}

public final class RingBuffer<E> extends RingBufferFields<E> implements Cursored, EventSequencer<E>, EventSink<E>
{
    ......
    protected long p1, p2, p3, p4, p5, p6, p7;
    ......
}
```

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721201347.webp)

面临这样一个情况，Disruptor 里发明了一个神奇的代码技巧，这个技巧就是缓存行填充。Disruptor 在 RingBufferFields 里面定义的变量的前后，分别定义了 7 个 long 类型的变量。前面的 7 个来自继承的 RingBufferPad 类，后面的 7 个则是直接定义在 RingBuffer 类里面。这 14 个变量没有任何实际的用途。我们既不会去读他们，也不会去写他们。

而 RingBufferFields 里面定义的这些变量都是 final 的，第一次写入之后不会再进行修改。所以，一旦它被加载到 CPU Cache 之后，只要被频繁地读取访问，就不会再被换出 Cache 了。这也就意味着，对于这个值的读取速度，会是一直是 CPU Cache 的访问速度，而不是内存的访问速度。

**使用 RingBuffer，利用缓存和分支预测**

其实这个利用 CPU Cache 的性能的思路，贯穿了整个 Disruptor。Disruptor 整个框架，其实就是一个高速的生产者 - 消费者模型（Producer-Consumer）下的队列。生产者不停地往队列里面生产新的需要处理的任务，而消费者不停地从队列里面处理掉这些任务。

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721233410.webp)

如果你熟悉算法和数据结构，那你应该非常清楚，如果要实现一个队列，最合适的数据结构应该是链表。我们只要维护好链表的头和尾，就能很容易实现一个队列。生产者只要不断地往链表的尾部不断插入新的节点，而消费者只需要不断从头部取出最老的节点进行处理就好了。我们可以很容易实现生产者 - 消费者模型。实际上，Java 自己的基础库里面就有 LinkedBlockingQueue 这样的队列库，可以直接用在生产者 - 消费者模式上。

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721247878.webp)

不过，Disruptor 里面并没有用 LinkedBlockingQueue，而是使用了一个 RingBuffer 这样的数据结构，这个 RingBuffer 的底层实现则是一个固定长度的数组。比起链表形式的实现，数组的数据在内存里面会存在空间局部性。

就像上面我们看到的，数组的连续多个元素会一并加载到 CPU Cache 里面来，所以访问遍历的速度会更快。而链表里面各个节点的数据，多半不会出现在相邻的内存空间，自然也就享受不到整个 Cache Line 加载后数据连续从高速缓存里面被访问到的优势。

除此之外，数据的遍历访问还有一个很大的优势，就是 CPU 层面的分支预测会很准确。这可以使得我们更有效地利用了 CPU 里面的多级流水线，我们的程序就会跑得更快。

**总结延伸**

好了，不知道讲完这些，你有没有体会到 Disruptor 这个框架的神奇之处呢？

CPU 从内存加载数据到 CPU Cache 里面的时候，不是一个变量一个变量加载的，而是加载固定长度的 Cache Line。如果是加载数组里面的数据，那么 CPU 就会加载到数组里面连续的多个数据。所以，数组的遍历很容易享受到 CPU Cache 那风驰电掣的速度带来的红利。

对于类里面定义的单独的变量，就不容易享受到 CPU Cache 红利了。因为这些字段虽然在内存层面会分配到一起，但是实际应用的时候往往没有什么关联。于是，就会出现多个 CPU Core 访问的情况下，数据频繁在 CPU Cache 和内存里面来来回回的情况。而 Disruptor 很取巧地在需要频繁高速访问的变量，也就是 RingBufferFields 里面的 indexMask 这些字段前后，各定义了 7 个没有任何作用和读写请求的 long 类型的变量。

这样，无论在内存的什么位置上，这些变量所在的 Cache Line 都不会有任何写更新的请求。我们就可以始终在 Cache Line 里面读到它的值，而不需要从内存里面去读取数据，也就大大加速了 Disruptor 的性能。

这样的思路，其实渗透在 Disruptor 这个开源框架的方方面面。作为一个生产者 - 消费者模型，Disruptor 并没有选择使用链表来实现一个队列，而是使用了 RingBuffer。RingBuffer 底层的数据结构则是一个固定长度的数组。这个数组不仅让我们更容易用好 CPU Cache，对 CPU 执行过程中的分支预测也非常有利。更准确的分支预测，可以使得我们更好地利用好 CPU 的流水线，让代码跑得更快。