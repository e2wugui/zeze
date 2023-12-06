package Zeze.Onz;

import Zeze.Application;
import Zeze.Net.AsyncSocket;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

public class OnzSagaStub<A extends Bean, R extends Bean> extends OnzProcedureStub<A, R> {
	private final OnzFuncSaga<A, R> func;
	private final OnzFuncSagaEnd funcCancel;

	public OnzSagaStub(Application zeze,
					   String name,
					   OnzFuncSaga<A, R> func, Class<A> argumentClass, Class<R> resultClass,
					   OnzFuncSagaEnd funcCancel) {
		// super.func 在saga模式下未用。
		super(zeze, name, null, argumentClass, resultClass);
		this.func = func;
		this.funcCancel = funcCancel;
	}

	@Override
	public OnzProcedure newProcedure(AsyncSocket onzServer, long onzTid, int flushMode, IByteBuffer buffer) throws Exception {
		var a = super.getArgumentClass().getConstructor((Class<?>[])null).newInstance((Object[])null);
		var r = super.getResultClass().getConstructor((Class<?>[])null).newInstance((Object[])null);
		a.decode(buffer);
		return new OnzSaga(onzServer, onzTid, flushMode,this, a, r);
	}

	@SuppressWarnings("unchecked")
	@Override
	public long call(OnzProcedure onzProcedure, Bean argument, Bean result) throws Exception {
		return func.call((OnzSaga)onzProcedure, (A)argument, (R)result);
	}

	@Override
	public long end(OnzProcedure onzProcedure) throws Exception {
		return funcCancel.call((OnzSaga)onzProcedure);
	}
}