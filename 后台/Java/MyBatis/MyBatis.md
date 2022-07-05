# 目录

[TOC]

# 源码解析

## 1、架构设计

![image-20220617224537669](MyBatis.assets/image-20220617224537669.png)

我们把 MyBatis 的功能架构分为三层：

1. API 接口层：提供给外部使用的接口 API，开发人员通过这些本地 API 来操纵数据库。接口层在接收到调用请求就会调用数据处理层来完成具体的数据处理。

    MyBatis 和数据库的交互有两种方式：

    a. 使用传统的 MyBatis 提供的 API。

    b. 使用 Mapper 代理方式。

2. 数据处理层：负责具体的 SQL 查找、SQL 解析、SQL 执行和执行结果映射处理等。它主要的目的是根据调用的请求完成一次数据库操作。
3. 框架支撑层：负责最基础的功能支撑，包括连接管理、事务管理、配置加载和缓存处理，这些都是共用的东西，将它们抽取出来作为最基础的组件。为上层的数据处理层提供最基础的支撑。

## 2、主要构建及其相互关系

| 构建             | 描述                                                         |
| :--------------- | :----------------------------------------------------------- |
| SqlSession       | 作为 MyBatis 工作的主要顶层 API，表示和数据库交互的会话，完成必要数据库增、删、改、查功能。 |
| Executor         | MyBatis 执行器，是 MyBatis 的调度核心，负责 SQL 语句的生成和查询缓存的维护。 |
| StatementHandler | 封装 JDBC Statement 操作，负责对 JDBC Statement 的操作，如设置参数、将 Statement 结果集转换成 List 集合。 |
| ParameterHandler | 负责对用户传递的参数转换成 JDBC Statement 所需要的参数。     |
| ResultSetHandler | 负责将 JDBC 返回的 ResultSet 结果集对象转换成 List 类型的集合。 |
| TypeHandler      | 负责 JAVA 数据类型和 JDBC 数据类型之间的映射和转换。         |
| MappedStatement  | MappedStatement 维护了一条 <select\|update\|delete\|insert> 节点的封装。 |
| SqlSource        | 负责根据用户传递的 parameterObject，动态生成 SQL 语句，将信息封装到 BoundSql 对象中，并反回。 |
| BoundSql         | 表示动态生成的 SQL 语句以及相应的参数信息。                  |

![img](MyBatis.assets/webp)

## 3、总体流程

### 3.1、加载配置并初始化

触发条件：加载配置文件。

配置来源于两个地方，一个是配置文件（主配置文件 config.xml，mapper*.xml），一个是 JAVA 代码中的注解，将主要配置文件内容解析封装到 Configuration 类中，将 SQL 的配置信息加载成为一个 MappedStatement 对象，存储在内存之中。

### 3.2、接收调用请求

触发条件：调用 MyBatis 提供的 API。

传入参数：为 SQL 的 ID 和传入参数对象。

处理过程：将请求传递给下层的**数据处理层**进行处理。

### 3.3、处理操作请求

触发条件：API 接口层传递请求过来。

传入参数：为 SQL 的 ID 和传入参数对象。

处理过程：

1. 根据 SQL 的 ID 查找对应的 MappedStatement 对象。
2. 根据传入参数对象解析 MappedStatement 对象，得到最终要执行的 SQL 和执行传入参数。
3. 获取数据库连接，根据得到的最终 SQL 语句和执行传入参数到数据库执行，并得到执行结果。
4. 根据 MappedStatement 对象中的结果映射配置对得到的执行结果进行转换处理，并得到最终的处理结果。
5. 释放连接资源。

### 3.4、返回处理结果

将最终的处理结果返回。

## 4、源码剖析

### 4.1、传统方式源码剖析

#### 4.1.1、源码剖析-初始化

MyBatisTest.java，代码如下：

```java
// 1、读取配置文件，读成字节输入流，但是并没有进行解析
InputStream resourceAsStream = Resources.getResourceAsStream("sqlMapConfig.xml");

// 2、解析配置文件，封装 Configuration 对象，创建 DefaultSqlSessionFactory 对象
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(resourceAsStream);
```

进入源码分析 SqlSessionFactoryBuilder 类：

