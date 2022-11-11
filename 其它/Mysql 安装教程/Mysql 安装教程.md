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



# 安装MySql

## 1、下载安装包

安装包：[mysql-8.0.31-linux-glibc2.17-x86_64-minimal.tar.xz](https://cdn.mysql.com//Downloads/MySQL-8.0/mysql-8.0.31-linux-glibc2.17-x86_64-minimal.tar.xz)

## 2、安装必要依赖

检查系统中是否有 libaio

```bash
yum list installed | grep libaio
```

![image-20221103223719334](Mysql%20%E5%AE%89%E8%A3%85%E6%95%99%E7%A8%8B.assets/image-20221103223719334.png)

没有 libaio 则执行下面的命令安装

```bash
yum install libaio -y
```

检查系统中是否有 ncurses-compat-libs

```bash
yum list installed | grep ncurses-compat-libs
```

![image-20221103223935762](Mysql%20%E5%AE%89%E8%A3%85%E6%95%99%E7%A8%8B.assets/image-20221103223935762.png)

没有 ncurses-compat-libs 则执行下面的命令安装

```bash
yum install ncurses-compat-libs -y
```

## 3、安装 MySql

先将安装包解压到 `/usr/local/mysql` 目录下，然后继续下面的操作

```bash
# 创建用户组 mysql
groupadd mysql

# 创建用户 mysql
useradd -r -g mysql -s /bin/false mysql

# 初始化数据目录
cd /usr/local/mysql
bin/mysqld --initialize --user=mysql # 注意看初始化过程中生成的随机密码

# 启动 mysql
bin/mysqld_safe --user=mysql &
```

![截屏2022-11-03 23.08.59](Mysql%20%E5%AE%89%E8%A3%85%E6%95%99%E7%A8%8B.assets/%E6%88%AA%E5%B1%8F2022-11-03%2023.08.59.png)

## 4、其他配置

### 4.1、验证 MySql 是否安装成功

使用 `ps` 命令

```bash
ps -ef | grep mysql
```

![image-20221103230458872](Mysql%20%E5%AE%89%E8%A3%85%E6%95%99%E7%A8%8B.assets/image-20221103230458872.png)

使用客户端登录

```bash
# 进入 MySql 的 bin 目录
cd /usr/local/mysql/bin
# 使用客户端工具登录，并输入密码，（密码是【第3节】生成的随机密码
./mysql -u root -p
```

![image-20221103231108907](Mysql%20%E5%AE%89%E8%A3%85%E6%95%99%E7%A8%8B.assets/image-20221103231108907.png)

出现上图效果，说明 MySql 已经安装成功

### 4.2、修改 MySql 密码

```bash
ALTER USER 'root'@'localhost' IDENTIFIED BY '新密码';
```

### 4.2、解决 MySql 只能通过本机连接问题

修改 `mysql` 库中的 `user` 表的字段为 `'%'`

```bash
# 切换到 mysql 库
use mysql;
# 查看有哪些表
show tables;
# 查看 user 表的表结构
desc user;
# 查看用户和主机信息
SELECT User,Host FROM user;
# 修改 root 的 Host 为 %
UPDATE user SET HOST='%' WHERE USER='root';
# 将用户数据和权限数据刷新到内存中
FLUSH PRIVILEGES;
```

### 4.3、MySql 默认会读取的配置文件 `/etc/mysql/my.cnf`

下面的例子是修改 MySql 的一些配置信息，并进行服务重启

```bash
cd /usr/local/mysql
# 停止 MySql 服务
bin/mysqladmin -uroot -p shutdown
```

创建配置文件 `/etc/mysql/my.cnf`，内容如下

```tex
[client]
port=53306
socket=/tmp/mysql.sock

[mysqld]
port=53306
socket=/tmp/mysql.sock
key_buffer_size=16M
max_allowed_packet=128M
character-set-server=utf8mb4
collation-server=utf8mb4_general_ci
init_connect='SET NAMES utf8mb4'

[mysqldump]
quick
```

启动 MySql 服务

```bash
cd /usr/local/mysql
bin/mysqld_safe --user=mysql &
```

### 4.4、让 MySql 开机自动启动

暂时还没有找到方法。

## 5、常见问题

1. MySql 默认的数据文件在安装目录下的 `data` 目录中，在进行初始化化目录的时候会自动创建该目录。

2. 无法使用 Navicat 连接数据库，提示 `caching_sha2_password could not be loaded` 错误。

    这是因为在 **MySQL-8.0.3** 前，MySql 默认用的是 **mysql_native_password** 密码认证方式，这种方式是使用 **SHA1** 来实现的，存在的问题就是，如果密码相同，生成的密文就是相同的。

    在之后的版本后，为了解决上面的问题，MySql 默认用的是 **caching_sha2_password** 密码认证方式，这种方式使用 **caching_sha2_password** 密码认证方式 来实现的，这样就算密码相同，生成的密文也不相同，从而增加了安全性。

    为什么 Navicat 提示 `caching_sha2_password could not be loaded` 错误，是因为一些版本比较老的 Navicat 内置的 MySql 驱动还不支持 **caching_sha2_password** 密码认证方式，所以解决办法是，要么升级客户端，要么将 MySQL 里面相应账号的 密码认证方式改为 **mysql_native_password**。步骤如下

    ```bash
    # 切换到 mysql 库
    use mysql;
    # 查看用户的密码认证方式
    SELECT Host,User,plugin FROM user;
    ```

    ![image-20221111133935626](Mysql%20%E5%AE%89%E8%A3%85%E6%95%99%E7%A8%8B.assets/image-20221111133935626.png)

    ```bash
    # 修改 root 用户的密码认证方式为 mysql_native_password，并重新设置密码
    ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '新密码';
    # 将用户数据和权限数据刷新到内存中
    FLUSH PRIVILEGES;
    # 验证用户的密码认证方式是否生效
    SELECT Host,User,plugin FROM user;
    ```

    
