package Zeze.Onz;

import java.util.ArrayList;
import java.util.Set;
import Zeze.Builtin.Onz.BFuncProcedure;
import Zeze.Builtin.Onz.FlushReady;
import Zeze.Builtin.Onz.FuncProcedure;
import Zeze.Builtin.Onz.Ready;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;
import Zeze.Util.TaskCompletionSource;

public class OnzProcedure implements FuncLong {
	private final BFuncProcedure.Data funcArgument;
	private final OnzProcedureStub<?, ?> stub;
	private final Bean argument;
	private final Bean result;
	private final Rpc<?, ?> rpc;

	public Rpc<?, ?> getRpc() {
		return rpc;
	}

	public OnzProcedure(Rpc<?, ?> rpc,
						BFuncProcedure.Data funcArgument,
						OnzProcedureStub<?, ?> stub, Bean argument, Bean result) {
		this.rpc = rpc;
		this.funcArgument = funcArgument;
		this.stub = stub;
		this.argument = argument;
		this.result = result;
	}

	public int getFlushMode() {
		return funcArgument.getFlushMode();
	}

	public long getOnzTid() {
		return funcArgument.getOnzTid();
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
		return stub.call(this, argument, result);
	}

	public String getName() {
		return stub.getName();
	}

	public void sendReadyAndWait() {
		// 发送rpc结果
		var req = (FuncProcedure)rpc;
		var bbResult = ByteBuffer.Allocate();
		getResult().encode(bbResult);
		req.Result.setFuncResult(new Binary(bbResult));
		req.SendResult();

		// 发送事务执行阶段的两段式提交的准备完成，同时等待一起提交的信号。
		var r = new Ready();
		r.Argument.setOnzTid(getOnzTid());
		r.SendForWait(rpc.getSender()).await();
	}

	protected TaskCompletionSource<Long> sendFlushReady() {
		// 发送事务保存阶段的两段式提交的准备完成，同时等待一起提交的信号。
		var future = new TaskCompletionSource<Long>();
		var r = new FlushReady();
		r.Argument.setOnzTid(getOnzTid());
		r.Send(rpc.getSender(), (p) -> {
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
	public static void sendFlushAndWait(Set<OnzProcedure> onzProcedures) {
		if (null != onzProcedures) {
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
}
