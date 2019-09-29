# Spring事务管理
## 什么是Spring事务管理
Spring事务管理是SpringAOP的最佳实践之一，AOP底层用的是动态代理。当我么在类或者方法上标注注解`@Transactional`，那么就会生成一个**代理对象**。
## Spring事务管理的两种方式
1. 编码式，通过编码方式实现事务。
2. 声明式，基于AOP将具体业务逻辑
## 事务不生效情况
- 如果在**当前类**中用没有事务的方法去调用带事务的方法时，事务是不会生效的。
## Spring事务传播机制
### 动态代理
1. JDK代理，基于接口代理，凡是非public修饰，或者使用了static关键字修饰，那这些方法都不能被SpringAOP增强。
2. CGLib代理，基于子类代理，凡是类的方法使用了private、static和final关键字修饰，那这些方法不能被SpringAOP增强。
**ps:**那些不能被SpringAOP增强的方法并不意味着不能在事务环境下工作。只要它们被**外层的事务方法**调用了，由于Spring事务的传播机制，内部方法也可以工作在外部方法所启动的事务上下文中。
### Spring事务传播机制的几个级别
## 什么是BPP
### 定义
BBP的全称是BeanPostProcessor，一般称为**对象后处理器**，简单的说就是可以通过BBP对我们的对象进行**加工处理**。
### SpringBean的生命周期
1. ResourceLoader加载配置信息。
2. BeanDefintionReader解析配置信息，生成一个BeanDefintion。
3. BeanDefintion由BeanDefintionRegistry管理起来。
4. BeanFactoryPostProcessor对配置信息进行加工（也就是处理配置信息，一般通过PropertyPlaceHolderConfigurer来实现）。
5. 实例化Bean。
6. 如果该Bean配置/实现了InstantiationAwareBean，则调用对应的方法。
7. 使用BeanWarpper来完成对象之间的属性配置（依赖）。
8. 如果该Bean配置/实现了Aware接口，则调用对应的方法。
9. 如果该Bean配置了BeanPostProcessor的before方法，则调用。
10. 如果该Bean配置了init-method或者实现InstantiationBean，则调用对应的方法。
11. 如果该Bean配置了BeanPostProcessor的after方法，则调用。
12. 将对象放入到HashMap中。
13. 最后如果配置了destroy或者DisposableBean的方法，则执行销毁操作。
## Spring事务几个重要的接口
- TransactionDefinition：定义了Spring兼容的事务属性(比如事务隔离级别、事务传播、事务超时、是否只读状态)。
- TransactionStatus：代表了事务的具体运行状态(获取事务运行状态的信息，也可以通过该接口间接回滚事务等操作)。
- PlatformTransactionManager：事务管理器接口(定义了一组行为，具体实现交由不同的持久化框架来完成---类比JDBC)。
- TransactionProxyFactoryBean：生成代理对象。
- TransactionInterceptor：实现对象的拦截。
- TransactionAttrubute：事务配置的数据。
