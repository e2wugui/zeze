# 第二十九章 Onz

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

## 开发模式

1. 开发OnzProcedure。在不同的Zeze上分别实现Onz分布式事务的自己相关逻辑并注册。
2. 实现OnzTransaction接口。完成不同Zeze上的远程调用。支持并发调用或者顺序调用。
这些OnzTransaction需要注册到OnzServer中。
3. 使用OnzServer的接口调用Onz分布式事务功能。这点在下面的运行模式介绍里面会有进
一步信息。

## 运行模式

1. OnzServer直接运行，作为独立的服务器，提供远程调用接口提供Onz分布式事务的执行。
优点是Onz功能独立了出来，跟复杂的跨服功能无关。缺点是需要额外的一次远程调用。
2. OnzServer嵌入到跨服服务器中，本地调用OnzServer的功能。
3. 以上两种运行模式的选择，建议参考跨服功能的稳定性。需求变动大的建议第一种。

## Api

### OnzProcedure(在Zeze服武器内实现)
* 两段式实现注册
```
public <A extends Bean, R extends Bean> void register(
            Application zeze,
            String name, OnzFuncProcedure<A, R> func,
            Class<A> argumentClass, Class<R> resultClass)
```
* Sage实现注册
```
public <A extends Bean, R extends Bean> void registerSaga(
            Application zeze,
            String name, OnzFuncSaga<A, R> func, OnzFuncSagaCancel funcCancel,
            Class<A> argumentClass, Class<R> resultClass)
```

### OnzTransaction(OnzServer的实现)
```
// todo 还没有完全设计好
public interface OnzTransaction {
    String getName();
    int getFlushMode();
    long perform() throws Exception;
}
```

### OnzServer
```
// todo 还没有完全设计好
public void register(OnzTransaction onzTransaction);
public Bean call(String onzTransactionName, Bean argument);
```
