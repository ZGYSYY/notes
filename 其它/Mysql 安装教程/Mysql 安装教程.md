# 目录

[TOC]

# 添加虚拟内存 swap

swap 被称为虚拟内存。作用是当内存不足时，可以暂时将内存中运行的程序存放在虚拟内存中。缺点是会降低程序的执行效率。

创建 swap 有两种方式，一种是在磁盘分区上创建 swap，另一种是在一个文件上创建 swap。

如果在内存比较充足的情况下，该步骤可以省略。

## 1、磁盘分区创建 swap

前提条件：磁盘中有多余的未进行格式化的容量。

## 2、文件创建 swap

这种方案的使用场景：磁盘中没有多余的未进行格式化的容量。

步骤如下：

1. 创建一个内容为空的文件，使用 `dd` 命令

    ```bash
    # 在目录 /tmp 下创建文件 swap，大小为 16G 的空文件
    dd if=/dev/zero of=/tmp/swap bs=1G count=16
    
    # 查看创建文件的文件信息
    ls -lh /tmp/swap
    ```

2. 将文件格式化为 swap 的文件格式，使用 `mkswap` 命令

    ```bash
    mkswap /tmp/swap
    ```

    ![image-20221101231121524](Mysql%20%E5%AE%89%E8%A3%85%E6%95%99%E7%A8%8B.assets/image-20221101231121524.png)

3. 改变 swap 文件的权限，使用 `chmod` 命令

    ```bash
    # 当前用户能读和写
    chmod 600 /tmp/swap
    ```

    

4. 激活 swap，使用 `swapon` 命令

    ```bash
    swapon /tmp/swap
    ```

5. 设置 swap 在启动时，自动激活，修改 `/etc/fstab` 文件

    ![截屏2022-11-01 23.26.53](Mysql%20%E5%AE%89%E8%A3%85%E6%95%99%E7%A8%8B.assets/%E6%88%AA%E5%B1%8F2022-11-01%2023.26.53.png)

6. 查看 swap 是否激活成功

    ```bash
    swapon -s
    ```

7. 重启系统，验证 swap 是否生效

    ```bash
    # 重启系统
    shutdown -r now
    
    # 查看 swap 是否生效
    free
    ```

    ![image-20221101233550579](Mysql%20%E5%AE%89%E8%A3%85%E6%95%99%E7%A8%8B.assets/image-20221101233550579.png)
