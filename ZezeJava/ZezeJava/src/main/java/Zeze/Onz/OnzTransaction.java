package Zeze.Onz;

import Zeze.Builtin.Onz.FlushReady;
import Zeze.Builtin.Onz.Ready;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.TaskCompletionSource;

public class OnzTransaction {
	private final OnzServer onzServer;
	private final String name;
	private final OnzFuncTransaction func;
	private final int flushMode;
	private final Bean argument;
	private final Bean result;

	public OnzTransaction(OnzServer onzServer,
						  String name, OnzFuncTransaction func,
						  int flushMode, Bean argument, Bean result) {
		this.onzServer = onzServer;
		this.name = name;
		this.func = func;
		this.flushMode = flushMode;
		this.argument = argument;
		this.result = result;
	}

	public long perform() throws Exception {
		return this.func.perform(this);
	}

	public String getName() {
		return name;
	}

	public int getFlushMode() {
		return flushMode;
	}

	public Bean getArgument() {
		return argument;
	}

	public Bean getResult() {
		return result;
	}

	// 远程调用辅助函数
	public <A extends Bean, R extends Bean> TaskCompletionSource<R>
	callProcedureAsync(AsyncSocket zezeOnzInstance,
					   String onzProcedureName, A argument, R result) {
		return onzServer.getOnzAgent().callProcedureAsync(
				this, zezeOnzInstance, onzProcedureName, argument,result, flushMode);
	}

	public <A extends Bean, R extends Bean> TaskCompletionSource<R>
	callSagaAsync(AsyncSocket zezeOnzInstance,
				  String onzProcedureName, A argument, R result) {
		return onzServer.getOnzAgent().callSagaAsync(
				this, zezeOnzInstance, onzProcedureName, argument,result, flushMode);
	}

	public void commit() {
		for (var ready : readies)
			ready.SendResult();
	}

	public void rollback() {
		for (var ready : readies)
			ready.SendResultCode(IModule.errorCode(Onz.ModuleId, Onz.eRollback));
	}

	private final long onzTid = 0; // allocate todo
	private final ConcurrentHashSet<Rpc<?, ?>> readies = new ConcurrentHashSet<>();
	private final ConcurrentHashSet<Rpc<?, ?>> flushReadies = new ConcurrentHashSet<>();
	private final ConcurrentHashSet<AsyncSocket> zezes = new ConcurrentHashSet<>(); // todo

	public long getOnzTid() {
		return onzTid;
	}

	public void trySetReady(Ready r) {
		readies.add(r);
	}

	public void trySetFlushReady(FlushReady r) {
		flushReadies.add(r);

		if (flushReadies.size() == zezes.size()) {
			// 简单的用数量判断，足够可靠了。
			for (var ready : flushReadies)
				ready.SendResult();
		}
	}
}