```java
// 1、初始化调用的 build 方法
public SqlSessionFactory build(InputStream inputStream) {
    return build(inputStream, null, null);
}

// 2、调用的重载方法
public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
    try {
        // XMLConfigBuilder 是专门解析 MyBatis 的配置文件的类
        XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
        // 这里又调用了一个重载方法，parser.parse() 的返回值是 Configuration 对象
        return build(parser.parse());
    } catch (Exception e) {
        throw ExceptionFactory.wrapException("Error building SqlSession.", e);
    } finally {
        ErrorContext.instance().reset();
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            // Intentionally ignore. Prefer previous error.
        }
    }
}
```

MyBatis 在初始化的时候，会将 MyBatis 的配置信息全部加载到内存中，使用`org.apache.ibatis.session.Configuration` 实例来维护。

##### 4.1.1.1、配置文件解析

###### 4.1.1.1.1、Configuration 对象进行介绍

Configuration 对象的结构和 xml 配置⽂件的对象⼏乎相同。

回顾⼀下 xml 中的配置标签有哪些：properties (属性)， settings (设置)， typeAliases (类型别名)， typeHandlers (类型处理器)， objectFactory (对象⼯⼚)， mappers (映射器)等。

Configuration 有对应的对象属性来封装它们也就是说，初始化配置⽂件信息的本质就是创建 Configuration 对象，将解析的 xml 数据封装到 Configuration 内部属性中。以下是 org.apache.ibatis.builder.xml.XMLConfigBuilder 类主要代码：

```java
// 解析 XML 为 Configuration 对象
public Configuration parse() {
    if (parsed) {
        // 若已解析，抛出 BuilderException 异常
        throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    // 标记已解析
    parsed = true;
    // 解析 XML 的 configuration 节点
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
}

// 解析 XML
private void parseConfiguration(XNode root) {
    try {
        // issue #117 read properties first
        // 解析 <properties/> 标签
        propertiesElement(root.evalNode("properties"));
        // 解析 <settings/> 标签
        Properties settings = settingsAsProperties(root.evalNode("settings"));
        // 加载自定义的 VFS 实现类
        loadCustomVfs(settings);
        loadCustomLogImpl(settings);
        // 解析 <typeAliases/> 标签
        typeAliasesElement(root.evalNode("typeAliases"));
        // 解析 <plugins/> 标签
        pluginElement(root.evalNode("plugins"));
        // 解析 <objectFactory/> 标签
        objectFactoryElement(root.evalNode("objectFactory"));
        // 解析 <objectWrapperFactory/> 标签
        objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
        // 解析 <reflectorFactory/> 标签
        reflectorFactoryElement(root.evalNode("reflectorFactory"));
        // 赋值 <setting/> 至 Configuration 属性
        settingsElement(settings);
        // read it after objectFactory and objectWrapperFactory issue #631
        // 解析 <environments/> 标签
        environmentsElement(root.evalNode("environments"));
        // 解析 <databaseIdProvider/> 标签
        databaseIdProviderElement(root.evalNode("databaseIdProvider"));
        // 解析 <typeHandlers/> 标签
        typeHandlerElement(root.evalNode("typeHandlers"));
        // 解析 <mappers/> 标签
        mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
        throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
}
```

###### 4.1.1.1.2、MappedStatement 对象介绍

作用：MappedStatement 与 Mapper 配置文件中的一个 select、update、insert、delete 节点相对应。Mapper 配置文件中的标签都被封装到此对象中，主要用途是描述一条 SQL 语句。

初始化过程：回顾上面介绍的加载配置文件的过程中，会对 mybatis-config.xml 中的各个标签都进行解析，其中有 mappers 标签用来引入 mapper.xml 文件或者配置 mapper 接口的目录。

比如下面的代码：

```xml
<select id="getUser" resultType="user">
	select * from user where id=#{id}
</select>
```

这样的一个 select 标签会在初始化配置文件的时候被封装成一个 MappedStatement 对象，然后存储在 Configuration 对象的 mappedStatements 属性中，mappedStatements 是个 HashMap，存储的 key = 全限定名 + 方法名，value = 对应的 MappedStatement 对象。

在 XMLConfigBuilder 类中，处理逻辑如下：

```java
private void parseConfiguration(XNode root) {
    try {
        //省略其他标签的处理
        mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
        throw new BuilderException("Error parsing SQL MapperConfiguration.Cause:" + e, e);
    }
}
```

到此对 xml 配置文件的解析就结束了，回到 4.1.1 的步骤 2 中调用的重载 build 方法，代码如下：

