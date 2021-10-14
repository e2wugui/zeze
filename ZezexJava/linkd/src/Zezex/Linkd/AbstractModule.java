package Zezex.Linkd;

import Zezex.*;

// auto-generated


public abstract class AbstractModule implements Zeze.IModule {
	@Override
	public String getFullName() {
		return "Zezex.Linkd";
	}
	@Override
	public String getName() {
		return "Linkd";
	}
	@Override
	public int getId() {
		return 10000;
	}

	public abstract int ProcessAuthRequest(Auth rpc);

	public abstract int ProcessKeepAlive(KeepAlive protocol);

}