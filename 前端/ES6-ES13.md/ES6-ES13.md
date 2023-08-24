# 目录

[TOC]



# ES6

> ECMAScript 6.0 （简称 ES6）是 JavaScript 语言下的下一代标准，于 2015 年 6 月 正式发布。它的目标是使得 JavaScript 语言可以用来编写复杂的大型应用程序，成为企业级开发语言
>
> - 1997 年：ECMAScript 1.0。
> - 1998 年：ECMAScript 2.0。
> - 1999 年：ECMAScript 3.0。
> - 2006 年：ECMAScript 4.0（未通过）。
> - 2009 年：ECMAScript 5.0。
> - 2015 年：ECMAScript 6.0。
> - 至今：版本号改用年号的形式。

## 1、let 声明变量

### 1.1、块级作用域

使用 var 来声明变量时，在代码的任何地方都可以访问，示例代码如下

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    
    <script>
        // 1、块级作用域
        if (true) {
            var i = 100;
        }
        console.log(i);
    </script>
</body>
</html>
```

结果如下：

![image-20230824222432327](ES6-ES13.assets/image-20230824222432327.png)

如果使用 let 来声明变量，就可以避免该问题，代码如下：

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    
    <script>
        // 1、块级作用域
        if (true) {
            let i = 100;
        }
        console.log(i);
    </script>
</body>
</html>
```

结果如下：

![image-20230824222628861](ES6-ES13.assets/image-20230824222628861.png)

### 1.2、不允许重复声明

示例代码如下：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <script>
        // ==================== 使用 var 可以重复声明 ==================== START
        var a = 1;
        var a = 2;
        console.log(a);
        // ==================== 使用 var 可以重复声明 ==================== END

        // ==================== 使用 let 不可以重复声明 ==================== START
        let a = 1;
        let a = 2;
        console.log(a);
        // ==================== 使用 let 不可以重复声明 ==================== END
    </script>
</body>
</html>
```

### 1.3、无变量提升

当使用 var 时，会出现变量提升的效果，示例代码如下：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <script>
        // ==================== 使用 var 变量提升 ==================== START
        console.log(name);
        var name = "ZGY";
        // ==================== 使用 var 变量提升 ==================== END
    </script>
</body>
</html>
```

结果如下：

![image-20230824230726512](ES6-ES13.assets/image-20230824230726512.png)

当使用 let 时，不会出现变量提升的效果，示例代码如下：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <script>
        // ==================== 使用 let 不存在变量提升 ==================== START
        console.log(name);
        let name = "ZGY";
        // ==================== 使用 let 不存在变量提升 ==================== END
    </script>
</body>
</html>
```

结果如下：

![image-20230824230907064](ES6-ES13.assets/image-20230824230907064.png)

### 1.4、暂存性死区

示例代码如下：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <script>
        // ==================== 暂存性死区 ==================== START
        let myname = "ZGY";
        function test() {
            console.log(myname); // 方法体和外部使用 let 定义相同名称的属性名，导致暂存性死区，即变量不可访问
            let myname = "YY";
        }
        test();
        // ==================== 暂存性死区 ==================== END
    </script>
</body>
</html>
```

结果如下：

![image-20230824231527526](ES6-ES13.assets/image-20230824231527526.png)

### 1.5、不与顶层挂钩

使用 var 声明变量，将默认和顶层的 window 对象挂钩，示例代码如下：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <script>
        // ==================== 使用 var 声明变量与顶层 window 挂钩 ==================== START
        var myname = "ZGY";
        console.log(myname, window.myname);
        // ==================== 使用 var 声明变量与顶层 window 挂钩 ==================== END
    </script>
</body>
</html>
```

结果如下：

![image-20230824231932288](ES6-ES13.assets/image-20230824231932288.png)

使用 let 声明变量，不会和顶层的 window 对象挂钩，示例代码如下：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <script>
         // ==================== 使用 let 声明变量不与顶层 window 挂钩 ==================== START
         let myname = "ZGY";
         console.log(myname, window.myname);
        // ==================== 使用 var 声明变量不与顶层 window 挂钩 ==================== END
    </script>
</body>
</html>
```

结果如下：

![image-20230824232315573](ES6-ES13.assets/image-20230824232315573.png)

## 2、const 声明常量

### 2.1、初始化必须赋值且不能再次赋值

常量必须在声明时就赋值，不能再次给常量赋值，示例代码如下：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <script>
        // ==================== 给常量再次赋值错误 ==================== START
        const myname = "ZGY";
        myname = "YY";
        // ==================== 给常量再次赋值错误 ==================== END
    </script>
</body>
</html>
```

结果如下：

![image-20230824233130308](ES6-ES13.assets/image-20230824233130308.png)

定义常量时不赋值，后续再赋值错误，示例代码如下：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <script>
        // ==================== 定义常量时不赋值，后续再赋值错误 ==================== START
        const myname;
        myname = "ZGY";
        // ==================== 定义常量时不赋值，后续再赋值错误 ==================== END
    </script>
</body>
</html>
```

