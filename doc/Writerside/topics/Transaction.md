# Transaction

## 存储过程
事务的执行单位是存储过程。通常情况下，应用不需要创建自己的存储过程，可以通过配置
或注解声明事务级别（TransactionLevel），然后由Zeze自动创建。

## @TransactionLevel

在定义协议的时候，配置TransactionLevel属性（默认是Serializable，表示需要事务）。
在处理函数前面加注解。注解的优先级最高，会覆盖定义协议时的配置。例子：
@Zeze.Util.TransactionLevelAnnotation(Level=Zeze.Transaction.TransactionLevel.None)

```
public enum TransactionLevel {
    None, // 不需要事务
    Serializable, // 可串行化的事务。【Default】
    AllowDirtyWhenAllRead, // 当事务没有写操作时，允许脏读。
}
```

举个事务等级的例子。两个账户初始为0，系统并发随机转账（允许结果为负数）。一个统计事务把两个账户加起
来得到Sum。当Serializable时，Sum总是为0。当AllowDirtyWhenAllRead时，Sum可能
不为0。

1. 程序默认TransactionLevel为Serializable
   优先级最低。
2. Module.DefaultTransactionLevel
   配置模块里面协议的默认事务级别。仅应用于当前模块，不包括子模块。模块默认事务级别
   一开始要计划好，设定了之后就不能改了。
3. Protocol.TransactionLevel
   协议里面配置这个协议处理时的事务级别。
4. @TransactionLevel
   协议处理函数前面加这个注解，指定处理函数的事务级别。

## TrasactionLevel的建议配置方式

上面1-4，四个地方可以配置事务级别，优先级从低到高，高优先级如果配置了会覆盖低优
先级配置。
1.	模块内大部分协议处理需要Serializable级别的事务时，除了个别用注解或协议
      级别配置覆盖，其他都不需要配置。
2.	模块内大部分协议处理不需要事务时，配置Module.DefaultTransactionLevel为
      None，个别需要的用注解或协议级别配置。

## 嵌套存储过程

当业务需要忽略部分失败，并继续执行事务时，就需要嵌套存储过程。此时需要主动创建存
储过程。创建存储过程接口为：Zeze.Application.NewProcedure。例子如下：

```
protected long ProcessMainTransaction(SomeProtocol p) {
　　// 一些处理
	if (0 != App.Zeze.NewProcedure(MyNestProcedure, “MyNestProcedure”).Call()) {
		// 一些嵌套存储过程失败的处理，此时MyNestProcedure的修改全部被回滚。
	}
	// 继续处理
}
private long MyNestProcedure() {
	if (someCondition)
		return 0; // success
	return ErrorCode(1); // fail
}
```

## Table

Table是存储过程访问数据的接口。
Table的数据结构在Solution.xml中描述。
Table就像一个Map，主要包含的方法：GetOrAdd，Get，Put，Remove。

## 乐观锁 &amp; 不会死锁

Zeze采用乐观锁，事务执行过程中不会对数据加锁，在最后提交时才加锁并检查冲突，如
果冲突了就重做事务。

## WhileCommit & WhileRollback

由于事务会重做，即事务内的所有代码都会可能被重复执行。当在事务内发送协议时，重做
导致协议可能被发送多次。
•	WhileCommit 事务成功提交时执行。
•	WhileRollback 事务失败回滚时执行。
这两个方法定义在Zeze.Transaction.Transaction中。

```
　　public void VerifyAccountSum() {
　　  var account1 = tableAccount.get(“tom”);
　　  var account2 = talbeAccount.get(“jack”);
　　  var sum = account1.value + account2.value;
　　  Transacton.WhileCommit(() => assert sum == 0);
　　  // 如果没有WhileCommit，即使在TransactionLevel为Serializable，
　　  // 这个断言也会失败。因为乐观锁执行的过程中是不加锁的。
　　}
```

## 自定义日志

可以通过Transaction.GetLog、PutLog实现自定义日志完成一些事务相关特殊操作。自定义
日志实现的Commit必须成功，否则程序会终止运行。

## 存储过程返回值

* =0: 成功
* &lt;0: Zeze内部错误码
* &gt;0: 用户自定义错误码。

