package Zeze.Onz;

import java.util.ArrayList;
import java.util.Set;
import Zeze.Application;
import Zeze.Builtin.Onz.FlushReady;
import Zeze.Builtin.Onz.Ready;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;
import Zeze.Util.TaskCompletionSource;

public class OnzProcedure implements FuncLong {
	private final long onzTid;
	private final OnzProcedureStub<?, ?> stub;
	private final Bean argument;
	private final Bean result;
	private final int flushMode;
	private final AsyncSocket onzServer;

	public OnzProcedure(AsyncSocket onzServer,
						long onzTid, int flushMode,
						OnzProcedureStub<?, ?> stub, Bean argument, Bean result) {
		this.onzServer = onzServer;
		this.onzTid = onzTid;
		this.stub = stub;
		this.argument = argument;
		this.result = result;
		this.flushMode = flushMode;
	}

	public int getFlushMode() {
		return flushMode;
	}

	public long getOnzTid() {
		return onzTid;
	}

	public OnzProcedureStub<?, ?> getStub() {
		return stub;
	}

	public Bean getArgument() {
		return argument;
	}

	public Bean getResult() {
		return result;
	}

	public boolean isEnd() {
		return true;
	}

	@Override
	public long call() throws Exception {
		// 这里实际上需要侵入Zeze.Transaction，在锁定，时戳检查完成后，
		// 发送result给调用者，完成ready状态，
		// Zeze.Transaction 需要同步进行等待。

		var txn = Transaction.getCurrent();
		if (null == txn)
			throw new RuntimeException("no transaction.");
		txn.setOnzProcedure(this);
		try {
			return stub.call(this, argument, result);
		} finally {
			txn.setOnzProcedure(null);
		}
	}

	public String getName() {
		return stub.getName();
	}

	public void sendReadyAndWait() {
		// 发送事务执行阶段的两段式提交的准备完成，同时等待一起提交的信号。
		var r = new Ready();
		r.Argument.setOnzTid(onzTid);
		r.SendForWait(onzServer).await();
	}

	protected TaskCompletionSource<Long> sendFlushReady() {
		// 发送事务保存阶段的两段式提交的准备完成，同时等待一起提交的信号。
		var future = new TaskCompletionSource<Long>();
		var r = new FlushReady();
		r.Argument.setOnzTid(onzTid);
		r.Send(onzServer, (p) -> {
			if (r.getResultCode() == 0) {
				future.setResult(0L);
			} else {
				future.setException(new RuntimeException("FlushReady error=" + IModule.getErrorCode(r.getResultCode())));
			}
			return 0;
		});
		return future;
	}

	// helper
	// 注意参数是一个集合是为了实现eFlushPeriod准备的，历史原因。现在不实现了，本质上只需要传递一个可为空的OnzProcedure变量即可。
	public static void sendFlushAndWait(Set<OnzProcedure> onzProcedures) {
		// send all
		var futures = new ArrayList<TaskCompletionSource<Long>>();
		for (var onz : onzProcedures) {
			if (null != onz && onz.isEnd())
				futures.add(onz.sendFlushReady());
		}
		// wait all
		for (var future : futures) {
			future.await();
		}
	}
}
