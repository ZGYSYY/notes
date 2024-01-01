<center><h1>Linux常用命令总结</h1></center>
# 目录

[TOC]

# yum常用命令

- yum install 【软件包名】[-y]：安装指定的软件包，如果加上`-y`表示不询问安装。
- yum remove 【软件包名】[-y]：删除指定软件包，如果加上`-y`表示不询问删除。
- yum list installed：列出所用安装过的软件，可以配合`|`和`grep`命令做过滤。
- yum list：列出所用软件，可以配合`|`和`grep`命令做过滤。
- yum search 【软件包名】：查找指定的软件包。
- yum repolist all：查看软件库列表。
- yum clean all：删除缓存。
- yum update ：更新所用的软件包，如果后面添加【软件包名】就是更新指定的软件包。

# rpm命令

- rpm -e 【软件包名】：删除软件包。
- rpm -ivh 【软件包名】：安装指定软件包。
- rpm -Uvh 【软件包名】：更新指定软件包。
- rpm -Uvh --oldpackage 【软件包名】：软甲包降级。
- rpm -qa 【软件包名】：查找指定软件，可以配合`|`和`grep`命令做过滤。
- rpm -ql 【软件包名】：查看软件安装的路径。

# 修改主机名

```bash
# 显示主机名以及其它信息
hostnamectl
# 查看主机名
cat /etc/hostname
# 设置主机名
hostnamectl set-hostname 主机名
# 设置主机（存在大写）
hostnamectl set-hostname --static 主机名
```

# 时间调整

```bash
# 显示目前的系统时区与时间等信息
timedatectl
# 设置系统时区为中国/上海
timedatectl set-timezone Asia/Shanghai
# 设置硬件时间为 UTC 时间，0：UTC；1：本地时间（CST）
timedatectl set-local-rtc 0
```

> **Tips**
>
> 建议将硬件时间设置为 UTC。

# 内核升级

CentOS7 内核版本 3.10.* 在安装有些 Bug，建议升级到最新长期维护版比较好。

查看当前 Linux 最新内核版本网址：https://www.kernel.org/

RedHat 内核仓库网址：http://elrepo.org/

安装步骤如下：

1. 导入签名文件，签名文件目录为：/etc/pki/rpm-gpg/RPM-GPG-KEY-elrepo.org

    ```bash
    rpm --import https://www.elrepo.org/RPM-GPG-KEY-elrepo.org
    ```

2. 安装仓库包

    ```bash
    rpm -Uvh https://www.elrepo.org/elrepo-release-7.el7.elrepo.noarch.rpm
    ```

3. 搜索内核相关软件

    ```bash
    yum --enablerepo=elrepo-kernel list |grep kernel
    ```

    ![image-20230305215426082](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/image-20230305215426082.png)

4. 安装内核

    ```bash
    yum --enablerepo=elrepo-kernel install -y kernel-lt.x86_64
    ```

5. 设置默认内核

    查看内核列表

    ```bash
    awk -F \' '$1=="menuentry " {print i++ " : " $2}' /etc/grub2.cfg
    ```

    ![image-20230305213524514](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/image-20230305213524514.png)

    查看当前默认启动内核

    ```bash
    grub2-editenv list
    ```

    ![image-20230305213636324](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/image-20230305213636324.png)

    设置默认启动内核

    ```bash
    # 设置默认启动内核
    grub2-set-default 'CentOS Linux (5.4.234-1.el7.elrepo.x86_64) 7 (Core)'
    # 查看当前默认启动内核
    grub2-editenv list
    ```

    ![image-20230305213924496](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/image-20230305213924496.png)

6. 重启系统验证

    ```bash
    reboot
    ```

    ![image-20230305214158146](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/image-20230305214158146.png)

    ```bash
    uname -a
    ```

    ![image-20230305214332015](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/image-20230305214332015.png)

7. 删除旧内核（可选）

    查看当前系统已安装的内核列表

    ```bash
    yum list installed |grep kernel
    ```

    ![image-20230305214734371](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/image-20230305214734371.png)

    卸载相关的软件包

    ```bash
    yum remove -y kernel.x86_64
    yum remove -y kernel-tools.x86_64 kernel-tools-libs.x86_64
    ```

    重启系统

    ```bash
    reboot
    ```

# 系统服务

## 1、systemd

### 1.1、配置文件目录

