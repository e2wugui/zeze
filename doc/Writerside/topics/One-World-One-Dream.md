# 全球同服

## 单点模块
虽然Zeze提供了分布式能力，但有时系统内有一些全局单点模块，不能提供足够的并发能
力。举个例子，存在一种游戏内的即时排行榜，角色数值变化马上更新排行榜。由于这个排
行榜只包含一个数据列表，所有的更新请求需要排队互斥进入列表。当同时在线角色数量很
大，更新非常多，那么这个排行榜就会成为一个并发瓶颈。如果全局单点的数据可以按一定
规则分开存储，并在需要的时候汇总。能这样做的数据仍然可以用很小的代价并提供足够的
并发性能。比如上面的排行榜，分成128个分组数据，把角色hash分散到这些分组中。每
个分组独立进行排名，每个分组都保存足够数量的排名。当需要全局排名时，把所有的分组
整合起来就能得到最终的排名。这样排行榜的并发就增加了128倍。

## 分组数量（ConcurrentLevelSource）
分组数量决定了最大的并发度。一般来说设置足够大，并留有一定余地即可。比如128。嗯，
这个数字比较漂亮。分组数量一般来说不好随便改。比如对于排行榜来说，修改这个参数，
对导致分组数据全部失效（需要作废掉重来）。

## 负载分配
为了提高数据的Cache命中，访问同一个分组的请求需要转发到同一台服务器执行。分组
数量是固定的。但服务器数量开始一般小于分组数量。RedirectHash会根据hash把负载分
配到实际服务器中。每个服务器可能处理多个分组。当然分组数量决定了最大服务器数量。

## 相关Api
@RedirectHash

## 大量共享模块的优化
游戏内的帮派，即时通讯里面的群等拥有一定量成员列表的模块属于这种类型。帮派（群）
本身是自然分布的。但成员可能登录在多台Server上，实际上可能所有的Server都有零星
几个登录，当他需要访问成员列表时，就会把成员列表缓存到本Server，此时只是多占用内
存。此时成员列表发生了修改，就会作废所有Server上的缓存，性能表现出一定的突发特
性。对于成熟的帮派（群），每天的修改次数只有几个，总体性能不会明显造成问题。但是
我们如果精益求精，也是有办法解决这个问题的。只需要把群的所有操作发送给同一台
Server处理，就避免了大量共享问题。

### 实现
把群操作定向到同一台Server，由Arch.Linkd完成。Linkd拦截所有的群操作，并根据群编
号哈希定向到同一台Server即可。
* Linkd::LinkdService重载dispatchUnknownProtocol：

```
switch (moduleId) {
    case ModuleFriend.ModuleId:
    switch (protocolId) {
    case CreateGroup.ProtocolId_:
        // 创建群，随机找一台服务器。
        if (ChoiceHashSend(Random.getInstance().nextInt(), moduleId, dispatch))
            return; // 失败尝试继续走默认流程?
        break;
    case GetGroupMemberNode.ProtocolId_:
        ... 其他所有的群操作
        if (ChoiceHashSend(DecodeGroupIdHash(data), moduleId, dispatch))
            return; // 失败尝试继续走默认流程?
        break;
    }
    break;
}
```

* DecodeGroupIdHash

```
private static int DecodeGroupIdHash(Zeze.Serialize.ByteBuffer bb) {
    var rpc = new RpcGroupId();
    rpc.decode(bb);
    return rpc.Argument.hashCode();
}

public static class GroupId extends Zeze.Transaction.Bean {
    public String Group;

    @Override
    public void encode(ByteBuffer bb) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void decode(ByteBuffer bb) {
　　	    // 所有的群操作的参数的第一个变量必须时Group，
    　　	// 并且variable.id必须等于1.
    　　 // 这个类的目的是优化。
    　　 // 因为Lindk只需要得到Group即可，不需要解析出完整的参数信息。
    　　 // 这个decode的实现必须符合Bean的编码规范。
    　　 // 如果不在乎decode完整协议的性能开销，也可以不需要这个类。
    　　 // 直接decode出完整协议即可。
        int _t_ = bb.ReadByte();
        int _i_ = bb.ReadTagSize(_t_);
        if (_i_ == 1) {
        Group = bb.ReadString(_t_);
        _i_ += bb.ReadTagSize(_t_ = bb.ReadByte());
        }
        // 由于Group,DepartmentId默认值时，不会Encode任何东西，这里就不做是否
        存在值的验证了。
    }

    @Override
    public int hashCode() {
        final int _prime_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _prime_ + Group.hashCode();
        return _h_;
    }
}
```
* ChoiceHashSend

这个方法实际上是Linkd选择负载的正常包装。也列出来吧。
```
private boolean ChoiceHashSend(int hash, int moduleId, Dispatch dispatch) {
    var provider = new OutLong();
    if (linkdApp.linkdProvider.choiceHashWithoutBind(moduleId, hash, provider)) {
        var providerSocket = linkdApp.linkdProviderService.GetSocket(provider.value);
        if (null != providerSocket) {
            // ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
            return providerSocket.Send(dispatch);
        }
    }
    return false;
}
```
