package Zeze.Onz;

import org.jspecify.annotations.Nullable;

import Zeze.Builtin.Onz.BFuncProcedure;
import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

/** saga注册条目：在procedure基础上绑定补偿函数与补偿参数类型，支持FuncSagaEnd的解码与补偿调用。 */
public class OnzSagaStub<A extends Bean, R extends Bean, T extends Bean> extends OnzProcedureStub<A, R> {
	private final OnzFuncSaga<A, R> func;
	private final OnzFuncSagaEnd<T> funcCancel;
	private final Class<T> cancelClass;

	public OnzSagaStub(Onz onz,
					   String name,
					   OnzFuncSaga<A, R> func, Class<A> argumentClass, Class<R> resultClass,
					   OnzFuncSagaEnd<T> funcCancel, Class<T> cancelClass) {
		// super.func 在saga模式下未用。
		super(onz, name, null, argumentClass, resultClass);
		this.func = func;
		this.funcCancel = funcCancel;
		this.cancelClass = cancelClass;
	}

	@Override
	public OnzProcedure newProcedure(Rpc<?, ?> rpc, BFuncProcedure.Data funcArgument, IByteBuffer buffer) throws Exception {
		var a = super.getArgumentClass().getConstructor((Class<?>[])null).newInstance((Object[])null);
		var r = newResult();
		a.decode(buffer);
		return new OnzSaga(rpc, funcArgument, this, a, r);
	}

	@SuppressWarnings("unchecked")
	@Override
	public long call(OnzProcedure onzProcedure, Bean argument, Bean result) throws Exception {
		return func.call((OnzSaga)onzProcedure, (A)argument, (R)result);
	}

	public Bean decodeCancelArgument(@Nullable Binary argument) throws Exception {
		var bean = cancelClass.getConstructor((Class<?>[])null).newInstance((Object[])null);
		// 协调者不填充补偿载荷，空 Binary 时直接使用默认构造的空 bean，跳过 decode。
		if (argument != null && argument.size() > 0)
			bean.decode(ByteBuffer.Wrap(argument));
		return bean;
	}

	@SuppressWarnings("unchecked")
	public long end(OnzProcedure onzProcedure, Bean cancelArgument) throws Exception {
		return funcCancel.call((OnzSaga)onzProcedure, (T)cancelArgument);
	}
}