- /usr/lib/systemd/system/：每个服务最主要的启动脚本设定，有点类似以前的 /etc/init.d 底下的文件；
- /run/systemd/system/：系统执行过程中所产生的服务脚本，这些脚本的优先序要比 /usr/lib/systemd/system/ 高！
- /etc/systemd/system/：管理员依据主机系统的需求所建立的执行脚本，这个目录有点像以前 /etc/rc.d/rc5.d/Sxx 之类的功能！执行优先序又比 /run/systemd/system/ 高！

> **Tips**
>
> 通常使用 systemctl 命令来启动某个服务时，如果不知道服务名的时候，可以到 /usr/lib/systemd/system/ 下看看服务名称。

## 2、systemctl

systemctl命令是系统服务管理器指令，它实际上将 service 和 chkconfig 这两个命令组合到一起。

<table>
    <tr>
    	<th>任务</th>
        <th>旧指令</th>
        <th>新指令</th>
    </tr>
    <tr>
        <td>使某服务自动启动</td>
        <td>chkconfig --level 3 httpd on</td>
        <td>systemctl enable httpd.service</td>
    </tr>
    <tr>
        <td>使某服务不自动启动</td>
        <td>chkconfig --level 3 httpd off</td>
        <td>systemctl disable httpd.service</td>
    </tr>
    <tr>
        <td>检查服务状态</td>
        <td>service httpd status</td>
        <td>systemctl status httpd.service （服务详细信息） systemctl is-active httpd.service （仅显示是否 Active)</td>
    </tr>
    <tr>
        <td>显示所有已启动的服务</td>
        <td>chkconfig --list</td>
        <td>systemctl list-units --type=service</td>
    </tr>
    <tr>
        <td>启动某服务</td>
        <td>service httpd start</td>
        <td>systemctl start httpd.service</td>
    </tr>
     <tr>
        <td>停止某服务</td>
        <td>service httpd stop</td>
        <td>systemctl stop httpd.service</td>
    </tr>
     <tr>
        <td>重启某服务</td>
        <td>service httpd restart</td>
        <td>systemctl restart httpd.service</td>
    </tr>
    <tr>
        <td>查看自启服务</td>
        <td>--</td>
        <td>systemctl list-unit-files</td>
    </tr>
</table>
# 内核参数

查看系统核心参数的命令如下：`sysctl -a`。

配置系统核心参数的目录为：/etc/sysctl.d。

使配置核心参数生效的命令如下：sysctl -p [配置文件全路径]

# 核心模块

## 1、查看模块

