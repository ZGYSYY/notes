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

### 1.8、日志记录服务修改

1. rsyslog 和 systemd-journald 服务区别

    rsyslogd 必须要开机完成并且执行了 rsyslogd 这个 daemon 之后，登录文件才会开始记录。所以，核心还得要自己产生一个 klogd 的服务， 才能将系统在开机过程、启动服务的过程中的信息记录下来。

    在有了 systemd 之后，由于这玩意儿是核心唤醒的，然后又是第一支执行的软件，它可以主动呼叫 systemd-journald 来协助记载登录文件，因此在开机过程中的所有信息，包括启动服务与服务若启动失败的情况等等，都可以直接被记录到 systemd-journald 里头去。

2. systemd-journald 配置日志持久化

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

3. rsyslog 和 systemd-journald 关系

    两者没有什么直接关系，rsyslog 保存的是文本文件，systemd-journald 保存的是二进制文件。

    两者功能存在重复，因此可以停掉一个服务，看自己愿意使用那种日志记录方式。

### 1.9、调整内核参数

新建 /etc/sysctl.d/kubernetes.conf 文件，内容如下：

```tex
net.ipv6.conf.all.disable_ipv6=1
net.bridge.bridge-nf-call-ip6tables=1
net.bridge.bridge-nf-call-iptables=1
net.ipv4.ip_forward=1
vm.swappiness=0
vm.overcommit_memory=1
vm.panic_on_oom=0
fs.inotify.max_user_instances=8192
fs.inotify.max_user_watches=1048576
fs.file-max=52706963
fs.nr_open=52706963
net.netfilter.nf_conntrack_max=2310720
```

让内核参数配置生效，使用如下命令：`sysctl -p /etc/sysctl.d/kubernetes.conf`。

### 1.10、加载相关核心模块

加载相关核心模块，命令如下：

```bash
modprobe br_netfilter
modprobe ip_vs
modprobe ip_vs_rr
modprobe ip_vs_wrr
modprobe ip_vs_sh
modprobe nf_conntrack
```

## 2、安装 K8S 集群

### 2.1、安装 Docker

1. 安装 Docker 存储驱动程序，官网推荐使用 devicemapper 驱动，需要安装 device-mapper-persistent-data 和 lvm2 这两个软件，命令如下：

    ```bash
    yum install -y device-mapper-persistent-data lvm2
    ```

2. 配置 Docker Yum 源，使用 Docker 官方的 Yum 源下载十分的慢，这里我使用国内阿里云的 Yum 源，地址是：http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo，将该文件下载下来后，将内容复制到服务器的 /etc/yum.repos.d 目录下，然后使用如下命令安装 Docker：

    ```bash
    yum install -y docker-ce
    ```

3. 启动 Docker，并设置开机自启，命令如下：

    ```bash
    systemctl start docker.service
    systemctl enable docker.service
    ```

4. 配置 Docker 的镜像仓库地址和其他配置（存储驱动程序、日志存储格式），因为默认的镜像仓库地址很慢，这里我使用的是国内网易云的镜像仓库地址，在服务器 /etc/docker 目录下新建 daemon.json 文件，内容如下：

    ```json
    {
      "registry-mirrors": [
        "https://hub-mirror.c.163.com"
      ],
      "storage-driver": "devicemapper",
      "exec-opts": ["native.cgroupdriver=systemd"],
      "log-driver": "json-file",
      "log-opts": {
        "max-size": "100m"
      }
    }
    ```

    配置说明如下：

    - registry-mirrors：镜像加速地址。
    - storage-driver：设置存储驱动程序为 devicemapper。
    - exec-opts：运行时执行选项，Docker 默认是 cgroupfs，由于 Kubernetes 默认使用的是 systemd，所以使其保持一致，来避免资源在有压力的情况时,出现不稳定的情况。
    - log-driver：设置容器的默认日志驱动程序。
    - log-opts：设置容器的日志驱动程序其他选项。

    让配置文件生效，使用如下命令：

    ```bash
    systemctl daemon-reload
    systemctl restart docker.service
    ```

    检验仓库地址是否生效，使用如下命令：

    ```bash
    docker info
    ```

    注意看 `Registry Mirrors`、`Storage Driver`、`Logging Driver`、`Cgroup Driver` 的值是否与 /etc/docker/daemon.json 所设置的值一致，如果一致，则设置成功。

