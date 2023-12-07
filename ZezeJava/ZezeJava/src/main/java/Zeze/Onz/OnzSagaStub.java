package Zeze.Onz;

import Zeze.Application;
import Zeze.Builtin.Onz.BFuncProcedure;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

public class OnzSagaStub<A extends Bean, R extends Bean, T extends Bean> extends OnzProcedureStub<A, R> {
	private final OnzFuncSaga<A, R> func;
	private final OnzFuncSagaEnd<T> funcCancel;
	private final Class<T> cancelClass;

	public OnzSagaStub(Application zeze,
					   String name,
					   OnzFuncSaga<A, R> func, Class<A> argumentClass, Class<R> resultClass,
					   OnzFuncSagaEnd<T> funcCancel, Class<T> cancelClass) {
		// super.func 在saga模式下未用。
		super(zeze, name, null, argumentClass, resultClass);
		this.func = func;
		this.funcCancel = funcCancel;
		this.cancelClass = cancelClass;
	}

	@Override
	public OnzProcedure newProcedure(AsyncSocket onzServer, BFuncProcedure.Data argument, IByteBuffer buffer) throws Exception {
		var a = super.getArgumentClass().getConstructor((Class<?>[])null).newInstance((Object[])null);
		var r = super.getResultClass().getConstructor((Class<?>[])null).newInstance((Object[])null);
		a.decode(buffer);
		return new OnzSaga(onzServer, argument,this, a, r);
	}

	@SuppressWarnings("unchecked")
	@Override
	public long call(OnzProcedure onzProcedure, Bean argument, Bean result) throws Exception {
		return func.call((OnzSaga)onzProcedure, (A)argument, (R)result);
	}

	public Bean decodeCancelArgument(Binary argument) throws Exception {
		var bean = cancelClass.getConstructor((Class<?>[])null).newInstance((Object[])null);
		bean.decode(ByteBuffer.Wrap(argument));
		return bean;
	}

	@SuppressWarnings("unchecked")
	public long end(OnzProcedure onzProcedure, Bean cancelArgument) throws Exception {
		return funcCancel.call((OnzSaga)onzProcedure, (T)cancelArgument);
	}
}