结果如下：

![image-20230824233522465](ES6-ES13.assets/image-20230824233522465.png)

### 2.2、不能重复定义

示例代码如下：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <script>
        // ==================== 常量重复定义 ==================== START
        const myname = "ZGY";
        const myname = "YY";
        // ==================== 常量重复定义 ==================== END
    </script>
</body>
</html>
```

结果如下：

![image-20230824233904109](ES6-ES13.assets/image-20230824233904109.png)

### 2.3、存在块级作用域

定义在代码块中的常量，代码块外是无法访问的。示例代码如下：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <script>
        // ==================== 块级作用域以外无法访问 ==================== START
        {
            const myname = "ZGY";
        }
        console.log(myname);
        // ==================== 块级作用域以外无法访问 ==================== END
    </script>
</body>
</html>
```

结果如下：

![image-20230824234154027](ES6-ES13.assets/image-20230824234154027.png)

### 2.4、无常量提升

无法在常量声明前，进行访问。示例代码如下：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <script>
        // ==================== 无常量提升 ==================== START
        console.log(myname);
        const myname = "ZGY";
        // ==================== 无常量提升 ==================== END
    </script>
</body>
</html>
```

结果如下：

![image-20230824234625055](ES6-ES13.assets/image-20230824234625055.png)

### 2.5、暂存性死区

示例代码如下：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <script>
        // ==================== 暂存性死区 ==================== START
        const myname = "ZGY";
        function test() {
            console.log(myname); // 方法体和外部使用 const 定义相同名称的属性名，导致暂存性死区，即常量不可访问
            const myname = "YY";
        }
        test();
        // ==================== 暂存性死区 ==================== END
    </script>
</body>
</html>
```

结果如下：

![image-20230824234912323](ES6-ES13.assets/image-20230824234912323.png)

### 2.6、不与顶层对象挂钩

示例代码如下：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <script>
        // ==================== 不与顶层对象 window 挂钩 ==================== START
        const myname = "ZGY";
        console.log(myname, window.myname);
        // ==================== 不与顶层对象 window 挂钩 ==================== END
    </script>
</body>
</html>
```

结果如下：

![image-20230824235119181](ES6-ES13.assets/image-20230824235119181.png)

### 2.7、声明对象

使用 const 声明对象时，实际指向的是对象的引用，因此对象内部是属性依然可以修改，如果想要实现对象内部属性不能修改，可以使用 `Object.freeze()` 函数将对象作为入参。示例代码如下：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <script>
        // ==================== 修饰对象 ==================== START
        const user = {
            name: "ZGY",
            age: 26
        }
        console.log(user)
        user.name = "YY";
        console.log(user);

        const student = Object.freeze({
            name: "ZGY",
            age: 18
        });
        console.log(student);
        student.age = 22;
        console.log(student);
        // ==================== 修饰对象 ==================== END
    </script>
</body>
</html>
```

结果如下：

![image-20230824235850060](ES6-ES13.assets/image-20230824235850060.png)

<b style="color:red">Tips</b>：Object.freeze() 函数只对对象的第一层简单属性生效，如果对象中存在复杂属性，将会失效，解决办法是将复杂属性作为参数调用 Object.freeze() 函数做进一步封装。

## 3、变量结构赋值

## 4、模板字符串

## 5、字符串与数值扩展

## 6、数组扩展

## 7、对象扩展

## 8、函数扩展

## 9、Symbol

## 10、Iterator

## 11、Set 数据结构

## 12、Map 数据结构

## 13、Proxy

## 14、Reflect

## 15、Promise 对象

## 16、Generator 函数

## 17、Class 语法

## 18、Class 继承

## 19、Module 语法

## 20、NodeJS 中的模块化

# ES7

## 1、新特性

# ES8

## 1、async 与 await

## 2、对象方法扩展

## 3、字符串填充

# ES9

## 1、rest 与扩展运算符

## 2、正则扩展

## 3、Promise.finally

## 4、异步迭代

# ES10

## 1、Object.fromEntries

## 2、trimStart 与 trimEnd

## 3、其他新增

# ES11

## 1、Promise.allSettled

## 2、Module 新增

## 3、String 的 matchAll 方法

## 4、BigInt

## 5、顶层对象 globalThis

## 6、空值合并运算符

## 7、可选链操作符

# ES12

## 1、新增逻辑操作符

## 2、数字分隔符

## 3、字符串的 replaceAll 方法

## 4、Promise.any

## 5、WeakRefs

## 6、FinalizationRegistry

# ES13

## 1、类新增特性

## 2、最外层的 await

## 3、at 函数

## 4、正则匹配的开始和结束索引

## 5、其他新增特性