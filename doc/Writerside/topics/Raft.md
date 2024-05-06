# Raft

## 什么是Raft
请阅读 zeze/Zeze/Raft/下的Raft.mhtml，raft.pdf，OngaroPhD.pdf。
这里不再重复。

## Zeze.Raft.StateMachine
应用数据定义在这里。

## Zeze.Raft.Log
定义应用数据（StateMachine）修改操作。
每次操作对应一个Log子类。

## Zeze.Raft.Raft.AppendLog
把修改操作日志（Log）添加到Raft日志队列中。
当AppendLog方法返回时，表示操作已经被成功处理。

## Zeze.Raft.Agent
应用的客户端管理类。
自动切换Raft-Leader变更，自动重发请求。

## Zeze.Raft.RaftRpc
自定义应用访问协议基类。
当协议用zeze生成的时候，在协议定义里面指名 base="Zeze.Raft.RaftRpc"。
建议每个RaftRpc对应一条Log。
* ResultCode==RaftApplied：Raft发现请求是重发的，但是已经 成功处理过。
* ResultCode==RaftExpired：请求过期了，无法判断是否被成功 处理。

## 例子
```
// 注意，以下例子没有处理多线程问题。
// 应用数据
class MyStateMachine : Zeze.Raft.StateMachine
{
    public long Count;
    // 操作日志
    class AddCount : Zeze.Raft.Log
    {
        public AddCount(IRaftRpc req) : base(req)
        {
        }           
        public override void Apply(RaftLog holder, StateMachine stateMachine)
        {
            (stateMachine as MyStateMachine).Count += 1;
        }
    }

	// 应用操作接口。这里传入的是下面定义的AddCount网络协议。
	public void AddCount(IRaftRpc request)
	{
	    Raft.AppendLog(new AddCount(request));
	}

	// 需要实现的接口
	// 参考Zeze.Raft.Test.TestStateMachine
    public override void LoadSnapshot(string path)
	{
	}
    
    public override bool Snapshot(string path, out long LastIncludedIndex,
        out long LastIncludedTerm)
    {
    }

	public MyAppStateMachine()
	{
	    // 注册Log工厂。
	    AddFactory(new AddCount(null).TypeId, () => new AddCount(null));
	}
}

// 增加Count计数的协议
public sealed class AddCount : RaftRpc<EmptyBean, EmptyBean>
{
    public readonly static int ProtocolId_ = Bean.Hash32(typeof(AddCount).FullName);
    public override int ModuleId => 0;
    public override int ProtocolId => ProtocolId_;
}

    // 服务器创建Raft实例
    var configFileName = "raft.xml";
    var config = Zeze.Raft.RaftConfig.Load(configFileName);
    var nodeName = config.Name; // 所有的raft-node共享一个配置文件时，
    // 需要通过参数指定启动的node名字。

    var stateMachine = new MyAppStateMachine();
    var raft = new Raft(stateMachine, nodeName, config);

    // 服务器协议处理
    long ProcessAddCountRequest(Protocol p)
    {
	var r = p as AddCount;
	stateMachine.AddCount(r);
    }

    // 客户端创建Agent
    var agent = new Agent("MyRaftApp.Agent", config);
    agent.Client.AddFactoryHandle(new AddCount().TypeId,
        new Net.Service.ProtocolFactoryHandle()
    {
        Factory = () => new AddCount(),
    });

    // 客户端发送网络请求
    var req = new AddCount();
    agent.SendForWait(req).Task.Wait();
    if (req.Result.ResultCode == )
    ...
```
