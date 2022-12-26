# Webpack学习笔记

## nrm工具使用

简述：一个管理镜像源地址的工具。

安装：`npm i nrm -g`。

查看当前使用的镜像源地址：`nrm ls`。

切换镜像源地址：`nrm use npm`或`nrm use taobao`。

## 什么是webpack

简述：Webpack是一个前端的项目构建工具，是基于node.js开发的一个前段工具。

## Webpack安装的两种方式

1. 运行`npm i webpack -g`全局安装，这样就能在全局使用webpack命令了。
2. 在项目根目录中运行`npm i webpack --save-dev`安装到项目依赖中。

# 常见问题

1、如何快速删除 `node_modules` 文件夹？

Linux：`rm -rf node_modules`。

Windows：

​	CMD：`rd /s /q node_modules`。

​	Powershell：`rd -r node_modules`。

2、`npm install --save` 和 `--save-dev` 的区别？

`npm install packageName`：安装到项目目录下，不在 package.json 中写入依赖。

`npm install packageName -g`：全局安装，安装在Node安装目录下的 node_modules 下。

`npm install packageName --save`：安装到项目目录下，并在 package.json 文件的 dependencies 中写入依赖，简写为 `-S`。

`npm install packageName --save-dev`：安装到项目目录下，并在 package.json 文件的 devDependencies 中写入依赖，简写为 `-D`。