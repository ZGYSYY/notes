# 目录

[TOC]

# 1、使用 OpenSSL 创建证书

## 1.1、Windows 查看系统已存在证书

在 Windows 中，使用 `Win + R`，打开运行命令窗口，在窗口输入 `certmgr.msc` 打开证书管理器。

![image-20230223140616981](https.assets/image-20230223140616981.png)

## 1.2、相关概念

CA：第三方证书签名机构。

KEY：私钥文件。

CSR：证书签名请求文件，由 KEY 来生成。

CRT：证书文件，由 CSR 通过 CA 签名后生成。 

## 1.3、OpenSSL 证书生成

创建服务器私钥

```cmd
openssl genrsa -des3 -out d:/ca/server.key 1024
```

> **Tips**
>
> 参数解释如下：
>
> - genrsa：生成私钥。
>
> - -des3：生成的私钥盐的加密方式。
>
> - -out：生成文件的位置。
>
> - 1024：生成的私钥长度，可以不写，默认 2048。

由私钥创建服务器待签名证书

```cmd
openssl req -new -key d:/ca/server.key -out d:/ca/server.csr
```

![image-20230223143505040](https.assets/image-20230223143505040.png)

> **Tips**
>
> 参数解释如下：
>
> - req：发起请求。
> - -new：生成一个新的证书。
> - -key：指定私钥的位置。
> - -out：生成文件的位置。

查看服务器待签名证书内容

```cmd
openssl req -text -in d:/ca/server.csr
```

创建 CA 私钥

```
openssl genrsa -out d:/ca/myca.key
```

生成 CA 待签名证书

```
openssl req -new -key d:/ca/myca.key -out d:/ca/myca.csr
```

生成 CA 根证书

```
openssl x509 -req -in d:/ca/myca.csr -extensions v3_ca -signkey d:/ca/myca.key -out d:/ca/myca.crt
```

> **Tips**
>
> 参数解释如下：
>
> - x509：是密码学里公钥证书的格式标准。
> - -req：发起请求。
> - -extensions：证书的扩展项，这里的值是 v3_ca。
> - -signkey：指定私钥的位置。

对服务器证书签名

```cmd
openssl x509 -days 365 -req -in d:/ca/server.csr -extensions v3_req -CAkey d:/ca/myca.key -CA d:/ca/myca.crt -CAcreateserial -out d:/ca/server.crt
```

> **Tips**
>
> 参数解释如下：
>
> - -days：有效天数。
> - -in：服务器待签名证书位置。
> - -extensions：证书的扩展项，这里的值是 v3_req。
> - -CAkey：CA 私钥位置。
> - -CA：CA 证书位置。
> - -CAcreateserial：签发证书。