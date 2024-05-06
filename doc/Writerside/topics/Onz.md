# Onz

## 需求来源
Zeze集群内的分布式事务支持的很好，在游戏运维中，主要用来方便的实现全球同服。
但现实是游戏很可能采取分服运维方式，每个分服一个独立的Zeze集群，一个独立的
世界。在分服方式下，又存在跨服需求。这种跨Zeze集群的功能如果很重要，就需要
跨服事务支持。Onz就是用来支持跨Zeze集群的分布式事务。Onz是On Zeze的缩写。

## 应用限制
Onz分布式事务在跨服应用中，只能用来实现某些对数据完整性要求很高的功能。比如
跨服转账。跨服功能中对性能要求很高部分还是只能采取从不同Zeze集群装载数据，
在本地缓存并提供高速访问，定时或者玩家退出跨服时等不是很及时的方式保存回Zeze
集群。

## 原理和优缺点
Onz支持两段式提交模式和Saga模式两种分布式事务。

* 两段式优点是对逻辑功能几乎没有限制，对开发没有额外要求。
* 两段式缺点是同步实现，延迟较高，对相关Zeze集群的并发有一定影响。
* Saga的优点是对相关Zeze集群几乎没有影响。
* Saga的缺点是需要支持取消，对能支持的逻辑功能有一定限制。

## 保存模式
Flush是保存事务到后端数据库的Zeze叫法。

* FlushAsync

Onz分布式事务只提交到Zeze集群的缓存中，没有立即保存到后端数据库。不同步保存行为，
以后每个Zeze集群自由的选择自己的保存时间。显然，如果某个Zeze服务在保存前宕机，
那么它上面的事务修改就没有保存，会丢失，造成数据不一致。这个模式看起来不好用，
但是由于这个系统基于java，意料之外的宕机很少发生，所以也具备了一定的可用性。
对完整性要求不高，性能要求很高的功能可以采用。

* FlushImmediately

Onz分布式事务执行完成，马上保存到后端数据库。除了在执行期按两段式或者Saga模式实现
Onz分布式事务，保存到后端数据库也提供了两段式提交。最终确保后端数据库的数据是一致的。
这个等级的安全性最高，但延迟也最高。

* FlushPeriod

Onz按两段式或者Saga模式实现事务执行阶段，但没有马上保存，每个Zeze集群自由的定时发起
保存。当然保存到后端数据库也按两段式方式实现。这个模式看起来不错，既降低了延迟，数据
一致性也有保证，但有个致命的缺点，如果某个Zeze服务宕机，会导致其他健在的Zeze服务也
只能放弃自由所有的缓存数据，此时的保存行为已经缺失了数据，不可能一致了。最终导致所有
的Zeze集群不可控的被完全关联起来，谁都不允许出错。【所以，这个等级在没有好的解决方案
前不考虑支持】

## Flush退化
Onz事务在执行阶段如果发生两段式协调失败，那么所有事务回滚，不会有任何影响。事务Flush阶段
失败采取降低一致性要求的方案进行处理，即退化为FlushAsync，不再两段式协调Flush。错误处理
规则如下：

1. Zeze服务器方，它等待FlushReady超时，记录日志继续保存。相当于降级为FlushAsync模式。
2. OnzServer协调方，收不到部分Zeze服务器的FlushReady，先允许已经FlushReady的继续保存，
然后定时发起未收到FlushReady的部分Zeze服务器的全服Checkpoint，触发它的Flush流程，直到
相关Zeze服务器的数据保存成功。

以上策略就是退化，它的核心思路是尽量降低数据不一致的发生。

## 开发模式

1. 开发OnzProcedure。在不同的Zeze上分别实现Onz分布式事务的自己相关逻辑并注册。
2. 实现OnzTransaction接口。完成不同Zeze上的远程调用。支持并发调用或者顺序调用。
当需要以独立进程方式运行OnzServer时，把OnzTransaction注册到OnzServer中。

## 运行模式

1. OnzServer直接运行，作为独立的服务器，提供远程调用接口提供Onz分布式事务的执行。
优点是Onz功能独立了出来，跟复杂的跨服功能无关。缺点是需要额外的一次远程调用。
2. OnzServer嵌入到跨服服务器中，本地调用OnzServer的功能。
3. 以上两种运行模式的选择，建议参考跨服功能的稳定性。需求变动大的建议第一种。

## Api

### OnzProcedure(在Zeze服务器内实现)
* 两段式实现注册和实现接口
```
public <A extends Bean, R extends Bean> void register(
            Application zeze,
            String name, OnzFuncProcedure<A, R> func,
            Class<A> argumentClass, Class<R> resultClass)

@FunctionalInterface
public interface OnzFuncProcedure<A extends Bean, R extends Bean> {
	long call(OnzProcedure onzProcedure, A argument, R result) throws Exception;
}
```

* Sage实现注册和实现接口
```
public <A extends Bean, R extends Bean, T extends Bean> void registerSaga(
        Application zeze,
        String name, OnzFuncSaga<A, R> func, OnzFuncSagaEnd<T> funcCancel,
        Class<A> argumentClass, Class<R> resultClass, Class<T> cancelClass)
            
@FunctionalInterface
public interface OnzFuncSaga<A extends Bean, R extends Bean> {
	long call(OnzSaga sage, A argument, R result) throws Exception;
}

```

### OnzTransaction(OnzServer的实现)

```
public MyOnzTransaction extends OnzTransaction {
    @Override
    protected long perform() throws Exception {
        // 调用相关Zeze服务器的OnzProcdure。
		var future1 = super.callProcedureAsync("zeze1", "kuafu", a1, r1);
		var future2 = super.callProcedureAsync("zeze2", "kuafu", a2, r2);

        // 等待处理结果
		future1.get();
		future2.get();
    }
}
```

### OnzServer
```
/**
 * 每个zeze集群使用独立的ServiceManager实例时，使用这个方法构造OnzServer。
 * 建议按这种方式配置，便于解耦。
 * 此时zezes编码如下：
 * zeze1=zeze1.xml;zeze2=zeze2.xml;...
 * zeze1,zeze2是OnzServer自己对每个zeze集群的命名，以后用于Onz分布式事务的调用。需要唯一。
 * zeze1.xml,zeze2.xml是不同zeze集群的配置文件path。
 */
public OnzServer(String zezeConfigs, Config myConfig);

public void start() throws Exception;
public void stop() throws Exception;

/**
 * 独立进程运行OnzServer时需要注册。
 * 嵌入时不用注册。
 */
public <A extends Data, R extends Data>
    void register(Class<OnzTransaction<?, ?>> txnClass,
                  Class<A> argumentClass, Class<R> resultClass);

/**
 * 执行OnzTransaction。
 */
public long perform(OnzTransaction<?, ?> txn) {
```