```java
// 3、调⽤的重载⽅法
public SqlSessionFactory build(Configuration config) {
    //创建了 DefaultSqlSessionFactory 对象，传⼊ Configuration 对象。
    return new DefaultSqlSessionFactory(config);
}
```

#### 4.1.2、源码剖析-执⾏SQL流程

先简单介绍 SqlSession ：
SqlSession 是⼀个接⼝，它有两个实现类： DefaultSqlSession (默认)和 SqlSessionManager (弃⽤，不做介绍)。
SqlSession 是 MyBatis 中⽤于和数据库交互的顶层类，通常将它与 ThreadLocal 绑定，⼀个会话使⽤⼀个 SqlSession,并且在使⽤完毕后需要 close。

```java
public class DefaultSqlSession implements SqlSession {

  private final Configuration configuration;
  private final Executor executor;
    
  // 省略其他代码  
}
```

SqlSession 中的两个最重要的参数， configuration 与初始化时的相同， Executor 为执⾏器 Executor：
Executor 也是⼀个接⼝，他有三个常⽤的实现类：

1. BatchExecutor (重⽤语句并执⾏批量更新)。
2. ReuseExecutor (重⽤预处理语句 prepared statements)。
3. SimpleExecutor (普通的执⾏器，默认)。

继续分析，初始化完毕后，我们就要执⾏ SQL 了。

继续 MyBatisTest.java，代码如下：

```java
// 3、生产了 DefaultSqlSession 实例对象
SqlSession sqlSession = sqlSessionFactory.openSession();

// 4.1、根据statementId 来从 Configuration 中的 map 集合中获取到指定的 MappedStatement 对象
// 4.2、将查询任务委派给 executor 执行器
List<Object> list = sqlSession.selectList("com.zgy.demo.mapper.UserMapper.getUserByName");
```

获得 SqlSession 对象，org.apache.ibatis.session.defaults.DefaultSqlSessionFactory.java 代码如下：

```java
@Override
public SqlSession openSession() {
    // getDefaultExecutorType() 传递的是 SimpleExecutor
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
}

// ExecutorType 为 Executor 的类型，TransactionIsolationLevel 为事务隔离级别，autoCommit 是否开启事务
private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
    Transaction tx = null;
    try {
        final Environment environment = configuration.getEnvironment();
        final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
        tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
        // 根据参数创建指定类型的 Executor
        final Executor executor = configuration.newExecutor(tx, execType);
        // 返回的是 DefaultSqlSession
        return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
        closeTransaction(tx); // may have fetched a connection so lets call close()
        throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
        ErrorContext.instance().reset();
    }
}
```

执行 SqlSession 对象的 API，org.apache.ibatis.session.defaults.DefaultSqlSession.java，代码如下：

```java
@Override
public <E> List<E> selectList(String statement) {
    return this.selectList(statement, null);
}

@Override
public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    return selectList(statement, parameter, rowBounds, Executor.NO_RESULT_HANDLER);
}

private <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    try {
        // 根据传⼊的全限定名+⽅法名从映射的 Map 中取出 MappedStatement 对象
        MappedStatement ms = configuration.getMappedStatement(statement);
        // 调⽤ Executor 中的⽅法处理
        // RowBounds 是⽤来逻辑分⻚
        // wrapCollection(parameter) 是⽤来装饰集合或者数组参数
        return executor.query(ms, wrapCollection(parameter), rowBounds, handler);
    } catch (Exception e) {
        throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
        ErrorContext.instance().reset();
    }
}
```

#### 4.1.3、源码剖析-executor

继续上面的代码，从 18 行开始，进入 executor.query() 方法，进入 org.apache.ibatis.executor.BaseExecutor.java 文件，代码如下：

