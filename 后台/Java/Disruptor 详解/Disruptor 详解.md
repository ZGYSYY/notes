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

# 资料三——Disruptor无锁框架为啥这么快

**1.1 CPU缓存**

在现代计算机当中，CPU是大脑，最终都是由它来执行所有的运算。而内存(RAM)则是血液，存放着运行的数据；但是，由于CPU和内存之间的工作频率不同，CPU如果直接去访问内存的话，系统性能将会受到很大的影响，所以在CPU和内存之间加入了三级缓存，分别是L1、L2、L3。

当CPU执行运算时，它首先会去L1缓存中查找数据，找到则返回；如果L1中不存在，则去L2中查找，找到即返回；如果L2中不存在，则去L3中查找，查到即返回。如果三级缓存中都不存在，最终会去内存中查找。对于CPU来说，走得越远，就越消耗时间，拖累性能。

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721576044.webp)

在三级缓存中，越靠近CPU的缓存，速度越快，容量也越小，所以L1缓存是最快的，当然制作的成本也是最高的，其次是L2、L3。

CPU频率，就是CPU运算时的工作的频率（1秒内发生的同步脉冲数）的简称，单位是Hz。主频由过去MHZ发展到了当前的GHZ（1GHZ=10^3MHZ=10^6KHZ= 10^9HZ）。

内存频率和CPU频率一样，习惯上被用来表示内存的速度，内存频率是以MHz（兆赫）为单位来计量的。目前较为主流的内存频率1066MHz、1333MHz、1600MHz的DDR3内存，2133MHz、2400MHz、2666MHz、2800MHz、3000MHz、3200MHz的DDR4内存。

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721594437.webp)

可以看得出，如果CPU直接访问内存，是一件相当耗时的操作。

**1.2 缓存行**

当数据被加载到三级缓存中，它是以缓存行的形式存在的，不是一个单独的项，也不是单独的指针。

在CPU缓存中，数据是以缓存行(cache line)为单位进行存储的，每个缓存行的大小一般为32—256个字节，常用CPU中缓存行的大小是64字节；CPU每次从内存中读取数据的时候，会将相邻的数据也一并读取到缓存中，填充整个缓存行；

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721623615.webp)

可想而知，当我们遍历数组的时候，CPU遍历第一个元素时，与之相邻的元素也会被加载到了缓存中，对于后续的遍历来说，CPU在缓存中找到了对应的数据，不需要再去内存中查找，效率得到了巨大的提升；

但是，在多线程环境中，也会出现伪共享的情况，造成程序性能的降低，堪称无形的性能杀手；

**1.2.1 缓存命中**

通过具体的例子，来阐述缓存命中和未命中之间的效率：

测试代码：

```java
public class CacheHit {

    //二维数组：
    private static long[][] longs;

    //一维数组长度：
    private static int length = 1024*1024;

    public static void main(String [] args) throws InterruptedException {
        //创建二维数组,并赋值：
        longs = new long[length][];
        for(int x = 0 ;x < length;x++){
            longs[x] = new long[6];
            for(int y = 0 ;y<6;y++){
                longs[x][y] = 1L;
            }
        }
        cacheHit();
         cacheMiss();
    }
    //缓存命中：
    private static void cacheHit() {
        long sum = 0L;
        long start = System.nanoTime();
        for(int x=0; x < length; x++){
            for(int y=0;y<6;y++){
                sum += longs[x][y];
            }
        }
        System.out.println("命中耗时："+(System.nanoTime() - start));
    }
    //缓存未命中：
    private static void cacheMiss() {
        long sum = 0L;
        long start = System.nanoTime();
        for(int x=0;x < 6;x++){
            for(int y=0;y < length;y++){
                sum += longs[y][x];
            }
        }
        System.out.println("未命中耗时："+(System.nanoTime() - start));
    }
}
```

测试结果：

```java
未命中耗时：43684518
命中耗时：19244507
```

