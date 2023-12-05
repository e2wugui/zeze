package Zeze.Onz;

import Zeze.Application;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;

public class OnzSagaStub<A extends Bean, R extends Bean> extends OnzProcedureStub<A, R> {
	private final OnzFuncConfirm<A, R> func;
	private final OnzFuncCancel funcCancel;

	public OnzSagaStub(Application zeze,
					   String name,
					   OnzFuncConfirm<A, R> func, Class<A> argumentClass, Class<R> resultClass,
					   OnzFuncCancel funcCancel) {
		// super.func 在saga模式下未用。
		super(zeze, name, null, argumentClass, resultClass);
		this.func = func;
		this.funcCancel = funcCancel;
	}

	@Override
	public Procedure newZezeProcedure(long onzTid, IByteBuffer buffer) throws Exception {
		var a = super.getArgumentClass().getConstructor((Class<?>[])null).newInstance((Object[])null);
		var r = super.getResultClass().getConstructor((Class<?>[])null).newInstance((Object[])null);
		a.decode(buffer);
		return super.getZeze().newProcedure(new OnzSaga(onzTid, this, a, r), super.getName());
	}

	@SuppressWarnings("unchecked")
	@Override
	public long call(OnzProcedure onzProcedure, Bean argument, Bean result) throws Exception {
		return func.call((OnzSaga)onzProcedure, (A)argument, (R)result);
	}

	@Override
	public long cancel(OnzProcedure onzProcedure) throws Exception {
		return funcCancel.call((OnzSaga)onzProcedure);
	}
}