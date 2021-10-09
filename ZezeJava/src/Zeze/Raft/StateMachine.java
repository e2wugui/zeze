package Zeze.Raft;

import Zeze.Net.*;
import Zeze.*;

public abstract class StateMachine {
	private Raft Raft;
	public final Raft getRaft() {
		return Raft;
	}
	public final void setRaft(Raft value) {
		Raft = value;
	}

	public StateMachine() {
		AddFactory((new HeartbeatLog()).TypeId, () -> new HeartbeatLog());
	}

	private java.util.concurrent.ConcurrentHashMap<Integer, tangible.Func0Param<Log>> LogFactorys = new java.util.concurrent.ConcurrentHashMap<Integer, tangible.Func0Param<Log>>();


	// 建议在继承类的构造里面注册LogFactory。
	protected final void AddFactory(int logTypeId, tangible.Func0Param<Log> factory) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (!LogFactorys.TryAdd(logTypeId, factory)) {
			throw new RuntimeException("Duplicate Log Id");
		}
	}

	public Log LogFactory(int logTypeId) {
		TValue factory;
		tangible.OutObject<TValue> tempOut_factory = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (LogFactorys.TryGetValue(logTypeId, tempOut_factory)) {
		factory = tempOut_factory.outArgValue;
			return factory();
		}
	else {
		factory = tempOut_factory.outArgValue;
	}
		System.exit(7777);
		return null;
	}

	/** 
	 把 StateMachine 里面的数据系列化到 path 指定的文件中。
	 需要自己访问的并发特性。返回快照建立时的Raft.LogSequence.Index。
	 原子性建议伪码如下：
	 long oldFirstIndex = 0;
	 lock (Raft) // 这会阻止对 StateMachine 的写请求。
	 {
		 var lastAppliedLog = Raft.LogSequence.LastAppliedLog();
		 LastIncludedIndex = lastAppliedLog.Index;
		 LastIncludedTerm = lastAppliedLog.Term;
		 MyData.SerializeToFile(path);
		 oldFirstIndex = Raft.LogSequence.GetAndSetFirstIndex(LastIncludedIndex);
	 }
	 Raft.LogSequence.RemoveLogBeforeLastApplied(oldFirstIndex);
	
	 上面的问题是，数据很大时，SerializeToFile时间比较长，会导致服务不可用。
	 这时候需要自己优化并发。如下：
	 long oldFirstIndex = 0;
	 lock (Raft)
	 {
		 var lastAppliedLog = Raft.LogSequence.LastAppliedLog();
		 LastIncludedIndex = lastAppliedLog.Index;
		 LastIncludedTerm = lastAppliedLog.Term;
		 // 设置状态，如果限制只允许一个snapshot进行，
		 // 新进的snapshot调用返回false。
		 MyData.StartSerializeToFile();
	 }
	 MyData.ConcurrentSerializeToFile(path);
	 lock (Raft)
	 {
		 // 清理一些状态。
		 MyData.EndSerializeToFile();
		 oldFirstIndex = Raft.LogSequence.GetAndSetFirstIndex(LastIncludedIndex);
	 }
	 Raft.LogSequence.RemoveLogBeforeLastApplied(oldFirstIndex);
	
	 return true;
	
	 这样在保存数据到文件的过程中，服务可以继续进行。
	 
	 @param path
	*/
	public abstract boolean Snapshot(String path, tangible.OutObject<Long> LastIncludedIndex, tangible.OutObject<Long> LastIncludedTerm);

	/** 
	 从上一个快照中重建 StateMachine。
	 Raft 处理 InstallSnapshot 到达最后一个数据时，调用这个方法。
	 然后 Raft 会从 LastIncludedIndex 后面开始复制日志。进入正常的模式。
	 
	 @param path
	*/
	public abstract void LoadFromSnapshot(String path);
}