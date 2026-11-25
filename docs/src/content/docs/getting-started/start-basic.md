---
title: "基础大全"
sidebar:
  order: 11
---

【禁止ai修改本文档】
【只讲必要的信息，尽量不扩展】

## 首先定义数据结构
```
  <module name="Role" id="1">
    <!-- 定义数据结构 -->
    <bean name="BRole">
      <variable id="1" name="Level" type="int"/>
      <variable id="2" name="Experience" type="long"/>
    </bean>
    <!-- 定义表 -->
    <table name="tRole" key="long" value="BRole"/>
    <!-- 定义协议 -->
    <rpc name="AddExperience" argument="BRole" handle="server"/>
  </module>
```
## 生成代码编写业务逻辑
生成代码包括模块框架，只有处理函数ProcessAddExperienceRequest里面需要自己
填写业务逻辑。业务代码例子如下：
```
public final class ModuleRole extends AbstractModule {
    @Override
    protected long ProcessAddExperienceRequest(AddExperience rpc) {
        // 在这里访问table编写业务逻辑，除了这里的代码，外面的框架都是生成的。
        var session = ProviderUserSession.get(rpc);
        var roleId = session.getRoleId();
        var newExperience = rpc.Argument.getExperience();
        // 从表中读取角色数据，不存在则自动创建
        var role = _tRole.getOrAdd(roleId);
        // 累加经验
        role.setExperience(role.getExperience() + newExperience);
        // 发送rpc结果
        session.respond(rpc);
        return Procedure.Success;
    }
}
```

至此，zeze开发核心步骤完成。总结为一句话：定义数据，生成框架代码，编写业务逻辑。

## Process的返回值
函数返回0（Procedure.Success）表示成功，其他是错误码。返回值类型是long，小于0
的返回值内部保留；大于0的返回值用户自定义。用户返回值按
"moduleId<<32+ModuleErrorCode"编码。编码辅助函数在IModule中，Module是子类
可以方便访问到。例子：return errorCode(eMyModuleErrorCode);

## 事务
上一节中的ProcessAddExperienceRequest协议处理函数一进来就在一个事务中，所有的
处理直到函数退出事务完成。中间发生异常或者返回非0的错误码，所有的修改会回滚，避免
破坏数据。有了事务，就有了事务划分的问题。
* 最小事务划分
  事务的划分由需求决定。这是一句漂亮话，存在难点。懂业务的人通常也划分得不好事务
  的。只能作为某些事务划分出现争议时兜底。完美的事务划分是全部事务都是最小的。
* 可操作的事务划分
  1. 一个协议当作一个事务。自然干净，避免去思考事务划分，能解决大部分的划分问题。
  2. 当处理中出现遍历操作。比如遍历一张表，或遍历一个大小不可控的集合时，需要问问
  自己，这所有的处理是否要求要在一个事务内完成。可能的话，拆成每一项一个事务进行
  处理，避免一个事务里面访问太多数据。这一点需要特别关注，使用ai编写逻辑时，也需
  要让ai明白这一点。
* 嵌套事务
  事务处理中需要忽略某一部分失败，进行局部回滚，此时使用嵌套事务。

## Bean不能太大
Bean里面可以定义容器，要小心塞入太多数据。一般建议数量百级别，上限到千。如果需要
无限容量的容器，请使用Zeze.Collections.*下面定义的容器。

## Transaction.whileCommit
zeze事务在冲突时会重做(redo)，此时事务内如果有发送协议，就会发送多次。为了解决这
个问题，事务提供辅助函数whileCommit(action)，它的含义是在事务成功提交时调用
action。常见的，除了发送协议，还有记录日志，但是日志具有实时性，也允许重复，一般
不使用whileCommit，除了一些记账性质的日志需要用。

## TaskSpec
这个辅助类用来启动一个任务。例子：TaskSpec.ofAction(()->{}).run();
1. ofAction,ofFunc,ofProcedure... 按类型指定执行动作。
2. call,run,runNow,schedule,scheduleNow,executeOneByOne(key)... 任务启动方式。
3. TaskSpec.ofAction(()->{}).dispatchMode(mode)...run(); 创建和启动之间可以设置参数。 
*. 注意，上面带Now后缀的启动方式立即执行，其他的看是否在事务中，如果在事务中，
自动使用whileCommit。

## OnlineSpec
Game.Online是玩家角色在线管理器。OnlineSpec是给玩家发送协议的辅助类。
例子：OnlineSpec.ofRole(roleId).send();
1. ofRole,ofRoles,ofAllOnline,... 指定发送对象。
2. send,sendNow,... 发送方法。后缀Now的立即发送，其他的事务自适应：在事务
场景下自动whileCommit。

## Timer
Timer是个持久化定时器实现，会被调度到某一个GameServer执行，如果这个
GameServer宕机，其他GameServer会接管。由于GameServer是多进程的，
ScheduledThreadPool的本地timer有时不合适，需要Zeze-Timer。
例子：
Timer.schedule(TimerSpec.ofDelay(delay)..., handle, customData)
Timer.roles(roleId).online()...schedule();
