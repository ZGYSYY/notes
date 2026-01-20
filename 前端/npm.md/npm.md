# 目录

[TOC]

# 设置国内镜像

```cmd
npm config set registry https://registry.npmmirror.com
```

# 查看配置

## 项目配置

```cmd
npm config list
```

## 全局配置

```cmd
npm config list -g
```

# 安装依赖

## 全局安装

```cmd
npm install pkg --global
# 简写
npm install pkg -g
```

## 项目安装

### 依赖拉取

```cmd
npm install
```

### 开发依赖安装

```cmd
npm install pkg --save-dev
# 简写
npm install pkg -D
# 推荐写法
npm install pkg
```

> [!NOTE]
>
> - npm 在 5.0 版本前
>     - `npm install pkg`仅下载包到 node_modules，不修改 package.json。
>     - `npm install pkg --save`下载包到 node_modules 并写入 package.json 的 dependencies。
> - npm 在 5.0 版本后
>     - `npm install pkg`和`npm install pkg --save`效果一样，下载包到 node_modules 并写入 package.json 的 dependencies。

### 生产依赖安装

```cmd
npm install pkg --save
# 简写
npm install pkg -S
```



# 卸载依赖

## 全局卸载

```cmd
npm uninstall pkg -g
# 简写
npm rm pkg -g
```

## 开发依赖卸载

```cmd
npm uninstall pkg --save-dev
# 简写
npm rm pkg -D
```

## 生产依赖卸载

```cmd
npm uninstall pgk
# 简写
npm rm pgk
```

## 依赖卸载且保留 package.json

```cmd
npm rm pkg --no-save
```

# 清除缓存

```cmd
npm cache clean --force
```

# 清理项目未使用的包

```cmd
npm prune
```

# node_moeules 快速删除

## Linux

```bash
rm -rf node_modules
```

## Windows

### CMD

```cmd
rd /s /q node_modules
```

### Powershell

```cmd
rd -r node_modules
```