```java
@Override
public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
    // 根据传⼊的参数动态获得 SQL 语句，最后返回⽤ BoundSql 对象表示
    BoundSql boundSql = ms.getBoundSql(parameter);
    // 为本次查询创建缓存的 Key
    CacheKey key = createCacheKey(ms, parameter, rowBounds, boundSql);
    return query(ms, parameter, rowBounds, resultHandler, key, boundSql);
}

@Override
public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
    ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
    if (closed) {
        throw new ExecutorException("Executor was closed.");
    }
    if (queryStack == 0 && ms.isFlushCacheRequired()) {
        clearLocalCache();
    }
    List<E> list;
    try {
        queryStack++;
        list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
        if (list != null) {
            handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
        } else {
            // 如果缓存中没有本次查找的值，那么从数据库中查询
            list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
        }
    } finally {
        queryStack--;
    }
    if (queryStack == 0) {
        for (DeferredLoad deferredLoad : deferredLoads) {
            deferredLoad.load();
        }
        // issue #601
        deferredLoads.clear();
        if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
            // issue #482
            clearLocalCache();
        }
    }
    return list;
}

private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
    List<E> list;
    localCache.putObject(key, EXECUTION_PLACEHOLDER);
    try {
        // 查询的⽅法
        list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
    } finally {
        localCache.removeObject(key);
    }
    // 将查询结果放⼊缓存
    localCache.putObject(key, list);
    if (ms.getStatementType() == StatementType.CALLABLE) {
        localOutputParameterCache.putObject(key, parameter);
    }
    return list;
}
```

继续上面的代码，从第 51 行进入 org.apache.ibatis.executor.SimpleExecutor.java 文件，代码如下：

```java
@Override
public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    Statement stmt = null;
    try {
        Configuration configuration = ms.getConfiguration();
        // 传⼊参数创建 StatementHanlder 对象来执⾏查询
        StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
        // 创建 jdbc 中的 statement 对象
        stmt = prepareStatement(handler, ms.getStatementLog());
        // StatementHandler 进⾏处理
        return handler.query(stmt, resultHandler);
    } finally {
        closeStatement(stmt);
    }
}

private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
    Statement stmt;
    // getConnection ⽅法经过重重调⽤最后会调⽤ openConnection ⽅法，从连接池中获得连接
    Connection connection = getConnection(statementLog);
    stmt = handler.prepare(connection, transaction.getTimeout());
    handler.parameterize(stmt);
    return stmt;
}
```

继续上面的代码，从 20 行进入父类 org.apache.ibatis.executor.BaseExecutor.java 文件，代码如下：

```java
protected Connection getConnection(Log statementLog) throws SQLException {
    Connection connection = transaction.getConnection();
    if (statementLog.isDebugEnabled()) {
        return ConnectionLogger.newInstance(connection, statementLog, queryStack);
    } else {
        return connection;
    }
}
```

继续上面代码，从 2 行进入 org.apache.ibatis.transaction.jdbc.JdbcTransaction.java 文件，代码如下：

```java
@Override
public Connection getConnection() throws SQLException {
    if (connection == null) {
        openConnection();
    }
    return connection;
}

protected void openConnection() throws SQLException {
    if (log.isDebugEnabled()) {
        log.debug("Opening JDBC Connection");
    }
    // 从连接池获得连接
    connection = dataSource.getConnection();
    if (level != null) {
        connection.setTransactionIsolation(level.getLevel());
    }
    setDesiredAutoCommit(autoCommit);
}
```

上述的 Executor.query() ⽅法⼏经转折，最后会创建⼀个 StatementHandler 对象，然后将必要的参数传递给 StatementHandler，使⽤ StatementHandler 来完成对数据库的查询，最终返回 List 结果集。
从上⾯的代码中我们可以看出， Executor 的功能和作⽤是：

1. 根据传递的参数，完成 SQL 语句的动态解析，⽣成 BoundSql 对象，供 StatementHandler 使⽤。
2. 为查询创建缓存，以提⾼性能。
3. 创建 JDBC 的 Statement 连接对象，传递给 StatementHandler 对象，返回 List 查询结果。

#### 4.1.4、源码剖析-StatementHandler

StatementHandler 对象主要完成两个⼯作：

1. 对于 JDBC 的 PreparedStatement 类型的对象，创建的过程中，我们使⽤的是 SQL 语句字符串会包含若⼲个 `？`占位符，我们其后再对占位符进⾏设值。 StatementHandler 通过 parameterize(statement) ⽅法对 Statement 进⾏设值。
2. StatementHandler 通过 List query(Statement statement, ResultHandler resultHandler) ⽅法来完成执⾏ Statement，和将 Statement 对象返回的 resultSet 封装成 List。

进入到 org.apache.ibatis.executor.statement.PreparedStatementHandler.java 文件，代码如下：

```java
@Override
public void parameterize(Statement statement) throws SQLException {
    // 使⽤ParameterHandler对象来完成对Statement的设值
    parameterHandler.setParameters((PreparedStatement) statement);
}
```

