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
生成代码包括模块框架，只有ProcessAddExperienceRequest里面需要自己填写业务逻辑。
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
模块是包含数据和处理接口的集中营，模块之间可以通过函数互相调用，实现复杂业务。
本质上，所有的表在任何process过程中都是可达的，所以这个结构就能实现所有业务逻辑了。

## Process的返回值
函数返回0（Procedure.Success）表示成功，其他表示错误码。错误码类型是long，小于0的返回值内部保留；大于0的返回值用户自定义。
用户返回值按"moduleId<<32+moduleErrorCode"编码。编码辅助函数在IModule中，Module是子类可以方便访问到。
例子如：return errorCode(eMyModuleErrorCode);

## 事务
上一节中的ProcessAddExperienceRequest协议处理函数一进来就在一个事务中，所有的处理直到函数退出事务完成。
中间发生异常或者返回非0的错误码，所有的修改会回滚，避免破坏数据。有了事务，就有了事务划分的问题。
* 最小事务划分
  事务的划分由需求决定。这是一句漂亮话，存在难点。懂业务的人通常也划分得不好事务的。
  只能作为某些事务划分出现争议时兜底。完美的事务划分是全部事务都是最小的。
* 可操作的事务划分
  1. 一个协议当作一个事务。自然干净，避免去思考事务划分，能解决大部分的划分问题。
  2. 当处理中出现遍历操作。比如遍历一张表，遍历一个较大或大小不可控的集合时，问问自己，
  这所有的事项是否要求一定要在一个事务内处理。可能的话，拆成每一项一个事务进行处理，
  避免一个事务里面访问太多数据。
* 嵌套事务
  事务处理中需要忽略某一部分失败，进行局部回滚，此时使用嵌套事务。

## Bean不能太大
Bean里面可以定义容器，要小心塞入太多数据。一般建议百级别，上限到千。如果需要无限容器，去使用Zeze.Collections.*下面定义的容器。

