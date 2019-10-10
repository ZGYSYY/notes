<center><h1>Docker常用命令笔记</h1></center>
# 使用Dockerfile创建镜像

# Docker管理Volume

## 查看所有的volume

```bash
docker volume ls
# 显示所有的volume名称列表
docker volume ls -qf dangling=true
```

## 删除volume

```bash
# 删除指定name的volume
docker volume rm name名称
# 删除所有的volume
docker volume rm $(docker volume ls -qf dangling=true)
```

