<center><h1><b>containerd-nerdctl 详解</b></h1></center>

# 目录

[TOC]

# 安装 nerdctl

访问 github 地址：https://github.com/containerd/nerdctl/releases 获取安装包

```bash
# 解压
tar Cxzvvf /usr/local/bin nerdctl-2.1.3-linux-amd64.tar.gz
# 验证（如果无效，可以退出会话从新进入再试）
nerdctl --version
```

# 配置镜像加速

1. 配置初始化

```bash
containerd config default > /etc/containerd/config.toml
```

`/etc/containerd/config.toml` 添加配置，内容如下：

```toml
[plugins."io.containerd.grpc.v1.cri".registry]
      config_path = "/etc/containerd/certs.d"
```

2. 创建 `/etc/containerd/certs.d/docker.io/hosts.toml` 文件，内容如下：

```toml
server = "https://docker.io"

[host."https://镜像地址"]
  capabilities = ["pull", "resolve"]
  skip_verify = true
```

3. 创建 `/etc/containerd/certs.d/registry-1.docker.io/hosts.toml` 文件，内容如下：

```toml
server = "https://registry-1.docker.io"

[host."https://镜像地址"]
  capabilities = ["pull", "resolve"]
  skip_verify = true
```

4. 重启 containerd 服务

```bash
systemctl restart containerd.service
```

5. 验证

```bash
nerdctl pull busybox:latest
```

# 常用命令

## 镜像类

| 命令                            | 说明               | 常用参数及示例                                               |
| ------------------------------- | ------------------ | ------------------------------------------------------------ |
| `nerdctl pull <镜像>`           | 从远程仓库拉取镜像 | `nerdctl pull nginx:latest`                                  |
| `nerdctl images`                | 列出本地所有镜像   | `nerdctl images`，带参数：`nerdctl images --digests`显示摘要 |
| `nerdctl inspect <镜像>`        | 查看镜像详细信息   | `nerdctl inspect nginx:latest`                               |
| `nerdctl tag <源镜像> <新标签>` | 给镜像打标签       | `nerdctl tag nginx:latest mynginx:v1`                        |
| `nerdctl rmi <镜像>`            | 删除本地镜像       | `nerdctl rmi nginx:latest`                                   |
| `nerdctl push <镜像>`           | 推送镜像到远程仓库 | `nerdctl push myregistry/myimage:tag`                        |

示例如下：

```bash
# 拉取镜像
nerdctl pull busybox:latest

# 列出镜像，显示详细摘要
nerdctl images --digests

# 给本地镜像打标签
nerdctl tag busybox:latest mybusybox:1.0

# 推送镜像（需要登录）
nerdctl login myregistry.com
nerdctl push myregistry.com/mybusybox:1.0

# 删除镜像
nerdctl rmi mybusybox:1.0

# 在当前目录有 Dockerfile 时构建镜像
nerdctl build -t myapp:latest .

# 关闭缓存，显示详细日志
nerdctl build --no-cache --progress=plain -t myapp:latest .

# 使用远程 Dockerfile
nerdctl build -f https://example.com/Dockerfile -t remoteapp .
```

## 网路类

```bash
# 列出网络
nerdctl network ls

# 创建网络
nerdctl network create mynet

# 删除网路
nerdctl network rm mynet
```

## 数据卷类

```bash
# 创建卷
nerdctl volume create myvol

# 列出卷
nerdctl volume ls

# 删除卷
nerdctl volume rm myvol
```

## 容器类

| 命令                                     | 说明                   | 常用参数示例                                  |
| ---------------------------------------- | ---------------------- | --------------------------------------------- |
| `nerdctl run [OPTIONS] IMAGE [COMMAND]`  | 创建并启动容器         | `nerdctl run -it --rm busybox sh`             |
| `nerdctl ps [OPTIONS]`                   | 列出运行中容器         | `nerdctl ps`，全部容器：`nerdctl ps -a`       |
| `nerdctl stop <CONTAINER>`               | 停止运行中的容器       | `nerdctl stop mycontainer`                    |
| `nerdctl rm <CONTAINER>`                 | 删除容器（需要先停止） | `nerdctl rm mycontainer`                      |
| `nerdctl logs [OPTIONS] <CONTAINER>`     | 查看容器日志           | `nerdctl logs -f mycontainer`（持续输出日志） |
| `nerdctl exec [OPTIONS] <CONTAINER> CMD` | 在运行容器内执行命令   | `nerdctl exec -it mycontainer /bin/sh`        |
| `nerdctl inspect <CONTAINER>`            | 查看容器详细信息       | `nerdctl inspect mycontainer`                 |
| `nerdctl pause <CONTAINER>`              | 暂停容器               | `nerdctl pause mycontainer`                   |
| `nerdctl unpause <CONTAINER>`            | 恢复暂停容器           | `nerdctl unpause mycontainer`                 |

常用参数说明：

| 参数                    | 说明               | 备注                          |
| ----------------------- | ------------------ | ----------------------------- |
| `-i` 或 `--interactive` | 保持标准输入打开   | 交互式容器常用                |
| `-t` 或 `--tty`         | 分配伪终端         | 配合 -i 使用实现终端交互      |
| `--rm`                  | 容器退出时自动删除 | 调试时常用                    |
| `-d` 或 `--detach`      | 后台运行容器       | 适合守护进程运行容器          |
| `--name`                | 给容器命名         | 容器操作时更方便引用          |
| `-p` 或 `--publish`     | 端口映射           | 格式 `主机端口:容器端口`      |
| `-v` 或 `--volume`      | 挂载卷或目录       | 格式 `主机路径:容器路径[:ro]` |
| `--env` 或 `-e`         | 设置环境变量       | `-e VAR=value`                |

示例如下：

```bash
# 交互式运行一个 BusyBox 容器，退出时自动删除
nerdctl run -it --rm busybox sh

# 后台运行 nginx，命名为 mynginx，映射80端口到宿主机8080
nerdctl run -d --name mynginx -p 8080:80 nginx

# 使用自定义网络启动容器
nerdctl run -d --net mynet --name myapp nginx

# 挂载卷启动容器
nerdctl run -d -v myvol:/data --name data-nginx nginx

# 查看正在运行的容器
nerdctl ps

# 查看所有容器（包括停止的）
nerdctl ps -a

# 查看容器日志，并持续输出
nerdctl logs -f mynginx

# 进入正在运行的容器的 shell
nerdctl exec -it mynginx /bin/bash

# 停止容器
nerdctl stop mynginx

# 删除容器
nerdctl rm mynginx

# 查看容器详细信息
nerdctl inspect <container>

# 查看容器资源使用情况
nerdctl stats
```