在Java中，一个long类型是8字节，而一个缓存行是64字节，因此一个缓存行可以存放8个long类型。但是，在内存中的布局中，对象不仅包含了实例数据(long类型变量)，还包含了对象头。对象头在32位系统上占用8字节，而64位系统上占用16字节。

所以，在上面的例子中，笔者向二维数组中填充了6个元素，占用了48字节。

在cacheHit()的例子中，当第一次遍历的时候，获取longs\[0\]\[0\]，而longs\[0\]\[0\]—longs\[0\]\[5\]也同时被加载到了缓存行中，接下来获取longs\[0\]\[1\]，已存在缓存行中，直接从缓存中获取数据，不用再去内存中查找，以此类推；

在cacheMiss()的例子中，当第一次遍历的时候，也是获取longs\[0\]\[0\]的数据，longs\[0\]\[0\]—longs\[0\]\[5\]也被加载到了缓存行中，接下来获取long\[1\]\[0\]，不存在缓存行中，去内存中查找，以此类推；

以上的例子可以充分说明缓存在命中和未命中的情况下，性能之间的差距。

**1.2.2 伪共享**

由于CPU加载机制，某个数据被加载的同时，其相邻的数据也会被加载到CPU当中。在得到CPU免费加载的同时，也产生了不好的情况；俗话说得好，凡事都有利有弊。

在我们的java程序中，当多个线程修改两个独立变量的时候，如果这两个变量存在于一个缓存行中，那么就有很大的概率产生伪共享。

这是为什么呢？

现如今，CPU都是多核处理器，一般为2核或者4核，当我们程序运行时，启动了多个线程。例如：核心1启动了1个线程，核心2启动了1个线程，这2个线程分别要修改不同的变量，其中核心1的线程要修改x变量，而核心2的线程要修改y变量，但是x、y变量在内存中是相邻的数据，他们被加载到了同一个缓存行当中，核心1的缓存行有x、y，核心2的缓存行也有x、y。

那么，只要有一个核心中的线程修改了变量，另一个核心的缓存行就会失效，导致数据需要被重新到内存中读取，无意中影响了系统的性能，这就是伪共享。

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721831140.webp)

cpu的伪共享问题本质是：几个在内存中相邻的数据，被CPU的不同核心加载在同一个缓存行当中，数据被修改后，由于数据存在同一个缓存行当中，进而导致缓存行失效，引起缓存命中降低。

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721844773.webp)

代码例子：

```java
public class FalseShare implements Runnable{

    //线程数、数组大小：
    public static int NUM_THREADS = 4; // change

    //数组迭代的次数：
    public final static long ITERATIONS = 500L * 1000L * 1000L;

    //线程需要处理的数组元素角标：
    private final int handleArrayIndex;

    //操作数组：
    private static VolatileLong[] longs = new VolatileLong[NUM_THREADS];

    //对数组的元素进行赋值：
    static{
        for (int i = 0; i < longs.length; i++) {
            longs[i] = new VolatileLong();
        }
    }

    public FalseShare(final int handleArrayIndex) {
        this.handleArrayIndex = handleArrayIndex;
    }

    //启动线程，每一个线程操作一个数组的元素，一一对应：
    public static void main(final String[] args) throws Exception {
        //程序睡眠必须加上：
        Thread.sleep(10000);

        final long start = System.nanoTime();

        Thread[] threads = new Thread[NUM_THREADS];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new FalseShare(i));
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        System.out.println(System.nanoTime() - start);
    }

    //对数组的元素进行操作：
    public void run() {
        long i = ITERATIONS;
        while (0 != --i) {
            longs[handleArrayIndex].value = i;
        }
    }

    //数组元素：
    public final static class VolatileLong {
        public volatile long value = 0L;
        public long p1, p2, p3, p4, p5; //代码1
        public int p6;//代码1
    }
}
```

测试结果：（纳秒）

```java
未注释代码1：19830512      18472356     19993249    19841462
注释代码1：  21141471      25611265     19939633    29976847
```

通过测试结果，可以看出，在注释掉代码后，性能明显下降。让我们来阐述下原因：

