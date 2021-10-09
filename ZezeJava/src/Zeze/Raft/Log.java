package Zeze.Raft;

import Zeze.Serialize.*;
import Zeze.Transaction.*;
import RocksDbSharp.*;
import Zeze.Net.*;
import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;

/** 
 用户接口。
*/
public abstract class Log implements Serializable {
	/** 
	 用于系列化，外部使用，实现类不用 Decode、Encode 这个变量。
	 只读，常量即可。
	 在一个StateMachine实现中唯一。
	 不冲突的时候使用默认实现即可。
	 【注意】
	 如果实现类的FullName发生了改变，需要更新所有的Raft-Node。
	 如果不想跟名字相关，重载并提供一个编号。
	*/
	private final int _TypeId;
	public int getTypeId() {
		return _TypeId;
	}

	// 当前这个Log是哪个应用的Rpc请求引起的。
	// 【Raft用来检测重复的请求】。
	// RaftConfig里面配置AutoKeyLocalStep开启这个功能。
	// 启用这个功能要求应用的RpcSessionId持久化，并且全局唯一，对每个AutoKeyLocalStep递增。
	// 【注意】应用生成的Id必须大于0；0保留给内部；小于0未使用。
	private long UniqueRequestId;
	public final long getUniqueRequestId() {
		return UniqueRequestId;
	}
	public final void setUniqueRequestId(long value) {
		UniqueRequestId = value;
	}
	private String AppInstance;
	public final String getAppInstance() {
		return AppInstance;
	}
	public final void setAppInstance(String value) {
		AppInstance = value;
	}

	public Log(String appInstance, long requestId) {
		setAppInstance(appInstance);
		setUniqueRequestId(requestId);
		_TypeId = (int)Bean.Hash32(this.getClass().getName());
	}

	/** 
	 最主要的实现接口。
	 
	 @param stateMachine
	*/
	public abstract void Apply(StateMachine stateMachine);

	public void Decode(ByteBuffer bb) {
		setUniqueRequestId(bb.ReadLong());
		setAppInstance(bb.ReadString());
	}

	public void Encode(ByteBuffer bb) {
		bb.WriteLong(getUniqueRequestId());
		bb.WriteString(getAppInstance());
	}
}