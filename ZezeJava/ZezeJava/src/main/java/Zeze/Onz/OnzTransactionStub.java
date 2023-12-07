package Zeze.Onz;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Data;

/**
 * 独立进程方式运行OnzServer时，需要注册Stub.
 *
 * @param <A>
 * @param <R>
 */
class OnzTransactionStub<A extends Data, R extends Data> {
	private final OnzServer onzServer;
	private final Class<A> argumentClass;
	private final Class<R> resultClass;

	public OnzTransactionStub(OnzServer onzServer, Class<A> argumentClass, Class<R> resultClass) {
		this.onzServer = onzServer;
		this.argumentClass = argumentClass;
		this.resultClass = resultClass;
	}

	public OnzTransaction<?, ?> createTransaction(String txnClassName, IByteBuffer buffer) throws Exception {
		var a = argumentClass.getConstructor((Class<?>[])null).newInstance((Object[])null);
		var r = resultClass.getConstructor((Class<?>[])null).newInstance((Object[])null);
		a.decode(buffer);
		return OnzServer.createTransaction(txnClassName, onzServer, a, r);
	}
}