继续上面的代码，从 4 行进入 org.apache.ibatis.scripting.defaults.DefaultParameterHandler.java 文件，代码如下：

```java
@Override
public void setParameters(PreparedStatement ps) {
    ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    if (parameterMappings != null) {
        for (int i = 0; i < parameterMappings.size(); i++) {
            ParameterMapping parameterMapping = parameterMappings.get(i);
            if (parameterMapping.getMode() != ParameterMode.OUT) {
                Object value;
                String propertyName = parameterMapping.getProperty();
                if (boundSql.hasAdditionalParameter(propertyName)) { // issue #448 ask first for additional params
                    value = boundSql.getAdditionalParameter(propertyName);
                } else if (parameterObject == null) {
                    value = null;
                } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                    value = parameterObject;
                } else {
                    MetaObject metaObject = configuration.newMetaObject(parameterObject);
                    value = metaObject.getValue(propertyName);
                }
                // 每⼀个 Mapping 都有⼀个 TypeHandler，根据 TypeHandler 来对 preparedStatement 进⾏设置参数
                TypeHandler typeHandler = parameterMapping.getTypeHandler();
                JdbcType jdbcType = parameterMapping.getJdbcType();
                if (value == null && jdbcType == null) {
                    jdbcType = configuration.getJdbcTypeForNull();
                }
                try {
                    // 设置参数
                    typeHandler.setParameter(ps, i + 1, value, jdbcType);
                } catch (TypeException | SQLException e) {
                    throw new TypeException("Could not set parameters for mapping: " + parameterMapping + ". Cause: " + e, e);
                }
            }
        }
    }
}
```

从上述的代码可以看到 StatementHandler 的 parameterize(Statement) ⽅法调⽤了 ParameterHandler 的 setParameters(statement) ⽅法，ParameterHandler 的 setParameters(Statement ) ⽅法负责根据我们输⼊的参数，对 statement 对象的`?`占位符处进⾏赋值。
进⼊到 org.apache.ibatis.executor.statement.PreparedStatementHandler.java 文件，代码如下： 

```java
@Override
public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
    // 调⽤ preparedStatemnt 的 execute()⽅法，然后将 resultSet 交给 ResultSetHandler 处理
    PreparedStatement ps = (PreparedStatement) statement;
    ps.execute();
    
    // 使⽤ ResultHandler 来处理 ResultSet
    return resultSetHandler.handleResultSets(ps);
}
```

从上述代码我们可以看出， StatementHandler 的 List query(Statement statement, ResultHandler resultHandler) ⽅法的实现，是调⽤了 ResultSetHandler 的 handleResultSets(Statement) ⽅法。

进入 org.apache.ibatis.executor.resultset.DefaultResultSetHandler.java 文件，代码如下：

```java
@Override
public List<Object> handleResultSets(Statement stmt) throws SQLException {
    ErrorContext.instance().activity("handling results").object(mappedStatement.getId());
    
    // 多 ResultSet 的结果集合，每个 ResultSet 对应⼀个 Object 对象。⽽实际上，每个 Object 是 List<Object> 对象
    // 在不考虑存储过程的多 ResultSet 的情况，普通的查询，实际就⼀个 ResultSet，也就是说，multipleResults 最多就⼀个元素
    final List<Object> multipleResults = new ArrayList<>();

    int resultSetCount = 0;
    
    // 获得⾸个 ResultSet 对象，并封装成 ResultSetWrapper 对象
    ResultSetWrapper rsw = getFirstResultSet(stmt);

    // 获得 ResultMap 数组
    // 在不考虑存储过程的多 ResultSet 的情况，普通的查询，实际就⼀个 ResultSet，也就是说， resultMaps 就⼀个元素
    List<ResultMap> resultMaps = mappedStatement.getResultMaps();
    int resultMapCount = resultMaps.size();
    // 校验
    validateResultMapsCount(rsw, resultMapCount);
    while (rsw != null && resultMapCount > resultSetCount) {
        // 获得 ResultMap 对象
        ResultMap resultMap = resultMaps.get(resultSetCount);
        // 处理 ResultSet，将结果添加到 multipleResults 中
        handleResultSet(rsw, resultMap, multipleResults, null);
        // 获得下⼀个 ResultSet 对象，并封装成 ResultSetWrapper 对象
        rsw = getNextResultSet(stmt);
        // 清理
        cleanUpAfterHandlingResultSet();
        resultSetCount++;
    }

    // mappedStatement.resultSets 只在存储过程中使⽤，先忽略不管
    String[] resultSets = mappedStatement.getResultSets();
    if (resultSets != null) {
        while (rsw != null && resultSetCount < resultSets.length) {
            ResultMapping parentMapping = nextResultMaps.get(resultSets[resultSetCount]);
            if (parentMapping != null) {
                String nestedResultMapId = parentMapping.getNestedResultMapId();
                ResultMap resultMap = configuration.getResultMap(nestedResultMapId);
                handleResultSet(rsw, resultMap, null, parentMapping);
            }
            rsw = getNextResultSet(stmt);
            cleanUpAfterHandlingResultSet();
            resultSetCount++;
        }
    }

    // 如果是 multipleResults 单元素，则取⾸元素返回
    return collapseSingleResultList(multipleResults);
}
```

