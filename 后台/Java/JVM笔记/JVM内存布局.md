<h1><b>JVM 内存布局</b></h1>

> 本文摘抄自https://mp.weixin.qq.com/s/hOGKhhwMwL_3rIJ4egQlcg。
>
> 并根据自己的对知识点的理解，做了相应的修改和完善，仅用于自己以后方便查阅。

# 概念

内存是非常重要的系统资源，是硬盘和 CPU 的中间仓库及桥梁，承载着操作系统和应用程序的实时运行。JVM 内存布局规定了 Java 在运行过程中内存申请、分配、管理的策略，保证了 JVM 的高效稳定运行。

![image-20200929165924703](https://raw.githubusercontent.com/ZGYSYY/notes-resources/master/后台/Java/JVM笔记/JVM%20内存布局/image-20200929165924703.png)

上图描述了当前比较经典的 JVM 内存布局。（堆区画小了 2333，按理来说应该是最大的区域）

如果按照线程是否共享来分类的话，如下图所示：

![image-20200929170426315](https://raw.githubusercontent.com/ZGYSYY/notes-resources/master/后台/Java/JVM笔记/JVM%20内存布局/image-20200929170426315.png)

PS：线程是否共享这点，实际上理解了每块区域的实际用处之后，就很自然而然的就记住了。不需要死记硬背。

下面让我们来了解下各个区域。

# Heap（堆）

## 1、堆区的介绍

我们先来说堆。堆是 OOM 故障最主要的发生区域。它是内存区域中最大的一块区域，被所有线程共享，存储着几乎所有的实例对象、数组。所有的对象实例以及数组都要在堆上分配，但是随着 JIT 编译器的发展与逃逸分析技术逐渐成熟，栈上分配、标量替换优化技术将会导致一些微妙的变化发生，所有的对象都分配在堆上也渐渐变得不是那么“绝对”了。

> 延伸知识点：JIT 编译优化中的一部分内容 - 逃逸分析。
> 推荐阅读：深入理解 Java 中的逃逸分析
> https://www.hollischuang.com/archives/2583
>
> PS：我也没有看过！

Java 堆是垃圾收集器管理的主要区域，因此很多时候也被称做“GC 堆”。从内存回收的角度来看，由于现在收集器基本都采用分代收集算法，所以 Java 堆中还可以细分为：新生代和老年代。再细致一点的有 Eden 空间、From Survivor 空间、To Survivor 空间等。从内存分配的角度来看，线程共享的 Java 堆中可能划分出多个线程私有的分配缓冲区（Thread Local Allocation Buffer,TLAB）。不过无论如何划分，都与存放内容无关，无论哪个区域，存储的都仍然是对象实例，进一步划分的目的是为了更好地回收内存，或者更快地分配内存。

## 2、堆区的调整

根据 Java 虚拟机规范的规定，Java 堆可以处于物理上不连续的内存空间中，只要逻辑上是连续的即可，就像我们的磁盘空间一样。在实现时，既可以实现成固定大小的，也可以在运行时动态地调整。

<b>如何调整呢？</b>

通过设置如下参数，可以设定堆区的初始值和最大值，比如 <span style="color:red;">-Xms256M -Xmx 1024M</span>，其中 <span style="color:red;">-X</span> 这个字母代表它是 JVM 运行时参数，<span style="color:red;">ms</span> 是 <span style="color:red;">memory start</span> 的简称，中文意思就是内存初始值，<span style="color:red;">mx</span> 是 <span style="color:red;">memory max</span> 的简称，意思就是最大内存。

值得注意的是，在通常情况下，服务器在运行过程中，堆空间不断地扩容与回缩，会形成不必要的系统压力所以在线上生产环境中 JVM 的 <span style="color:red;">Xms</span> 和 <span style="color:red;">Xmx</span> 会设置成同样大小，避免在 GC 后调整堆大小时带来的额外压力。

## 3、堆的默认空间分配

另外，再强调一下堆空间内存分配的大体情况。

![image-20200929171443819](https://raw.githubusercontent.com/ZGYSYY/notes-resources/master/后台/Java/JVM笔记/JVM%20内存布局/image-20200929171443819.png)

这里可能就会有人来问了，你从哪里知道的呢？如果我想配置这个比例，要怎么修改呢？

我先来告诉你怎么看虚拟机的默认配置。命令行上执行如下命令，就可以查看当前 JDK 版本所有默认的 JVM 参数。

```bash
java -XX:+PrintFlagsFinal -version
```

对应的输出应该有几百行，我们这里去看和堆内存分配相关的两个参数

```text
>java -XX:+PrintFlagsFinal -version
[Global flags]
    ...
    uintx InitialSurvivorRatio                      = 8
    uintx NewRatio                                  = 2
    ...
java version "1.8.0_131"
Java(TM) SE Runtime Environment (build 1.8.0_131-b11)
Java HotSpot(TM) 64-Bit Server VM (build 25.131-b11, mixed mode)
```

<b>参数解释</b>

![image-20200929171645700](https://raw.githubusercontent.com/ZGYSYY/notes-resources/master/后台/Java/JVM笔记/JVM 内存布局/image-20200929171645700.png)

因为新生代是由 <span style="color:red;">Eden + S0 + S1</span> 组成的，所以按照上述默认比例，如果 <span style="color:red;">eden</span> 区内存大小是 40M，那么两个 <span style="color:red;">survivor</span> 区就是 5M，整个 <span style="color:red;">young</span> 区就是 50M，然后可以算出 <span style="color:red;">Old</span> 区内存大小是 100M，堆区总大小就是 150M。

## 4、堆溢出演示

```java
/**
 * VM Args：-Xms10m -Xmx10m -XX:+HeapDumpOnOutOfMemoryError
 * @author Richard_Yi
 */
public class HeapOOMTest {

    public static final int _1MB = 1024 * 1024;

    public static void main(String[] args) {
        List<byte[]> byteList = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            byte[] bytes = new byte[2 * _1MB];
            byteList.add(bytes);
        }
    }
}
```

<b>输出</b>

```text
java.lang.OutOfMemoryError: Java heap space
Dumping heap to java_pid32372.hprof ...
Heap dump file created [7774077 bytes in 0.009 secs]
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
    at jvm.HeapOOMTest.main(HeapOOMTest.java:18)
```

PS：-XX:+HeapDumpOnOutOfMemoryError 可以让 JVM 在遇到 OOM 异常时，输出堆内信息。

## 5、创建一个新对象内存分配流程

看完上面对堆的介绍，我们趁热打铁再学习一下 JVM 创建一个新对象的内存分配流程。

![image-20200929172341989](https://raw.githubusercontent.com/ZGYSYY/notes-resources/master/后台/Java/JVM笔记/JVM%20内存布局/image-20200929172341989.png)

绝大部分对象在 <span style="color:red;">Eden</span> 区生成，当 Eden 区装填满的时候，会触发 <span style="color:red;">Young Garbage Collection</span>，即 <span style="color:red;">YGC</span>。垃圾回收的时候，在 <span style="color:red;">Eden</span> 区实现清除策略，没有被引用的对象则直接回收。依然存活的对象会被移送到 <span style="color:red;">Survivor</span> 区。<span style="color:red;">Survivor</span> 区分为 so 和 s1 两块内存空间。每次 <span style="color:red;">YGC</span> 的时候，它们将存活的对象复制到未使用的那块空间，然后将当前正在使用的空间完全清除，交换两块空间的使用状态。如果 YGC 要移送的对象大于 <span style="color:red;">Survivor</span> 区容量的上限，则直接移交给老年代。一个对象也不可能永远呆在新生代，就像人到了 18 岁就会成年一样，在 JVM 中 <span style="color:red;">－XX:MaxTenuringThreshold</span> 参数就是来配置一个对象从新生代晋升到老年代的阈值。默认值是 15，可以在 <span style="color:red;">Survivor</span> 区交换 14 次之后，晋升至老年代。

# Java 虚拟机栈

对于每一个线程，JVM 都会在线程被创建的时候，创建一个单独的栈。也就是说虚拟机栈的生命周期和线程是一致，并且是线程私有的。除了 Native 方法以外，Java 方法都是通过 Java 虚拟机栈来实现调用和执行过程的（需要程序技术器、堆、元空间内数据的配合）。所以 Java 虚拟机栈是虚拟机执行引擎的核心之一。而 Java 虚拟机栈中出栈入栈的元素就称为「栈帧」。

栈帧(Stack Frame)是用于支持虚拟机进行方法调用和方法执行的数据结构。栈帧存储了方法的局部变量表、操作数栈、动态连接和方法返回地址等信息。每一个方法从调用至执行完成的过程，都对应着一个栈帧在虚拟机栈里从入栈到出栈的过程。

PS：栈对应线程，栈帧对应方法。

在活动线程中， 只有位于栈顶的帧才是有效的， 称为当前栈帧。正在执行的方法称为当前方法。在执行引擎运行时， 所有指令都只能针对当前栈帧进行操作。而 <span style="color:red;">StackOverflowError</span> 表示请求的栈溢出， 导致内存耗尽， 通常出现在递归方法中。

虚拟机栈通过 pop 和 push 的方式，对每个方法对应的活动栈帧进行运算处理，方法正常执行结束，肯定会跳转到另一个栈帧上。在执行的过程中，如果出现了异常，会进行异常回溯，返回地址通过异常处理表确定。

可以看出栈帧在整个 JVM 体系中的地位颇高。下面也具体介绍一下栈帧中的存储信息。

![image-20200929173814739](https://raw.githubusercontent.com/ZGYSYY/notes-resources/master/后台/Java/JVM笔记/JVM%20内存布局/image-20200929173814739.png)

## 1、局部变量表

> 局部变量表就是存放方法参数和方法内部定义的局部变量的区域。

局部变量表所需的内存空间在编译期间完成分配，当进入一个方法时，这个方法需要在帧中分配多大的局部变量空间是完全确定的，在方法运行期间不会改变局部变量表的大小。

这里直接上代码，更好理解。

```java
public int test(int a, int b) {
    Object obj = new Object();
    return a + b;
}
```

如果局部变量是 Java 的 8 种基本基本数据类型，则存在局部变量表中，如果是引用类型。如 new 出来的 String，局部变量表中存的是引用，而实例在堆中。

![image-20200929174005123](https://raw.githubusercontent.com/ZGYSYY/notes-resources/master/后台/Java/JVM笔记/JVM%20内存布局/image-20200929174005123.png)

## 2、操作数栈

操作数栈（Operand Stack）看名字可以知道是一个栈结构。Java 虚拟机的解释执行引擎称为“基于栈的执行引擎”，其中所指的“栈”就是操作数栈。当 JVM 为方法创建栈帧的时候，在栈帧中为方法创建一个操作数栈，保证方法内指令可以完成工作。

还是用实操理解一下。

```java
/**
 * @author Richard_yyf
 */
public class OperandStackTest {

    public int sum(int a, int b) {
        return a + b;
    }
}
```

编译生成 .class 文件之后，再反汇编查看汇编指令

```bash
> javac OperandStackTest.java
> javap -v OperandStackTest.class > 1.txt
```

```text
 public int sum(int, int);
    descriptor: (II)I
    flags: ACC_PUBLIC
    Code:
      stack=2, locals=3, args_size=3 // 最大栈深度为2 局部变量个数为3
         0: iload_1 // 局部变量1 压栈
         1: iload_2 // 局部变量2 压栈
         2: iadd    // 栈顶两个元素相加，计算结果压栈
         3: ireturn
      LineNumberTable:
        line 10: 0
```

操作数栈和局部变量表一样，在编译时期就已经确定了该方法所需要分配的局部变量表的最大容量。

操作数栈的每一个元素可用是任意的Java数据类型，包括long和double。32位数据类型所占的栈容量为1，64位数据类型占用的栈容量为2。

当一个方法刚刚开始执行的时候，这个方法的操作数栈是空的，在方法执行的过程中，会有各种字节码指令往操作数栈中写入和提取内容，也就是 <span style="color:Orange;">出栈 / 入栈</span>操作。

例如，在做 <span style="color:Orange;">算术运算</span>的时候是通过操作数栈来进行的，又或者在调用其它方法的时候是通过操作数栈来进行 <span style="color:Orange;">参数传递</span>的。

![image-20200929175919072](https://raw.githubusercontent.com/ZGYSYY/notes-resources/master/后台/Java/JVM笔记/JVM%20内存布局/image-20200929175919072.png)

索然两个栈帧作为虚拟机栈的元素是完全独立的，但是虚拟机会做出相应的优化，令两个栈帧出现一部分重叠。

如上图所示，栈帧的部分操作数栈与上一个栈帧的局部变量表重叠在一起，这样在进行方法调用时就可以共用一部分数据，无须进行额外的参数复制传递。

## 3、动态连接

每个栈帧都包含一个指向运行时常量池中该栈帧所属方法的引用，持有这个引用是为了支付方法调用过程中的动态连接（Dynamic Linking）。

在类加载阶段中的解析阶段会将符号引用转为直接引用，这种转化也称为静态解析。另外的一部分将在每一次运行时期转化为直接引用。这部分称为动态连接。

## 4、方法返回地址

方法执行时有两种退出情况：

1. 正常退出，即正常执行到任何方法的返回字节码指令，如 <span style="color:red;">RETURN、IRETURN、ARETURN</span> 等。
2. 异常退出，即在方法执行过程中遇到了异常，并且没有处理这个异常，就会导致方法退出。

无论何种退出情况，都将返回至方法当前被调用的位置。方法退出的过程相当于弹出当前栈帧，退出可能有三种方式：

1. 返回值压入上层调用栈帧。
2. 异常信息抛给能够处理的栈帧。
3. PC 计数器指向方法调用后的下一条指令。

# 本地方法栈

本地方法栈（Native Method Stack）与虚拟机栈所发挥的作用是非常相似的，它们之间的区别不过是虚拟机栈为虚拟机执行 Java 方法（也就是字节码）服务，而本地方法栈则为虚拟机使用到的 Native 方法服务。在虚拟机规范中对本地方法栈中方法使用的语言、使用方式与数据结构并没有强制规定，因此具体的虚拟机可以自由实现它。甚至有的虚拟机（譬如 Sun HotSpot 虚拟机）直接就把本地方法栈和虚拟机栈合二为一。与虚拟机栈一样，本地方法栈区域也会抛出 <span style="color:red;">StackOverflowError</span> 和 <span style="color:red;">OutOfMemoryError</span> 异常。

# 程序计数器

程序计数器（Program Counter Register）是一块较小的内存空间。是线程私有的。它可以看作是当前线程所执行的字节码的行号指示器。什么意思呢？

白话版本：因为代码是在线程中运行的，线程有可能被挂起。即 CPU 一会执行线程 A，线程 A 还没有执行完被挂起了，接着执行线程 B，最后又来执行线程 A 了，CPU 得知道执行线程A的哪一部分指令，线程计数器会告诉 CPU。

由于 Java 虚拟机的多线程是通过线程轮流切换并分配处理器执行时间的方式来实现的，CPU 只有把数据装载到寄存器才能够运行。寄存器存储指令相关的现场信息，由于 CPU 时间片轮限制，众多线程在并发执行过程中，任何一个确定的时刻，一个处理器或者多核处理器中的一个内核，只会执行某个线程中的一条指令。

因此，为了线程切换后能恢复到正确的执行位置，每条线程都需要有一个独立的程序计数器，各条线程之间计数器互不影响，独立存储。每个线程在创建后，都会产生自己的程序计数器和栈帧，程序计数器用来存放执行指令的偏移量和行号指示器等，线程执行或恢复都要依赖程序计数器。此区域也不会发生内存溢出异常。

# Metaspace 元空间

在 <span style="color:red;">HotSpot JVM</span> 中，永久代（ ≈ 方法区）中用于存放类和方法的元数据以及常量池，比如 <span style="color:red;">Class</span> 和 <span style="color:red;">Method</span>。每当一个类初次被加载的时候，它的元数据都会放到永久代中。

永久代是有大小限制的，因此如果加载的类太多，很有可能导致永久代内存溢出，即万恶的 <span style="color:red;">java.lang.OutOfMemoryError: PermGen</span>，为此我们不得不对虚拟机做调优。

那么，Java 8 中 PermGen 为什么被移出 <span style="color:red;">HotSpot JVM</span> 了？（详见：JEP 122: Remove the Permanent Generation）：

1. 由于 <span style="color:red;">PermGen</span> 内存经常会溢出，引发恼人的 <span style="color:red;">java.lang.OutOfMemoryError: PermGen</span>，因此 JVM 的开发者希望这一块内存可以更灵活地被管理，不要再经常出现这样的 OOM。
2. 移除 PermGen 可以促进 <span style="color:red;">HotSpot JVM</span> 与 <span style="color:red;">JRockit VM</span> 的融合，因为 <span style="color:red;">JRockit</span> 没有永久代。

根据上面的各种原因，<span style="color:red;">PermGen</span> 最终被移除，方法区移至 <span style="color:red;">Metaspace</span>，字符串常量池移至堆区。

准确来说，Perm 区中的字符串常量池被移到了堆内存中是在 Java7 之后，Java 8 时，PermGen 被元空间代替，其他内容比如类元信息、字段、静态属性、方法、常量等都移动到元空间区。比如 <span style="color:red;">java/lang/Object</span> 类元信息、静态属性 System.out、整形常量 100000 等。

元空间的本质和永久代类似，都是对 JVM 规范中方法区的实现。不过元空间与永久代之间最大的区别在于：元空间并不在虚拟机中，而是使用本地内存。因此，默认情况下，元空间的大小仅受本地内存限制。（和后面提到的直接内存一样，都是使用本地内存）

| 参数                        | 作用                                                         |
| --------------------------- | :----------------------------------------------------------- |
| -XX:MetaspaceSize=N         | 这个参数是初始化的Metaspace大小，该值越大触发Metaspace GC的时机就越晚。随着GC的到来，虚拟机会根据实际情况调控Metaspace的大小，可能增加上线也可能降低。在默认情况下，这个值大小根据不同的平台在12M到20M浮动。使用java -XX:+PrintFlagsInitial命令查看本机的初始化参数，-XX:Metaspacesize为21810376B（大约20.8M）。 |
| -XX:MaxMetaspaceSize=N      | 这个参数用于限制Metaspace增长的上限，防止因为某些情况导致Metaspace无限的使用本地内存，影响到其他程序。在本机上该参数的默认值为4294967295B（大约4096MB）。 |
| -XX:MinMetaspaceFreeRatio=N | 当进行过Metaspace GC之后，会计算当前Metaspace的空闲空间比，如果空闲比小于这个参数，那么虚拟机将增长Metaspace的大小。在本机该参数的默认值为40，也就是40%。设置该参数可以控制Metaspace的增长的速度，太小的值会导致Metaspace增长的缓慢，Metaspace的使用逐渐趋于饱和，可能会影响之后类的加载。而太大的值会导致Metaspace增长的过快，浪费内存。 |
| -XX:MaxMetasaceFreeRatio=N  | 当进行过Metaspace GC之后， 会计算当前Metaspace的空闲空间比，如果空闲比大于这个参数，那么虚拟机会释放Metaspace的部分空间。在本机该参数的默认值为70，也就是70%。 |
| -XX:MaxMetaspaceExpansion=N | Metaspace增长时的最大幅度。在本机上该参数的默认值为5452592B（大约为5MB）。 |
| -XX:MinMetaspaceExpansion=N | Metaspace增长时的最小幅度。在本机上该参数的默认值为340784B（大约330KB为）。 |

>延伸阅读：关于 Metaspace 比较好的两篇文章
>Metaspace in Java 8
>http://lovestblog.cn/blog/2016/10/29/metaspace/
>
>PS：我也没有看过！

# 直接内存

直接内存（Direct Memory）并不是虚拟机运行时数据区的一部分，也不是 Java 虚拟机规范中定义的内存区域。但是这部分内存也被频繁地使用，而且也可能导致 OutOfMemoryError 异常出现，所以我们放到这里一起讲解。

在 JDK 1.4 中新加入了 NIO（New Input/Output）类，引入了一种基于通道（Channel）与缓冲区（Buffer）的 I/O 方式，它可以使用 Native 函数库直接分配堆外内存，然后通过一个存储在 Java 堆中的 <span style="color:red;">DirectByteBuffer</span> 对象作为这块内存的引用进行操作。这样能在一些场景中显著提高性能，因为避免了在 Java 堆和 Native 堆中来回复制数据。

显然，本机直接内存的分配不会受到 Java 堆大小的限制，但是，既然是内存，肯定还是会受到本机总内存（包括 RAM 以及 SWAP 区或者分页文件）大小以及处理器寻址空间的限制。如果内存区域总和大于物理内存的限制，也会出现 OOM。

# Code Cache

简而言之， JVM 代码缓存是 JVM 将其字节码存储为本机代码的区域 。我们将可执行本机代码的每个块称为  <span style="color:red;">nmethod</span>。该  <span style="color:red;">nmethod</span> 可能是一个完整的或内联 Java 方法。

实时（JIT）编译器是代码缓存区域的最大消费者。这就是为什么一些开发人员将此内存称为 JIT 代码缓存的原因。

这部分代码所占用的内存空间成为  <span style="color:red;">CodeCache</span> 区域。一般情况下我们是不会关心这部分区域的且大部分开发人员对这块区域也不熟悉。如果这块区域 OOM 了，在日志里面就会看到：
 <span style="color:red;">java.lang.OutOfMemoryError code cache</span>。

<b>诊断选项</b>

![image-20200929174806596](https://raw.githubusercontent.com/ZGYSYY/notes-resources/master/后台/Java/JVM笔记/JVM%20内存布局/image-20200929174806596.png)

>延伸阅读：Introduction to JVM Code Cache
>https://www.baeldung.com/jvm-code-cache
>
>PS：我也没有看过！