通过代码，我们可以看出来，程序模拟的情况就是每一个线程操作数组中的一个元素，例如：线程1操作longs[0]，线程2操作longs[1]，线程3操作longs[2]…以此类推；之前说过，CPU缓存中是以缓存行为单位来进行存储的，一个缓存行大小为64字节。在程序中VolatileLong对象，正好满足64字节，为什么这么说？

在Java程序中，对象在内存中的分布：对象头（Header），实例数据（Instance Data），对齐填充（Padding）；

其中，对象头在32位系统上占用8字节，64位系统上占用16字节；实例数据也就是我们平常是所用到的基本类型和引用类型；对齐填充是对象在内存区域内的补充，jvm要求对象在内存区域的大小必须是8的整数倍，所以当对象头+实例数据的和不是8的整数倍时，就需要用到对齐填充，少多少就填充多少无效数据；

综上所述，VolatileLong=对象头(12字节)+value(8字节)+p1-p5(40字节)+p6(4字节) = 64字节，正好填充满整个缓存行；

当我们没有注释掉代码的时候，数组的各个元素将分布在不同的缓存行当中；而当注释掉代码的时候，数组的元素有很大的几率分布在同一个缓存行当中；当不同线程操作元素的时候，就会产生冲突，产生伪共享，影响系统性能；

经过上面的叙述，你大概对伪共享有了一定的了解，但是你会不会有这样的疑问？为什么其中1个核心缓存行的数据被修改了，其余核心中的缓存行就失效了？是什么机制产生了这样的情况？

以下，我们就来简单的介绍CPU的一致性协议MESI，就是这个协议保证了Cache的一致性；

**1.2.3 MESI协议**

多核理器中，每个核心都有自己的cache，内存中的数据可以同时处于不同的cache中，若各个核心独立修改自己的cache，就会出现不一致问题。为了解决一致性问题，MESI协议被引入。

MESI（Modified Exclusive Shared Or Invalid）是一种广泛使用的支持写回策略的缓存一致性协议，该协议最早被应用在Intel奔腾系列的CPU中。

其实，MESI协议就是规定了缓存行的4种状态，以及这4种状态之间的流转，以来保证不同核心中缓存的一致；每种状态在缓存行中用2个bit位来进行描述，分别是修改态（M）、独享态（E）、共享态（S）、无效态（I）；

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721907495.webp)

- E(Exclusive)：x变量只存在于core1中；

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721931430.webp)

- S(Shared):x变量存在于core1 core2 core3中

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721944728.webp)

- M(Modified)：core1修改了x变量，core2 core3的缓存行被置为无效状态

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721956301.webp)

在CPU中，每个核心不但控制着自己缓存行的读写操作，而且还监听这其他核心中缓存行的读写操作；每个缓存行的状态受到本核心和其他核心的双重影响；

下面，我们就阐述下这4中状态的流转：

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576721967959.webp)

