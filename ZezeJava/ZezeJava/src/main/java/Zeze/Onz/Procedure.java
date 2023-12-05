package Zeze.Onz;

import java.util.Set;
import Zeze.Application;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Transaction;

public class Procedure implements Zeze.Util.FuncLong {
	public static class Stub<A extends Bean, R extends Bean> {
		private final Application zeze;
		private final String name;
		private final FuncRemote<A, R> func;
		private final Class<A> argumentClass;
		private final Class<R> resultClass;

		public Stub(Application zeze, String name, FuncRemote<A, R> func, Class<A> argumentClass, Class<R> resultClass) {
			this.zeze = zeze;
			this.name = name;
			this.func = func;
			this.argumentClass = argumentClass;
			this.resultClass = resultClass;
		}

		public Zeze.Transaction.Procedure newZezeProcedure(long onzTid, IByteBuffer buffer) throws Exception {
			var a = argumentClass.getConstructor((Class<?>[])null).newInstance((Object[])null);
			var r = resultClass.getConstructor((Class<?>[])null).newInstance((Object[])null);
			a.decode(buffer);
			return zeze.newProcedure(new Procedure(onzTid,this, a, r), name);
		}

		public String getName() {
			return name;
		}

		// 类型具体化辅助函数
		@SuppressWarnings("unchecked")
		public long call(Bean argument, Bean result) throws Exception {
			return func.call((A)argument, (R)result);
		}
	}

	private final long onzTid;
	private final Stub<?, ?> stub;
	private final Bean argument;
	private final Bean result;

	public Procedure(long onzTid, Stub<?, ?> stub, Bean argument, Bean result) {
		this.onzTid = onzTid;
		this.stub = stub;
		this.argument = argument;
		this.result = result;
	}

	public long getOnzTid() {
		return onzTid;
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
			return stub.call(argument, result);
		} finally {
			txn.setOnzProcedure(null);
		}
	}

	public String getName() {
		return stub.getName();
	}

	public void sendReadyAndWait() {
		// 发送事务执行阶段的两段式提交的准备完成，同时等待一起提交的信号。
	}

	private void sendFlushReady() {
		// 发送事务保存阶段的两段式提交的准备完成，同时等待一起提交的信号。
	}

	private void flushWait() {

	}

	// helper
	public static void sendFlushAndWait(Set<Procedure> onzProcedures) {
		// send all
		for (var onz : onzProcedures) {
			if (null != onz)
				onz.sendFlushReady();
		}
		// wait all
		for (var onz : onzProcedures) {
			if (null != onz)
				onz.flushWait();
		}
	}
}
