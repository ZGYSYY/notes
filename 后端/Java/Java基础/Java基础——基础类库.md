# Java基础——基础类库
## 与用户交互
- Scanner类
## 系统相关
- 获取当前操作系统的环境变量：`System.getenv()`。
- 获取当前操作系统的属性：`System.getProperties()`。
- 系统的标准输入流：`System.in`。
- 系统的标准输出流：`System.out`。
- 系统的错误输出流：`System.err`。
- 根据对象地址来获取精确的hashCode值：`System.identityHashCode(Object o)`。
## Runtime类
- 获取Runtime类实例：`Runtime.getRuntime()`。
- 获取处理器数量：`runtime.availableProcessors()`。
- 获取空闲内存数：`runtime.freeMemory()`。
- 获取总内存数：`runtime.totalMemory()`。
- 获取可用最大内存数：`runtime.maxMemory()`。
- 执行当前操作系统的命令：`runtime.exec("notepad.exe")`。
