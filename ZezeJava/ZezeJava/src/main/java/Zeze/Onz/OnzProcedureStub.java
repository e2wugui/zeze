package Zeze.Onz;

import Zeze.Application;
import Zeze.Net.AsyncSocket;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

public class OnzProcedureStub<A extends Bean, R extends Bean> {
	private final Application zeze;
	private final String name;
	private final OnzFuncProcedure<A, R> func;
	private final Class<A> argumentClass;
	private final Class<R> resultClass;

	public OnzProcedureStub(Application zeze, String name, OnzFuncProcedure<A, R> func, Class<A> argumentClass, Class<R> resultClass) {
		this.zeze = zeze;
		this.name = name;
		this.func = func;
		this.argumentClass = argumentClass;
		this.resultClass = resultClass;
	}

	public OnzProcedure newProcedure(AsyncSocket onzServer, long onzTid, int flushMode, IByteBuffer buffer) throws Exception {
		var a = argumentClass.getConstructor((Class<?>[])null).newInstance((Object[])null);
		var r = resultClass.getConstructor((Class<?>[])null).newInstance((Object[])null);
		a.decode(buffer);
		return new OnzProcedure(onzServer, onzTid, flushMode,this, a, r);
	}

	public Application getZeze() {
		return zeze;
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

	// 用于saga
	public long end(OnzProcedure onzProcedure) throws Exception {
		throw new UnsupportedOperationException(name);
	}
}