```
(1)I--本地读请求：CPU读取变量x，如果其他核中的缓存有变量x，且缓存行的状态为M，则将该核心的变量x更新到内存，本核心的再从内存中读取取数据，加载到缓存行中，两个核心的缓存行状态都变成S；如果其他核心的缓存行状态为S或者E，本核心从内存中杜取数据，之后所有核心中的包含变量x的缓存行状态都变成S。

(2)I--本地读请求：CPU读取变量x，如果其他核中的缓存没有变量x，则本核心从内存中读取变量x，存入本核心的缓存行当中，该缓存行状态变成E；

(3)I--本地写请求：CPU读取写入变量x，如果其他核中没有此变量，则从内存中读取，在本核心中修改，此缓存行状态变为M；如果其他缓存行中有变量x，并且状态为M,则需要先将其他核心中的变量x写回内存，本核心再从内存中读取；如果其他缓存行中有变量x，并且状态为E/S，则将其他核心中的缓存行状态置为I，本核心在从内存中读取变量x，之后将本核心的缓存行置为M；

注意，一个缓存除在Invalid状态外都可以满足CPU的读请求，一个invalid的缓存行必须从主存中读取（变成S或者 E状态）来满足该CPU的读请求。

(4)S--远程写请求：多个核心共享变量X，其他核心将变量x修改，本核心中的缓存行不能使用，状态变为I；

(5)S--本地读请求：多个核心共享变量X，本核心读取本缓存中的变量x，状态不变；

(6)S--远程读请求：多个核心共享变量X，其他核心要读取变量X，从主内存中读取变量x，状态置为S，本核心状态S不变；

(7)S--本地写请求：多个核心共享变量X，本核心修改本缓存行中的变量x，必须先将其他核心中所拥有变量x的缓存行状态变成I，本核心缓存行状态置为M；该操作通常使用RequestFor Ownership (RFO)广播的方式来完成；

(8)E--远程读请求：只有本核心拥有变量x，其他核心也要读取变量x,从内存中读取变量x，并将所有拥有变量x的缓存行置为S状态；

(9)E--本地读请求：只有本核心拥有变量x，本核心需要读取变量x，读取本地缓存行中的变量x即可，状态不变依旧为E；

(10)E--远程写请求：只有本核心拥有变量x，其他核心需要修改变量x，其他核心从内存中读取变量x，进行修改，状态变成M，而本核心中缓存行变为状态I；

(11)E--本地写请求：只有本核心拥有变量x，本核心修改本缓存行中的变量x，状态置为M；

(12)M--本地写请求：只有本核心中拥有变量x，本核心进行修改x操作，缓存行状态不变；

(13)M--本地读请求：只有本核心中拥有变量x，本核心进行读取x操作，缓存行状态不变；

(14)M--远程读请求：只有本核心中拥有变量x，其他核心需要读取变量x,先将本核心中的变量x写回到内存中，在将本缓存行状态置为S，其他核心拥有变量x的缓存行状态也变为S；

(15)M--远程写请求：只有本核心中拥有变量x，其他和核心需要修改变量x，先将本核心中的变量x写回内存，再将本核心中缓存行置为I。其他核心的在从缓存行中读取变量x，修改后置为M；
```

以上就是MESI协议的状态流转；如果对状态流转还有疑问的话，还可以结合以下图例进行学习：

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576722004805.webp)

**1.3 CAS**

那么，CAS是什么呢？

在Java中，多线程之间如何保证数据的一致性？想必大部分都会异口同声地说出锁—-synchronized锁。在JDK1.5之前，的确是使用synchronized锁来保证数据的一致性。但是，synchronized锁是一种比较重的锁，俗称悲观锁。在较多线程的竞争下，加锁、释放锁会对系统性能产生很大的影响，而且一个线程持有锁，会导致其他线程的挂起，直至锁的释放。

那么，有没有比较轻的锁呢，答案是有的！与之相对应的是乐观锁！乐观锁虽然名称中带有锁，但实际在代码中是不加锁的，乐观锁大多实现体现在数据库sql层面，通常是的做法是：为数据增加一个版本标识，在表中增加一个 “version” 字段来实现。读取出数据时，将此版本号一同读出，之后更新时，对此版本号加一。此时，将提交数据的版本数据与数据库表对应记录的当前版本信息进行比对，如果提交的数据版本号大于数据库表当前版本号，则予以更新，否则认为是过期数据。

```sql
update XXX_TABLE SET MONEY = 100 AND VERSION = 11 WHERE ID = 1 AND VERSION = 10;
```

这就是乐观锁！

上面说到了数据库层面的乐观锁，那么代码层面有没有类似的实现？答案是，有的！那就是我们本小节的主角—CAS；

CAS是一个CPU级别的指令，翻译为Compare And Swap比较并交换；

CAS是对内存中共享数据操作的一种指令，该指令就是用乐观锁实现的方式，对共享数据做原子的读写操作。原子本意是“不能被进一步分割的最小粒子”，而原子操作意为”不可被中断的一个或一系列操作”。原子变量能够保证原子性的操作，意思是某个任务在执行过程中，要么全部成功，要么全部失败回滚，恢复到执行之前的初态，不存在初态和成功之间的中间状态。

