# 深入分析Java I/O的工作机制

磁盘IO

网络IO

同步

异步

阻塞

非阻塞

NIO

## Java的IO操作类分组

- 基于字节操作的IO接口：InputStream 和 OutputStream。

  ![InputStream的类层次结构](C:/Users/ZGY/AppData/Roaming/Typora/typora-user-images/1563601055028.png)

  

  ![OutputStream的类层次结构](C:/Users/ZGY/AppData/Roaming/Typora/typora-user-images/1563601826581.png)

  

- 基于字符操作的IO接口：Writer 和 Reader。

  ![Writer类层次结构](C:/Users/ZGY/AppData/Roaming/Typora/typora-user-images/1563602353863.png)

  

  ![Reader类层次结构](C:/Users/ZGY/AppData/Roaming/Typora/typora-user-images/1563602661283.png)

- 基于磁盘操作的IO接口：File。

- 基于网络操作的IO接口：Socket。

## 影响IO操作的核心问题

1. 数据格式。
2. 传输方式。

## 几种访问文件的方式

- 标准访问文件的方式。
- 直接I/O的方式。
- 同步访问文件的方式。
- 异步访问文件的方式。
- 内存映射的方式。