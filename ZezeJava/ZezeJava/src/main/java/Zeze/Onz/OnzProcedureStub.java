package Zeze.Onz;

import Zeze.Builtin.Onz.BFuncProcedure;
import Zeze.Net.Rpc;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

public class OnzProcedureStub<A extends Bean, R extends Bean> {
	private final Onz onz;
	private final String name;
	private final OnzFuncProcedure<A, R> func;
	private final Class<A> argumentClass;
	private final Class<R> resultClass;

	public OnzProcedureStub(Onz onz, String name, OnzFuncProcedure<A, R> func, Class<A> argumentClass, Class<R> resultClass) {
		this.onz = onz;
		this.name = name;
		this.func = func;
		this.argumentClass = argumentClass;
		this.resultClass = resultClass;
	}

	public OnzProcedure newProcedure(Rpc<?, ?> rpc, BFuncProcedure.Data funcArgument, IByteBuffer buffer) throws Exception {
		var a = argumentClass.getConstructor((Class<?>[])null).newInstance((Object[])null);
		var r = resultClass.getConstructor((Class<?>[])null).newInstance((Object[])null);
		a.decode(buffer);
		return new OnzProcedure(rpc, funcArgument,this, a, r);
	}

	public Onz getOnz() {
		return onz;
	}

	public String getName() {
		return name;
	}

	public Class<A> getArgumentClass() {
		return argumentClass;
	}

	public Class<R> getResultClass() {
		return resultClass;
	}

	// 类型具体化辅助函数
	@SuppressWarnings("unchecked")
	public long call(OnzProcedure onzProcedure, Bean argument, Bean result) throws Exception {
		return func.call(onzProcedure, (A)argument, (R)result);
	}
}