5. 设置 Docker 开机自启，命令如下：

    ```bash
    systemctl enable docker
    ```

### 2.2、安装 Kubeadm

1. 由于 K8S 官网提供的 yum 源在国内几乎无法访问，因此需要配置国内的 yum 源。在 /etc/yum.repos.d 目录下，创建 kubernetes.repo 文件，内容如下：

    ```tex
    [kubernetes]
    name=Kubernetes
    baseurl=http://mirrors.aliyun.com/kubernetes/yum/repos/kubernetes-el7-x86_64
    enabled=1
    gpgcheck=1
    repo_gpgcheck=1
    gpgkey=http://mirrors.aliyun.com/kubernetes/yum/doc/yum-key.gpg http://mirrors.aliyun.com/kubernetes/yum/doc/rpm-package-key.gpg
    ```

2. 安装相关软件，命令如下：

    ```bash
    yum install -y kubeadm-1.15.1 kubectl-1.15.1 kubelet-1.15.1
    ```

3. 设置 kubelet 服务开机自启，命令如下：

    ```bash
    systemctl enable kubelet
    ```

### 2.3、初始化主节点

1. 获取 Kubeadm 初始化默认模板，将模板内容保存到 /usr/local/share/kubernates/kubeadm/kubeadm-config.yaml 文件中，命令如下：

    ```bash
    mkdir -p /usr/local/share/kubernates/kubeadm
    kubeadm config print init-defaults > kubeadm-config.yaml
    ```

2. 根据实际情况，修改 kubeadm-config.yaml 内容，修改内容如下：

    ```yaml
    apiVersion: kubeadm.k8s.io/v1beta2
    bootstrapTokens:
    - groups:
      - system:bootstrappers:kubeadm:default-node-token
      token: abcdef.0123456789abcdef
      ttl: 24h0m0s
      usages:
      - signing
      - authentication
    kind: InitConfiguration
    localAPIEndpoint:
      advertiseAddress: 192.168.2.5 # 本节点 ip 地址
      bindPort: 6443
    nodeRegistration:
      criSocket: /var/run/dockershim.sock
      name: k8s-master
      taints:
      - effect: NoSchedule
        key: node-role.kubernetes.io/master
    ---
    apiServer:
      timeoutForControlPlane: 4m0s
    apiVersion: kubeadm.k8s.io/v1beta2
    certificatesDir: /etc/kubernetes/pki
    clusterName: kubernetes
    controllerManager: {}
    dns:
      type: CoreDNS
    etcd:
      local:
        dataDir: /var/lib/etcd
    imageRepository: k8s.gcr.io
    kind: ClusterConfiguration
    kubernetesVersion: v1.15.1 # 指定 kubernetes 的版本
    networking:
      dnsDomain: cluster.local
      podSubnet: 10.244.0.0/16 # 指定 pod 的网段，为后续做全覆盖网络做准备
      serviceSubnet: 10.96.0.0/12
    scheduler: {}
    ---
    # 将默认的调度方式改为 IPVS 调度方式
    apiVersion: kubeproxy.config.k8s.io/v1alpha1
    kind: KubeProxyConfiguration
    featureGates:
      SupportIPVSProxyMode: true
    mode: ipvs
    ```

3. 初始化节点，命令如下：

    ```bash
    cd /usr/local/share/kubernates/kubeadm
    kubeadm init --config=kubeadm-config.yaml --experimental-upload-certs | tee kubeadm-init.log
    ```