编码：(Module.Id << 16) | Module.ErrorCode。Module的
基类有个辅助函数Zeze.IModule.ErrorCode(int code)用来构造这种错误码。模块级
别的错误码可以在模块定义(solutions.xml)中用枚举(enum)定义。

## 日志

Zeze记录了几乎所有的错误（异常）日志，只要出现问题，查看日志是个好办法。应用实
现时，记录自己逻辑相关的日志即可。

## 自动发送Rpc错误结果

Arch框架在处理Rpc的存储过程时，如果得到非零的返回值，会自动发送Rpc的错误结果。
异常的时候也会返回错误码。所以一般存储过程的处理流程只需要在正常的时候设置自定义
rpc的正常结果参数并调用rpc.SendResult()；错误的时候直接返回错误码。

## 分布式事务

在分布式架构中，Server有多台实例，每一台的代码是一样的。开发Server时，用户可以
简单的认为自己单独拥有后台Database的所有数据。在使用事务读写由Zeze-Table管理的
数据时，不需要任何额外的操作。数据在多台Server实例间的共享以及一致性保证由Zeze
处理。这就相当于任何Server实例上的事务都是分布的了。

## 事务重做导致的问题例子

事务在执行的过程中，如果发生了冲突，会自动重做。应用开发的时候必须注意这个特性，
否则会出现麻烦。例子如下：
* 事务中发送协议。除非不在乎协议的重复发送，可以使用直接发送协议的函数，否
则应该使用SendWhileCommit,SendWhileRollback等事务相关的版本。
* 事务中注册Timer。使用Task.schedule或者自己的Timer管理器注册Timer时，请
使用Transaction.whileCommit,whileRollback注册action完成注册操作。Zeze的
Timer会嵌入事务，自动回滚，不需要whileCommit。
* 事务中提交任务（Task）给线程池。一般也需要使用whileCommit,whileRollback。
* 事务中需要操作非Zeze管理的数据（自定义数据）。上面的Timer，Task实际上也
是操作非Zeze管理数据的一种。操作自定义数据，一般都需要使用whileCommit，
whileRollback。

## 传in，out，ref参数进存储过程说明

* 当参数传进存储过程是只读的，此时不会发生任何问题，不需要注意什么。
* 当参数是out，即存储过程执行完，通过这个参数返回处理结果，这里分为两种情况。
一，是赋值单个引用给out参数的成员变量，此时是安全的。二，通过out参数的一个
Collections形式的变量返回多个值，有两种处理方式：out集合能clear时，存储过程
执行开始的时候调用一下clear，防止由于事务重做，收集到多余的结果；out集合具
有归并能力，原来就有数据，不能clear，此时存储先收集自己的本地局部变量的集合
中，通过whileCommit合并到out集合中。
* 当参数是ref，即存储过程需要读取，同时也需要通过它返回结果。此时返回的结果需
要通过whileCommit设置到ref变量里面。如果结果是集合，先收集到本地局部变量中。

## 存储过程修改非zeze管理的数据

上面的事务重做导致的问题例子说了几个重做引起的问题。这里进一步特别说明一下，当在
存储过程中读取修改自己的数据时，此时这些数据可以隐含的看作是存储过程的参数，根据
上面in，out，ref的参数的建议形式进行处理即可。推荐的统一模式就是，1，随便读取自
己的数据；2. 计算值使用存储过程内的本地局部变量存储；3. whileCommit修改自己的数
据。读取修改自己定义的数据需要自定控制它的并发，因为存储过程是并发的。如果对自己
的数据的并发访问很复杂，最终可能会实现出另一套zeze的并发控制，所以一般情况下，
不自己定义的数据，都定义成zeze的数据，特殊情况下，并发控制比较简单，此时可以受
限的定义一些自己数据。建议让具有很高多线程开发经验，并且熟悉zeze事务的开发人员
来开发这样的模块。

## @DispatchMode

这是一个协议处理函数的注解，用来控制协议的调度方式（仅用于Java）：
1.	在普通线程池中执行。默认是这种。
2.	在重要线程池中执行。
3.	在调用者线程执行。
      协议的线程调度方式除了用这个注解单独控制。还可以在Zeze.Net.Service子类中重载
      DispatchProtocol,DispatchRpcResponse等控制。重载会覆盖默认实现，优先级比注解高。
      默认实现按注解方式调度协议的执行。