CAS有3个操作数，内存中的值V，预期内存中的值A，要修改成的值B。当内存值V和预期值相同时，就将内存值V修改为B，否则什么都不做。

例如：

```java
public class CasTest implements Runnable{

    private int memoryValue = 1;

    private int expectValue;

    private int updateValue;

    public CasTest(int expectValue,int updateValue){
        this.expectValue = expectValue;
        this.updateValue = updateValue;
    }

    public void run() {
        if(memoryValue==expectValue){
            this.memoryValue = updateValue;
            System.out.println("修改成功");
        }else {
            System.out.println("修改失败");
        }
    }

    public static void main(String[] agrs) throws InterruptedException {
        CasTest casTest1 = new CasTest(1,2);
        Thread t1 = new Thread(casTest1);
        t1.start();

        Thread t2= new Thread(casTest1);
        t2.start();

        t1.join();
        t2.join();
    }
}
```

在Java中，主要使用了Unsafe类来实现CAS操作，利用JNI来完成CPU指令的调用。JNI：java native interface为java本地调用，也就是说允许java调用其他计算机语言（例如：C、C++等）；

在java.util.concurrent.atomic包下(AtomicInteger为例)：

```java
public class AtomicInteger extends Number implements java.io.Serializable {

    private static final long serialVersionUID = 6214790243416807050L;

    private static final Unsafe unsafe = Unsafe.getUnsafe();

    public final boolean compareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }
}
```

实际最终调用了sun.misc.Unsafe类：

```java
public final native boolean compareAndSwapInt(Object var1, long var2, int var4, int var5);
```

可以看到Unsafe的compareAndSwapInt方法，使用了native修饰符，是一个本地方法调用，最终由C++代码来操作CPU。至于具体实现，有兴趣的朋友可以去参考openJDK中Unsafe类；

与synchronized锁相比较而言，CAS最大的优势就是非阻塞，在代码层面，多线程情况下不阻塞其他线程的执行，从而达到既保证数据的安全，又提高了系统的性能。

**1.4 Disruptor中的运用**

上面，说了分别说了CAS、缓存行、伪共享。接下来，就来看看再Disruptor中是如何使用的！

在多生产者的环境下，更新下一个可用的序列号地方，我们使用CAS（Compare And Swap）操作。

![img](Disruptor%20%E8%AF%A6%E8%A7%A3.assets/640-1576722127388.webp)

Disruptor中多生产者情况下，获取下一个可用序列号的实现:

```java
public final class MultiProducerSequencer extends AbstractSequencer{
    @Override
    public long next(int n){
        if (n < 1){
            throw new IllegalArgumentException("n must be > 0");
        }
        long current;
        long next;
        do{
            current = cursor.get();
            next = current + n;
            long wrapPoint = next - bufferSize;
            long cachedGatingSequence = gatingSequenceCache.get();
            if (wrapPoint > cachedGatingSequence || cachedGatingSequence > current){
                long gatingSequence = Util.getMinimumSequence(gatingSequences, current);

                if (wrapPoint > gatingSequence){
                    waitStrategy.signalAllWhenBlocking();
                    LockSupport.parkNanos(1); // TODO, should we spin based on the wait strategy?
                    continue;
                }
                gatingSequenceCache.set(gatingSequence);

            //对current,next进行compareAndSet，cursor就是序列号对象：
            } else if (cursor.compareAndSet(current, next)){
                break;
            }
        }while (true);
        return next;
    }
}
```

Disruptor通过缓存行填充的方式来解决伪共享：

```java
class LhsPadding{
    protected long p1, p2, p3, p4, p5, p6, p7;
}

class Value extends LhsPadding{
    protected volatile long value;
}

class RhsPadding extends Value{
    protected long p9, p10, p11, p12, p13, p14, p15;
}

public class Sequence extends RhsPadding{}
```

Sequence是Disruptor中序列号对象，value是对象具体的序列值，通过上面的方式，value不会与其他需要操作的变量存在同一个缓存行中。