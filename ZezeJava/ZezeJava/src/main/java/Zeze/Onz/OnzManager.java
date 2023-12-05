package Zeze.Onz;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Transaction.Bean;
import Zeze.Transaction.RelativeRecordSet;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.LongConcurrentHashMap;

public class OnzManager {
	private final ConcurrentHashMap<String, Procedure.Stub<?, ?>> procedureStubs = new ConcurrentHashMap<>();
	private final LongConcurrentHashMap<RelativeRecordSet> onzRrs = new LongConcurrentHashMap<>();

	public OnzManager() {
		// 网络注册。
	}

	public <A extends Bean, R extends Bean> void register(
			Application zeze,
			String name, FuncRemote<A, R> func,
			Class<A> argumentClass, Class<R> resultClass) {

		if (null != procedureStubs.putIfAbsent(name, new Procedure.Stub<>(zeze, name, func, argumentClass, resultClass)))
			throw new RuntimeException("duplicate Onz Procedure Name=" + name);
	}

	public void markRrs(RelativeRecordSet rrs, Procedure onzProcedure) {
		if (null != onzProcedure) {
			rrs.addOnzProcedures(onzProcedure);
			// todo 这里标记并记录rrs相关信息，
			//  用来在任意一个zeze集群发起rrs.flush的时候，
			//  能通知到相关onz，onz能追溯到这个onz事务相关的其他zeze集群，
			//  最终让其他相关的zeze集群也同步开始flush。
			//  因为不同的zeze集群的rrs.flush时机是没有统一控制的。
			//  这个追溯流程有点复杂，看看实现上有没有更好的方法。
			// todo 这里的代码随便写的。
			for (var onz : rrs.getOnzProcedures()) {
				onzRrs.put(onz.getOnzTid(), rrs);
			}
		}
	}

	// 网络服务
	// 1. 得到远程Onz的调用，解析出参数，查找并调用Procedure，最终完成本地Zeze的事务提交。
	// 2. 提供协调rrs的支持接口给本地rrs保存时用。

}