### 4.2、Mapper 代理⽅式

<span id="code-1">进入 MyBatisTest.java文件，代码如下：</span>

```java
/**
 * Mapper 代理方式
 *
 * @throws IOException
 */
public void test02() throws IOException {
    // 1、读取配置文件，读成字节输入流，但是并没有进行解析
    InputStream resourceAsStream = Resources.getResourceAsStream("sqlMapConfig.xml");

    // 2、解析配置文件，封装 Configuration 对象，创建 DefaultSqlSessionFactory 对象
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(resourceAsStream);

    // 3、生产了 DefaultSqlSession 实例对象
    SqlSession sqlSession = sqlSessionFactory.openSession();

    // 4、获得了接⼝对象，调⽤接⼝中的⽅法
    UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
    List<User> list = userMapper.getUserByName("tom");
}
```

思考⼀个问题，通常的 Mapper 接⼝我们都没有实现的⽅法却可以使⽤，是为什么呢？

答案很简单，是因为使用了动态代理技术。

先介绍⼀下 MyBatis 初始化时对接⼝的处理： MapperRegistry 是 Configuration 中的⼀个属性，它内部维护⼀个 HashMap ⽤于存放 mapper 接⼝的⼯⼚类，每个接⼝对应⼀个⼯⼚类。 mappers 中可以配置接⼝的包路径，或者某个具体的接⼝类。

UserMapper.xml 文件，代码如下：

```xml
<mappers>
    <mapper class="com.zgy.demo.mapper.UserMapper"/>
    <package name="com.zgy.demo.mapper"/>
</mappers>
```

com.zgy.demo.mapper.UserMapper 文件，代码如下：

```java
public interface UserMapper {
    /**
     * 根据用户名获取用户列表
     */
	List<User> getUserByName(String userName);
}
```

当解析 mappers 标签时，它会判断解析到的是 mapper 配置⽂件时，会再将对应配置⽂件中的增、删、改、查标签封装成 MappedStatement 对象，存⼊ mappedStatements 中。

当判断解析到接⼝时，会建此接⼝对应的 MapperProxyFactory 对象，存⼊ HashMap 中， key = 接⼝的字节码对象，value = 此接⼝对应的 MapperProxyFactory 对象。

#### 4.2.1、源码剖析-getmapper()

从 <a href="#code-1">MyBatisTest.java 文件</a>第 17 行进入 org.apache.ibatis.session.defaults.DefaultSqlSession.java 文件，代码如下：

```java
@Override
public <T> T getMapper(Class<T> type) {
    return configuration.getMapper(type, this);
}
```

继续上面的代码，从第 3 行进入 org.apache.ibatis.session.Configuration.java 文件，代码如下：

```java
public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    return mapperRegistry.getMapper(type, sqlSession);
}
```

继续上面的代码，从第 2 行进入 org.apache.ibatis.binding.MapperRegistry.java 文件，代码如下：

```java
public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    // 从 MapperRegistry 中的 HashMap 中拿 MapperProxyFactory 
    final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
    if (mapperProxyFactory == null) {
        throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
    }
    try {
        // 通过动态代理⼯⼚⽣成实例对象
        return mapperProxyFactory.newInstance(sqlSession);
    } catch (Exception e) {
        throw new BindingException("Error getting mapper instance. Cause: " + e, e);
    }
}
```

继续上面的代码，从第 9 行进入 org.apache.ibatis.binding.MapperProxyFactory.java 文件，代码如下：

