# Java基础——泛型

## 一、基本概念和原理

### 1. 一个简单的泛型类

类型参数：用来指示`元素`的`类型`，使代码具有更好的`可读性`和`安全性`。

**代码片段**

```java
package com.zgy.test.local.demo2;

public class Pair<T> {
    private T first;
    private T second;

    public Pair() {
    }

    public Pair(T first, T second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public void setFirst(T first) {
        this.first = first;
    }

    public T getSecond() {
        return second;
    }

    public void setSecond(T second) {
        this.second = second;
    }
}
```



```java
package com.zgy.test.local.demo2;

/**
 * @author ZGY
 */
public class PairTest1 {
    public static void main(String[] args) {
        String[] words = {"Mary", "had", "a", "title", "lamb"};
        Pair<String> minmax = ArrayAlg.minmax(words);
        System.out.println("min = " + minmax.getFirst());
        System.out.println("max = " + minmax.getSecond());
    }
}

class ArrayAlg {
    public static Pair<String> minmax(String[] arr) {
        if (arr == null || arr.length == 0) {
            return null;
        }
        String min = arr[0];
        String max = arr[0];

        for (int i = 0; i < arr.length; i++) {
            if (min.compareTo(arr[i]) > 0) {
                min = arr[i];
            }
            if (max.compareTo(arr[i]) < 0) {
                max = arr[i];
            }
        }

        return new Pair<>(min, max);
    }
}
```



### 2. 泛型方法

泛型方法：类型变量放在修饰符和返回值之间。泛型方法可以定义在普通类中，也可以定义在泛型类中。

```java
public static <T> T getMiddle(T ... a) {
    return a[a.length / 2];
}
```



### 3. 类型变量的限定

可以使用类型变量的限定来对类型变量加以约束，使用关键字`extends`来实现类型变量的限制，`<T extends BoundingType>`表示T应该是绑定类型的子类型。

一个类型变量或通配符可以有多个限定，用`&`符号表示，例如：

```java
T extends Comparable & Serializable
```

**代码片段**

```java
package com.zgy.test.local.demo2;

public class Pair<T> {
    private T first;
    private T second;

    public Pair() {
    }

    public Pair(T first, T second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public void setFirst(T first) {
        this.first = first;
    }

    public T getSecond() {
        return second;
    }

    public void setSecond(T second) {
        this.second = second;
    }
}

```



```java
package com.zgy.test.local.demo2;

import java.time.LocalDate;

/**
 * @author ZGY
 */
public class PairTest2 {
    public static void main(String[] args) {
        LocalDate[] localDates = {
                LocalDate.of(1906, 12, 9),
                LocalDate.of(1815, 12, 10),
                LocalDate.of(1903, 12, 3),
                LocalDate.of(1910, 6, 22),
        };

        Pair<LocalDate> minmax = ArrayAlg2.minmax(localDates);
        System.out.println("min = " + minmax.getFirst());
        System.out.println("max = " + minmax.getSecond());
    }
}

class ArrayAlg2 {
    public static <T extends Comparable> Pair<T> minmax(T[] a) {
        if (a == null || a.length == 0) {
            return null;
        }
        T min = a[0];
        T max = a[0];
        for (int i = 0; i < a.length; i++) {
            if (min.compareTo(a[i]) > 0) {
                min = a[i];
            }
            if (max.compareTo(a[i]) < 0) {
                max = a[i];
            }
        }

        return new Pair<>(min, max);
    }
}
```

