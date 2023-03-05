# 目录

[TOC]

# K8S 集群安装

## 1、环境准备

### 1.1、关闭 SELinux

修改 /etc/selinux/config 文件，将 SELINUX 的值设置为 disabled。

### 1.2、关闭防火墙

```bash
# 关闭服务
systemctl stop firewalld.service
# 关闭开机自启
systemctl disable firewalld.service
```

### 1.3、清空 iptables 规则

```bash
# 安装 iptables-services 服务
yum install -y iptables-services
# 启动服务
systemctl start firewalld.service
# 打开开机自启
systemctl enable firewalld.service
# 清空规则
iptables -F
iptables -X
iptables -Z
# 保持规则
service iptables save
```

### 1.4、关闭 Swap 功能

```bash
# 检查 swap 是否关闭
swapon -s
# 关闭所有的 swap 分区
swapoff -a
```

修改 /etc/fstab 文件，将 swap 相关配置去掉，保证下次开机，swap 不会自动挂载。

### 1.5、修改主机名

```bash
# 设置主机名为 k8s-master
hostnamectl set-hostname k8s-master
```

### 1.6、调整系统时间

```bash
# 设置系统时区为中国/上海
timedatectl set-timezone Asia/Shanghai
# 设置硬件时间为 UTC 时间，0：UTC；1：本地时间（CST）
timedatectl set-local-rtc 0
```

### 1.7、升级内核

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

    ![截屏2023-03-05 15.07.25](K8S%E7%AC%94%E8%AE%B0.assets/%E6%88%AA%E5%B1%8F2023-03-05%2015.07.25.png)

4. 安装内核

    ```bash
    yum --enablerepo=elrepo-kernel install -y kernel-lt.x86_64
    ```

5. 设置默认内核

    查看内核列表

    ```bash
    awk -F \' '$1=="menuentry " {print i++ " : " $2}' /etc/grub2.cfg
    ```

    ![image-20230305213524514](K8S%E7%AC%94%E8%AE%B0.assets/image-20230305213524514.png)

    查看当前默认启动内核

    ```bash
    grub2-editenv list
    ```

    ![image-20230305213636324](K8S%E7%AC%94%E8%AE%B0.assets/image-20230305213636324.png)

    设置默认启动内核

    ```bash
    # 设置默认启动内核
    grub2-set-default 'CentOS Linux (5.4.234-1.el7.elrepo.x86_64) 7 (Core)'
    # 查看当前默认启动内核
    grub2-editenv list
    ```

    ![image-20230305213924496](K8S%E7%AC%94%E8%AE%B0.assets/image-20230305213924496.png)

6. 重启系统验证

    ```bash
    reboot
    ```

    ![image-20230305214158146](K8S%E7%AC%94%E8%AE%B0.assets/image-20230305214158146.png)

    ```bash
    uname -a
    ```

    ![image-20230305214332015](K8S%E7%AC%94%E8%AE%B0.assets/image-20230305214332015.png)

7. 删除旧内核（可选）

    查看当前系统已安装的内核列表

    ```bash
    yum list installed |grep kernel
    ```

    ![image-20230305214734371](K8S%E7%AC%94%E8%AE%B0.assets/image-20230305214734371.png)

    卸载相关的软件包

    ```bash
    yum remove -y kernel.x86_64
    yum remove -y kernel-tools.x86_64 kernel-tools-libs.x86_64
    ```

    重启系统

    ```bash
    reboot
    ```

## 2、安装 K8S 集群

### 2.1、安装 Docker

1. 配置 Docker Yum 源，使用 Docker 官方的 Yum 源下载十分的慢，这里我使用国内阿里云的 Yum 源，地址是：http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo，将该文件下载下来后，将内容复制到服务器的 /etc/yum.repos.d 目录下，然后使用如下命令安装 Docker：

    ```bash
    yum install -y docker-ce
    ```

2. 启动 Docker，并设置开机自启，命令如下：

    ```bash
    systemctl start docker.service
    systemctl enable docker.service
    ```

    

3. 配置 Docker 的镜像仓库地址，因为默认的镜像仓库地址很慢，这里我使用的是国内网易云的镜像仓库地址，在服务器 /etc/docker 目录下新建 daemon.json 文件，内容如下：

    ```json
    {
      "registry-mirrors": [
        "https://hub-mirror.c.163.com"
      ],
      "exec-opts": ["native.cgroupdriver=systemd"],
      "log-driver": "json-file",
      "log-opts": {
        "max-size": "100m"
      }
    }
    ```

    创建 /etc/systemd/docker.service.d 目录，命令如下：

    ```bash
    mkdir -p /etc/systemd/docker.service.d
    ```

    让配置文件生效，使用如下命令：

    ```bash
    systemctl daemon-reload
    systemctl restart docker.service
    ```

    检验仓库地址是否生效，使用如下命令：

    ```bash
    docker info
    ```

    注意看 `Registry Mirrors`、 `Logging Driver`、`Cgroup Driver` 的值是否为所设置的镜像仓库地址，如果是，则设置成功。