内核模块保存位置：/lib/modules/*。

扫描系统中已有的模块，命令如下：

```bash
depmod
```

参数说明如下：

- 不加选项：depmod 命令会扫描系统中的内核模块，并写入 modules.dep 文件；
- -a：扫描所有模块；
- -A：扫描新模块，只有有新模块时，才会更新modules.dep文件；
- -n：把扫描结果不写入modules.dep文件，而是输出到屏幕上；

查看系统中到底安装了哪些内核模块，命令如下：`lsmod`。

## 2、加载/卸载模块

加载/卸载模块，语法如下：

modprobe [选项] 模块名。

选项说明如下：

- -f：强制加载模块；

- -r：删除模块。



# CentOS7 日志服务

## 1、简单介绍

CentOS7 中，记录日志的服务有 rsyslog 和 systemd-journald 服务。

rsyslogd 必须要开机完成并且执行了 rsyslogd 这个 daemon 之后，登录文件才会开始记录。所以，核心还得要自己产生一个 klogd 的服务， 才能将系统在开机过程、启动服务的过程中的信息记录下来。

在有了 systemd 之后，由于这玩意儿是核心唤醒的，然后又是第一支执行的软件，它可以主动呼叫 systemd-journald 来协助记载登录文件，因此在开机过程中的所有信息，包括启动服务与服务若启动失败的情况等等，都可以直接被记录到 systemd-journald 里头去。

## 2、systemd-journald 配置日志持久化

systemd-journald 默认是把日志保存到内存中（在 /run/log/journal/ 目录下），因此系统重新启动过后，对应的日志数据就会被清空。

但是可通过新建目录，让日志自动记录到新建目录中，并永久存储。步骤如下：

1. 修改 /etc/systemd/journald.conf 配置文件，内如如下：

    ```tex
    Storage=persistent # 将日志数据保存到磁盘上
    Compress=yes # 压缩历史日志
    SyncIntervalSec=5m # 向磁盘刷写日志文件的时间间隔，单位为分
    RateLimitInterval=30s # 限制日志的生成速率，单位是秒
    RateLimitBurst=1000 # 表示消息条数，与 RateLimitInterval 配合使用
    SystemMaxUse=10G # 最大占用磁盘空间
    SystemMaxFileSize=200M # 单日志文件最大 200M
    MaxRetentionSec=2week # 日志保存时间 2 周
    ForwardToSyslog=no # 不将日志转发到 rsyslog 中
    ```

2. 创建目录

    ```bash
    cd /var/log
    mkdir journal
    chown root:systemd-journal /var/log/journal
    chmod 2775 /var/log/journal
    ```

3. 重启服务，命令如下：`systemctl restart systemd-journald`。

4. 检验配置是否生效

    进入 /var/log/journal 目录下，查看是否有文件生成，如果有，则表示配置生效。

5. 停用 rsyslog （可选）

    ```bash
    systemctl stop rsyslog
    systemctl disable rsyslog
    ```

## 3、rsyslog 和 systemd-journald 关系

两者没有什么直接关系，rsyslog 保存的是文本文件，systemd-journald 保存的是二进制文件。

两者功能存在重复，因此可以停掉一个服务，看自己愿意使用那种日志记录方式。

# curl 命令

- curl -o 【文件名】url：将指定的url资源下载到指定的文件中。
- curl -L url：如果指定的url返回重定向，将跳转重定向地址，如果不加`L`将不回重定向。

# ln 命令

- ln -s 【源文件】 【目标文件】：给源文件创建一个软连接到目标文件。

更多详情，参考以下链接：

[linux 创建连接命令 ln -s 软链接](https://www.cnblogs.com/kex1n/p/5193826.html)

# wget 命令

- wget -O 【文件名】 url：将指定的url资源下载到指定的文件中。

更多详情，参考以下链接：

[wget命令详解](https://www.cnblogs.com/peida/archive/2013/03/18/2965369.html)

# scp 命令

将数据由本机上传到远程服务器，命令如下：

```bash
scp /etc/test.txt zgy@192.168.1.88:/home/
```

将数据由远程主机下载到本机上，命令如下：

```bash
scp zgy@192.168.11.88:/etc/test.txt .
```

<b><font color="red">Tips</font></b>：在传输目录时，要加上 `-r` 参数。

# sed 命令

替换正则表达式匹配的内容，并生成备份文件，命令如下：

```bash
sed -i'.bak' 's,/[a-z]*.ubuntu.com,/mirrors.tuna.tsinghua.edu.cn,' /etc/apt/sources.list
```

# SELinux

查看SELinux状态

```bash
sestatus  -v
```

临时关闭

```bash
setenforce 0
```

永久关闭

修改 /etc/selinux/config 文件中的 SELINUX="enforcing" 为 disabled ，然后重启。

# 防火墙

## 1、iptables

### 1.1、简介

iptables 不是真正的防火墙，它只是用来定义防火墙策略的防火墙管理工具而已，真正实现防火墙功能的是内核层面的 netfilter 网络过滤器。

### 1.2、使用

1.2.1、关闭 firewall 服务

```bash
systemctl stop firewalld.service # 停止服务
systemctl disable firewalld.service # 禁用开机自启
systemctl status firewalld.service # 查看状态
```

1.2.2、安装 iptables-service 服务

```bash
yum -y install iptables-services # 安装服务
systemctl start iptables # 启动服务
systemctl enable iptables # 启动开机自启
systemctl status iptables # 查看状态
```

1.2.3、清除规则

```bash
iptables -F # 清除所有的已订定的规则
iptables -X # 清除自定义的 table
iptables -Z # 将所有的 chain 的计数与流量统计都归零
```

4、定义预设政策（policy）

当封包不在设定的规则之内时，则该封包的通过与否，是以 Policy 的设定为准。

通常建议将 INPUT 设置为 DROP，OUTPUT 和 FORWARD 设置为 ACCEPT。

范例：将本机的 INPUT 设定为 DROP ，其他设定为 ACCEPT，命令如下：

```bash
iptables -P INPUT DROP # 该命令切忌不要在远程客户端执行
iptables -P OUTPUT ACCEPT
iptables -P FORWARD ACCEPT
```

5、范例

5.1、设定 lo 成为受信任的装置，亦即进出 lo 的封包都予以接受，命令如下：

```bash
iptables -A INPUT -i lo -j ACCEPT
```

参数解释：

- -A：新增加一条规则，该规则增加在原本规则的最后面。例如原本已经有四条规则，使用 -A 就可以加上第五条规则。

- -i：封包所进入的那个网络接口，例如 eth0，lo 等接口。需与 INPUT 链配合。

- -j：后面接动作，主要的动作有接受(ACCEPT)、丢弃(DROP)、拒绝(REJECT)以及记录(LOG)。

5.2、只要是来自 192.168.1.0/24 网域的封包都予以接受，命令如下：

```bash
iptables -A INPUT -i ens33 -s 192.168.1.0/24 -j ACCEPT
```

参数解释：

- -s：设定此规则之封包的来源，可指定单纯的 IP 或网域。IP格式为 192.168.0.100。网域格式为 192.168.0.0/24 或者 192.168.0.0/255.255.255.0 均可。若规范为“不许”时，则加上 ! 即可，格式为 !192.168.100.0/24。

5.3、只要是来自 192.168.1.6 就接受，但 192.168.1.8 这个恶意来源就丢弃，命令如下：

```bash
iptables -A INPUT -i ens33 -s 192.168.1.6 -j ACCEPT
iptables -A INPUT -i ens33 -s 192.168.1.8 -j DROP
```

5.4、想要联机进入本机 port 为 21 的封包都抵挡掉，命令如下：

```
iptables -A INPUT -i ens33 -p tcp --dport 21 -j DROP
```

参数解释：

- -p：封包的协议，有 tcp 和 udp。

- --dport：封包目标端口号，必须要和 -p 一起使用。

5.5、想要联机进入本机的 udp 协议下的 137 和 138 端口，以及 tcp 协议下的 139 和 445 端口，命令如下：

```bash
iptables -A INPUT -i ens33 -p udp --dport 137:138 -j ACCEPT
iptables -A INPUT -i ens33 -p tcp --dport 139 -j ACCEPT
iptables -A INPUT -i ens33 -p tcp --dport 445 -j ACCEPT
```

5.6、只要来自 192.168.1.0/24 的 1024:65535 端口的封包，且想要联机到本机的 ssh port 就予以抵挡，命令如下：

```bash
iptables -A INPUT -i ens33 -p tcp -s 192.168.1.0/24 --sport 1024:65534 --dport ssh -j DROP
```

5.7、将来自任何地方来源 port 1:1023 的主动联机到本机端的 1:1023 联机丢弃，命令如下：

```bash
iptables -A INPUT -i ens33 -p tcp --sport 1:1023 --dport 1:1023 --syn -j DROP
```

参数解释：

- --syn：TCP 建立握手的旗标。

5.8、只要已建立或相关封包就予以通过，只要是不合法封包就丢弃，命令如下：

```bash
iptables -A INPUT -m state --state RELATED,ESTABLISHED -j ACCEPT
iptables -A INPUT -m state --state INVALID -j DROP
```

参数解释：

- -m：外挂模块，有 state 和 mac 两个值，state：状态模块，mac：网卡硬件地址。

- --state：一些封包的状态，主要有：
    - INVALID：无效的封包，例如数据破损的封包状态；
    - ESTABLISHED：已经联机成功的联机状态；
    - NEW：想要新建立联机的封包状态；
    - RELATED：表示这个封包是与我们主机发送出去的封包有关（常用）。

5.9、针对局域网络内的 aa:bb:cc:dd:ee:ff 主机开放其联机，命令如下：

```bash
	iptables -A INPUT -m mac --mac-source aa:bb:cc:dd:ee:ff -j ACCEPT
```

参数解释：

- --mac-source：来源主机的 mac 地址。

6、保存规则

通常使用 iptables 设置规则，在系统重启后就会失效，想要永久有效，需要执行如下命令：

```bash
service iptables save
```

### 1.3、四表五链

1. 四表

    - Filter 表：iptables 默认表，负责包过滤，防火墙功能；
    - NAT 表：负责网络地址转换功能，对应内核模块；
    - Mangle 表：主要负责修改数据包，对应内核模块；
    - Raw 表：优先级最高，关闭 NAT 表启用的连接追踪机制；

2. 五链

    - PREROUTING 链：路由选择前；
    - INPUT 链：路由目的地为本机；
    - FORWARD 链：路由目的地非本机，转发；
    - OUTPUT 链：本机发出数据包；
    - POSTROUTING 链：路由选择后；

3. 表优先级

    优先级高到低：raw–>mangle–>nat–>filter。

![image-20230315222105875](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/image-20230315222105875.png)

![image-20230315221732874](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/image-20230315221732874.png)

## 2、firewall

### 2.1、简介

在旧版本的CentOS中，是使用 iptables 命令来设置防火墙的。但是，从CentOS7开始，默认就没有安装iptables，而是改用firewall来配置防火墙。

firewall的配置文件是以xml的格式，存储在 /usr/lib/firewalld/ 和 /etc/firewalld/ 目录中。

2.1.1、系统配置目录

```bash
/usr/lib/firewalld/
/usr/lib/firewalld/services
/usr/lib/firewalld/zones
```

2.1.2、用户配置目录

```bash
/etc/firewalld/
/etc/firewalld/services
/etc/firewalld/zones
```

### 2.2、使用

设置防火墙的方式有两种：firewall 命令和直接修改配置文件。

推荐使用 firewall 命令来设置防火墙。

注意： 对防火墙所做的更改，必须重启防火墙服务，才会立即生效。命令如下：

```bash
service firewalld restart 或 systemctl restart firewalld
```

2.1、firewall 命令

```bash
# 对外开放 3306 端口，供外部的计算机访问
# 该命令方式添加的端口，可在 /etc/firewalld/zones 中的对应配置文件中得到体现
firewall-cmd --zone=public --add-port=3306/tcp --permanent

# 查看端口状态
firewall-cmd --zone=public --query-port=3306/tcp --permanent

# 对外关闭 3306 端口
firewall-cmd --zone=public --remove-port=3306/tcp --permanent

# 重启防火墙
systemctl restart firewalld
```

说明：

- firewall-cmd：Linux 中提供的操作 firewall 的工具。
- –zone：指定作用域。
- –add-port=80/tcp：添加的端口，格式为：端口/通讯协议。
- –permanent：表示永久生效，没有此参数重启后会失效。

2.2、直接修改配置文件

/etc/firewalld/zones/public.xml 文件的默认内容为：

```bash
<?xml version="1.0" encoding="utf-8"?>
<zone>
  <short>Public</short>
  <description>For use in public areas. You do not trust the other computers on networks to not harm your computer. Only selected incoming connections are accepted.</description>
  <service name="dhcpv6-client"/>
  <service name="ssh"/>
</zone>
```

修改该配置文件，来添加3306端口。修改后的内容为：

```bash
<?xml version="1.0" encoding="utf-8"?>
<zone>
  <short>Public</short>
  <description>For use in public areas. You do not trust the other computers on networks to not harm your computer. Only selected incoming connections are accepted.</description>
  <service name="dhcpv6-client"/>
  <service name="ssh"/>
  <port protocol="tcp" port="3306"/>
</zone>
```

3、firewall 常用命令

3.1、查看firewall的状态

```bash
service firewalld status
systemctl status firewalld
firewall-cmd --state
```

3.2、启动、停止、重启

```bash
# 启动
service firewalld start
systemctl start firewalld

# 停止
service firewalld stop
systemctl stop firewalld

# 重启
service firewalld restart
systemctl restart firewalld
```

3.3、开机自启动的关闭与开启

```bash
# 关闭开机自启动
systemctl disable firewalld

# 开启开机自启动
systemctl enable firewalld
```

3.4、查看防火墙的规则

```bash
firewall-cmd --list-all 
```

# 端口占用查询

## 1、netstat命令

netstat 命令应用是比较频繁的，比如查看端口占用啦，查看端口进程啦，这些时候都是有必要的。

netstat命令各个参数说明如下：

　　-t : 指明显示TCP端口

　　-u : 指明显示UDP端口

　　-l : 仅显示监听套接字(所谓套接字就是使应用程序能够读写与收发通讯协议(protocol)与资料的程序)

　　-p : 显示进程标识符和程序名称，每一个套接字/端口都属于一个程序。

　　-n : 不进行DNS轮询，显示IP(可以加速操作)

常用命令如下：

查看所有TCP端口

```bash
netstat -ntlp
```

查看所有端口

```bash
netstat -ntulp
```

查看指定端口的连接数量，比如：80

```bash
netstat -pnt |grep :80 |wc
```

更多详情，参考以下链接：

[linux查看端口状态相关命令](https://www.cnblogs.com/cxbhakim/p/9353383.html)

# firewalld与docker冲突问题

在CentOS7中，如果安装有docker，会出现firewalld与docker冲突问题，具体现象为，firewalld中并没有开发端口，但是其它机器依然能够访问docker提供的服务。出现这个问题的原因是因为firewalld和docker的启动顺序造成的，在机器启动后，firewalld服务比docker服务要先启动，在docker服务启动后修改了iptables规则，因为firewalld的底层实现也是基于iptables规则的，导致firewalld服务没有开发的端口，依然能够访问docker提供的服务。

解决办法有两种，如下

1. 不用firewalld，改用iptables
2. 将firewalld服务和docker服务都停止，然后先启动docker服务，过5秒（服务启动存在时间，保证docker完全启动）再启动firewalld服务。

# 查看内存使用情况

## 1、top命令

内容说明如下：

- PID：进程的ID
- USER：进程所有者
- PR：进程的优先级别，越小越优先被执行
- NI：进程Nice值，代表这个进程的优先值
- VIRT：进程占用的虚拟内存
- RES：进程占用的物理内存
- SHR：进程使用的共享内存
- S：进程的状态。S表示休眠，R表示正在运行，Z表示僵死状态
- %CPU：进程占用CPU的使用
- %MEM：进程使用的物理内存和总内存的百分
- TIME+：该进程启动后占用的总的CPU时间，即占用CPU使用时间的累加值
- COMMAND：启动该进程的命令名称

## 2、free命令

```bash
# 用KB为单位展示数据
free
# 用MB为单位展示数据
free -m
# 用GB为单位展示数据
free -h
```

内容说明如下：

- total : 总计屋里内存的大小
- used : 已使用内存的大小
- free : 可用内存的大小
- shared : 多个进程共享的内存总额
- buff/cache : 磁盘缓存大小
- available : 可用内存大小 ， 从应用程序的角度来说：available = free + buff/cache .

# swap 交换分区

## 1、linux可用内存足够为什么还用swap

> 该部分内容摘抄自：[linux可用内存足够为什么还用swap]([http://www.ps-aux.com/linux%E5%8F%AF%E7%94%A8%E5%86%85%E5%AD%98%E8%B6%B3%E5%A4%9F%E4%B8%BA%E4%BB%80%E4%B9%88%E8%BF%98%E4%BD%BF%E7%94%A8%E4%BA%86swap.html](http://www.ps-aux.com/linux可用内存足够为什么还使用了swap.html))

### 1.1、为什么 `buffer/cache` 会占用这么多的内存?

buffer/cache使用过高通常是程序频繁存取文件后,物理内存会很快被用光,
当程序结束后,内存不会被正常释放,而是成为 cache 状态.
通常我们不需要手工释放 swap,Linux 会自动管理.
如果非要释放,请继续看.

### 1.2、如何释放占用的 `swap` 呢?

```bash
# 将内存缓冲区数据立刻同步到磁盘
[root@localhost ~]# sync
# 关闭所有的 swap
[root@localhost ~]# swapoff -a
# 启用所有 swap
[root@localhost ~]# swapon -a
# 查看 swap 内存使用情况
## 方法1
[root@localhost ~]# free -m
## 方法2
[root@localhost ~]# swapon -s
```

### 1.3、linux 可用内存足够为什么还用 swap ?

上面可以看到服务器共有 32G 内存,其中 buff/cache 占用了 21G+.
明明还有可以将近 12G 的内存可以使用.但系统却偏偏占用完了 swap 的 8G 内存.
可知系统并没有自动释放 buff/cache 最大化利用内存.

原因:

内核参数 swappiness 的值的大小,决定着 linux 何时开始使用 swap。

- `swappiness=0` 时表示尽最大可能的使用物理内存以避免换入到 swap.
- `swappiness＝100` 时候表示最大限度使用 swap 分区，并且把内存上的数据及时的换出到 swap 空间里面.
- 此值 linux 的基本默认设置为 60，不同发行版可能略微不同.

查看命令具体如下：

```bash
[root@localhost ~]# cat /proc/sys/vm/swappiness
60
```

什么意思呢?
就是说，你的内存在使用率到 40%(100%-60%) 的时候，系统就会开始出现有交换分区的使用。
大家知道，内存的速度会比磁盘快很多，这样子会加大系统 io，同时造的成大量页的换进换出，严重影响系统的性能，所以我们在操作系统层面，要尽可能使用内存，对该参数进行调整。

#### 1.3.1、调整 Swap 在什么时候使用

**临时生效**

```bash
[root@localhost ~]# sysctl vm.swappiness=10
vm.swappiness = 10
[root@localhost ~]# cat /proc/sys/vm/swappiness
10
```

**重启依旧生效**

需要在 /etc/sysctl.conf 修改:

```bash
[root@localhost ~]# cat /etc/sysctl.conf
vm.swappiness = 10
[root@localhost ~]# sysctl -p
```

### 1.4、shared 内存

通常我们还经常看到 shared 占用大量内存,shared 表示共享内存的占用,
起决定参数的两个分别是:

```bash
# 定义单个共享内存段的最大值
kernel.shmmax = 68719476736   
# 定义共享内存页数
kernel.shmall = 4194304  ##(16G)
```

当前系统内存页大小查看:

```bash
[root@localhost ~]# getconf  PAGESIZE
4096
```

当前系统共享内存段大小(bytes):

```bash
[root@localhost ~]# cat /proc/sys/kernel/shmmax
16777216000
```

共享内存段个数查看:

```bash
[root@localhost ~]# ipcs -m
```

# curl命令详解

curl -fsSL url 链接：获取 url 链接的内容。

参数详解：

- f：连接失败时不显示 http 错误。
- s：静默模式。不输出任何东西。
- S：当有错误信息时，显示错误。
- L：当请求放回 301（重定向）状态码时，会访问新的网址。

# 查看Linux内核与版本

- uname -a

  ![1570762324882](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/1570762324882.png)

- cat /proc/version

- lsb_release -a

- cat /etc/redhat-release，这种方法只适合 Redhat 系的 Linux。

- cat /etc/issue，这种方法通用。

# 查看 Linux 有几核

查看物理 CPU 个数，命令如下：

```bash
cat /proc/cpuinfo |grep "physical id"|sort |uniq|wc -l
```

查看逻辑 CPU 个数，命令如下：

```bash
cat /proc/cpuinfo |grep "processor"|wc -l
```

查看单个 CPU 是几核，命令如下：

```bash
cat /proc/cpuinfo |grep "cores"|uniq
```

# Ubuntu 环境变量设置方法

## 1、对所有用户生效，永久的

- 在 /etc/profile 文件中添加变量

  ```bash
  # 打开文件 (以设置java环境为例)
  vi /etc/profile
  # 在文末加入 
    # 注意：1）linux用冒号“:”来分隔路径, windows用分号;来分割
  	   # 2）CLASSPATH中当前目录“.”不能丢,把当前目录丢掉也是常见的错误。
  	   # 3) export是把这变量导出为全局变量。
  	   # 4) 严格区分大小写。
  export JAVA_HOME=/usr/share/jdk1.8.0_05
  export PATH=$JAVA_HOME/bin:$PATH
  export CLASSPATH=./JAVA_HOME/lib:$JAVA_HOME/jre/lib
  # 生效
  source /etc/profile
  ```

- 在 /etc/environment 中添加变量

  ```bash
  # enviroment文件比较不同, 原文件应为PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games" 的形式
  # 直接在后面加上冒号和路径即可
  PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/etc/apache/bin"
  PRESTO="/home/zhen/software/PRESTO/presto"
  ```

## 2、对单一用户生效，永久的

- 修改 /etc/bash.bashrc

- 修改 ~/.bashrc

- 修改 ~/.profile （有时候是 ~/.bash_profile，或者 ~/.bash_login）

  ```bash
  # 与 /etc/profile 文件的添加方法相同
  ```

## 3、只对当前shell有效，临时的

- 直接运行 export 命令定义变量

  ```bash
  # 注意：＝ 即等号两边不能有任何空格
  export SCHED=/home/zhen/software/sched_11.4
  export PATH=$PATH:/etc/apache/bin
  export PATH=/etc/apache/bin:$PATH
  ```

**使得设置立即生效**

```bash
# for sys
source /etc/environment
# for all users
source /etc/profile
# for single user
source ~/.bashrc
# or logout the system.
```

**参考链接如下**：

> [Ubuntu 环境变量设置方法](http://zhaozhen.me/2017/09/15/ubuntu-evm.html)

# 关于 ubuntu 的 sources.list 总结

## 1、作用

   文件 /etc/apt/sources.list 是一个普通可编辑的文本文件，保存了ubuntu软件更新的源服务器的地址。和 sources.list 功能一样的是 /etc/apt/sources.list.d/*.list (代表一个文件名，只能由字母、数字、下划线、英文句号组成)。sources.list.d 目录下的 *.list 文件为在单独文件中写入源的地址提供了一种方式，通常用来安装第三方的软件。

```
deb http://archive.ubuntu.com/ubuntu/ trusty main restricted universe multiverse
deb http://archive.ubuntu.com/ubuntu/ trusty-security main restricted universe multiverse
deb http://archive.ubuntu.com/ubuntu/ trusty-updates main restricted universe multiverse
deb http://archive.ubuntu.com/ubuntu/ trusty-proposed main restricted universe multiverse
deb http://archive.ubuntu.com/ubuntu/ trusty-backports main restricted universe multiverse
deb-src http://archive.ubuntu.com/ubuntu/ trusty main restricted universe multiverse
deb-src http://archive.ubuntu.com/ubuntu/ trusty-security main restricted universe multiverse
deb-src http://archive.ubuntu.com/ubuntu/ trusty-updates main restricted universe multiverse
deb-src http://archive.ubuntu.com/ubuntu/ trusty-proposed main restricted universe multiverse
deb-src http://archive.ubuntu.com/ubuntu/ trusty-backports main restricted universe multiverse
```

   如上是 ubuntu 官方 sources.list 文件内容，具体地含义如下：

   每一行的开头是 deb 或者 deb-src，分别表示直接通过.deb文件进行安装和通过源文件的方式进行安装。

   deb 或者 deb-src 字段之后，是一段URL，之后是五个用空格隔开的字符串，分别对应相应的目录结构。在浏览器中输入 http://archive.ubuntu.com/ubuntu/，并进入 dists 目录，可以发现有 5 个目录和前述 sources.list 文件中的第三列字段相对应。任选其中一个目录进入，可以看到和 sources.list 后四列相对应的目录结构。

更多内容可以使用 man source.list 获得。

![img](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/051605496117149.png)![img](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/051606162679773.png)

 

## 2、源的选择

   ubuntu官方有自身的软件源，直接从官方的软件源获取数据的速度比较慢。而通过国内的一些的源的镜像进行更新一般能够获得比官方源更快的速度，不过不同国内的源的下载速度也会不一样。[这里](http://wiki.ubuntu.org.cn/源列表)给出了较为详细的ubuntu软件源列表，个人现在觉得选取ubuntu软件源的方法是首先选择位于相同地区的源，然后进行ping操作，时延不是太高即可。对比aliyun、sohu、ubuntu官方ping的数据，可以发现aliyun的源在时延上表现最好。

![img](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/051619280959698.png)

![img](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/051620001422176.png)

![img](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/051620372528304.png)

# vim编辑器缩进设置

在 /etc/vimrc文件中追加如下内容

```bash
" vim 缩进设置
set ts=4
set expandtab
set autoindent
```

ts：表示缩进空格数。

expandtab：表示按退格键时，按一次删除一个空格。

autoindent：表示开启自动缩进。

# Linux 常用软件

1、net-tools：含有 ifconfig 命令。

2、yum-utils：

# CentOS7 网络配置

1、查看网卡列表

```bash
nmcli connection show
```

结果如下：

![image-20230226173235185](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/image-20230226173235185.png)

2、配置网络参数

```bash
nmcli connection modify ens33 \
connection.autoconnect yes \ # 开机时启动
ipv4.method manual \ # 手动设定网络参数，自动是 auto
ipv4.addresses 192.168.2.3/24 \ # 设置 ip 地址
ipv4.gateway 192.168.2.2 \ # 设置 网关地址
ipv4.dns 114.114.114.114 # 设置 dns
```

3、使配置生效

```bash
nmcli connection up ens33
```

4、测试

```bash
ping www.baidu.com
```

结果如下：

![image-20230226173755986](Linux%E5%B8%B8%E7%94%A8%E5%91%BD%E4%BB%A4%E6%80%BB%E7%BB%93.assets/image-20230226173755986.png)