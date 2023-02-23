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

# 2、使用 OpenSSL 创建证书并完成证书自签然后集成 SpringBoot

> **Tips**
>
> 从chrome58版本开始，使用`SAN (Subject Alternative Name)`来代替比较流行的`Common Name (CN)`，如果使用自签名的证书，你只定义了CN，则将会出现`Subject Alternative Name Missing`错误

## 2.1、创建根证书

1. 创建根证书的私钥文件

    ```
    openssl genrsa -out d:/ca/myca.key
    ```

2. 创建根证书的待签名证书文件

    ```
    openssl req -new -key d:/ca/myca.key -out d:/ca/myca.csr
    ```

3. 创建根证书的证书文件

    ```
    openssl x509 -req -in d:/ca/myca.csr -extensions v3_ca -signkey d:/ca/myca.key -out d:/ca/myca.crt
    ```

## 2.2、创建服务证书

1. 创建配置文件 `server.conf`，内容如下

    ```text
    [req]
    distinguished_name = req_distinguished_name
    req_extensions = v3_req
    
    [req_distinguished_name]
    countryName = country
    stateOrProvinceName = province
    localityName = city
    organizationName = company name
    commonName = domain name or ip
     
    [v3_req]
    subjectAltName = @alt_names
    
    [alt_names]
    DNS.1=zgysyy.xyz
    DNS.2=zgysyy.dev
    ```

2. 创建服务的私钥文件

    ```
    openssl genrsa -des3 -out d:/ca/server.key
    ```

3. 创建服务的待签名证书文件

    ```
    openssl req -new -key d:/ca/server.key -out d:/ca/server.csr -config d:/ca/server.conf
    ```

4. 创建服务的证书文件

    ```
    openssl x509 -days 365 -req -in d:/ca/server.csr -extensions v3_req --extfile d:/ca/server.conf -CAkey d:/ca/myca.key -CA d:/ca/myca.crt -CAcreateserial -out d:/ca/server.crt
    ```

## 2.3、集成 SpringBoot

1. 将服务的证书文件从 crt 转为 p12 格式

    ```
    openssl pkcs12 -export -clcerts -in d:/ca/server.crt -inkey d:/ca/server.key -out d:/ca/server.p12
    ```

2. 在 SpringBoot 项目的 `application.properties` 文件下添加如下配置

    ```
    server.port=443
    server.ssl.key-store=classpath:server.p12
    server.ssl.key-store-password=123456
    server.ssl.key-store-type=pkcs12
    ```

## 2.4、信任根证书

![image-20230223173540350](https.assets/image-20230223173540350.png)

## 2.5、验证

1. 修改本机 Hosts 文件，内容如下

    ![image-20230223173655915](https.assets/image-20230223173655915.png)

2. 打开浏览器访问验证

    ![image-20230223173823939](https.assets/image-20230223173823939.png)

    ![image-20230223173854141](https.assets/image-20230223173854141.png)