```java
public T newInstance(SqlSession sqlSession) {
    // 创建了 JDK 动态代理的 Handler 类
    final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, methodCache);
    // 调⽤了重载⽅法
    return newInstance(mapperProxy);
}

protected T newInstance(MapperProxy<T> mapperProxy) {
    return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
}
```

继续上面的代码，从第 3 行进入 org.apache.ibatis.binding.MapperProxy.java 文件，代码如下：

```java
public class MapperProxy<T> implements InvocationHandler, Serializable {

    private static final long serialVersionUID = -4724728412955527868L;
    private static final int ALLOWED_MODES = MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED
        | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC;
    private static final Constructor<Lookup> lookupConstructor;
    private static final Method privateLookupInMethod;
    private final SqlSession sqlSession;
    private final Class<T> mapperInterface;
    private final Map<Method, MapperMethodInvoker> methodCache;

    // 构造函数，传⼊了 SqlSession，说明每个 session 中的代理对象的不同的
    public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethodInvoker> methodCache) {
        this.sqlSession = sqlSession;
        this.mapperInterface = mapperInterface;
        this.methodCache = methodCache;
    }

    static {
        Method privateLookupIn;
        try {
            privateLookupIn = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
        } catch (NoSuchMethodException e) {
            privateLookupIn = null;
        }
        privateLookupInMethod = privateLookupIn;

        Constructor<Lookup> lookup = null;
        if (privateLookupInMethod == null) {
            // JDK 1.8
            try {
                lookup = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                lookup.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                    "There is neither 'privateLookupIn(Class, Lookup)' nor 'Lookup(Class, int)' method in java.lang.invoke.MethodHandles.",
                    e);
            } catch (Exception e) {
                lookup = null;
            }
        }
        lookupConstructor = lookup;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, args);
            } else {
                return cachedInvoker(method).invoke(proxy, method, args, sqlSession);
            }
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
    }

    private MapperMethodInvoker cachedInvoker(Method method) throws Throwable {
        try {
            return MapUtil.computeIfAbsent(methodCache, method, m -> {
                if (m.isDefault()) {
                    try {
                        if (privateLookupInMethod == null) {
                            return new DefaultMethodInvoker(getMethodHandleJava8(method));
                        } else {
                            return new DefaultMethodInvoker(getMethodHandleJava9(method));
                        }
                    } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                             | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return new PlainMethodInvoker(new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
                }
            });
        } catch (RuntimeException re) {
            Throwable cause = re.getCause();
            throw cause == null ? re : cause;
        }
    }

    private MethodHandle getMethodHandleJava9(Method method)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Class<?> declaringClass = method.getDeclaringClass();
        return ((Lookup) privateLookupInMethod.invoke(null, declaringClass, MethodHandles.lookup())).findSpecial(
            declaringClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
            declaringClass);
    }

    private MethodHandle getMethodHandleJava8(Method method)
        throws IllegalAccessException, InstantiationException, InvocationTargetException {
        final Class<?> declaringClass = method.getDeclaringClass();
        return lookupConstructor.newInstance(declaringClass, ALLOWED_MODES).unreflectSpecial(method, declaringClass);
    }

    interface MapperMethodInvoker {
        Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable;
    }

    private static class PlainMethodInvoker implements MapperMethodInvoker {
        private final MapperMethod mapperMethod;

        public PlainMethodInvoker(MapperMethod mapperMethod) {
            super();
            this.mapperMethod = mapperMethod;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
            return mapperMethod.execute(sqlSession, args);
        }
    }

    private static class DefaultMethodInvoker implements MapperMethodInvoker {
        private final MethodHandle methodHandle;

        public DefaultMethodInvoker(MethodHandle methodHandle) {
            super();
            this.methodHandle = methodHandle;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
            return methodHandle.bindTo(proxy).invokeWithArguments(args);
        }
    }
}
```

#### 4.2.2、源码剖析-invoke()

在动态代理返回了示例后，我们就可以直接调⽤ mapper 类中的⽅法了，但代理对象调⽤⽅法，执⾏是在 MapperProxy 中的invoke ⽅法中，进入到 org.apache.ibatis.binding.MapperProxy.java 文件，代码如下：

```java
@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        } else {
            return cachedInvoker(method).invoke(proxy, method, args, sqlSession);
        }
    } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
    }
}
```
