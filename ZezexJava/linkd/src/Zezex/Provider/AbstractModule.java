package Zezex.Provider;

import Zezex.*;

// auto-generated


public abstract class AbstractModule implements Zeze.IModule {
	@Override
	public String getFullName() {
		return "Zezex.Provider";
	}
	@Override
	public String getName() {
		return "Provider";
	}
	@Override
	public int getId() {
		return 10001;
	}

	public abstract int ProcessAnnounceProviderInfo(AnnounceProviderInfo protocol);

	public abstract int ProcessBindRequest(Bind rpc);

	public abstract int ProcessBroadcast(Broadcast protocol);

	public abstract int ProcessKick(Kick protocol);

	public abstract int ProcessModuleRedirectRequest(ModuleRedirect rpc);

	public abstract int ProcessModuleRedirectAllRequest(ModuleRedirectAllRequest protocol);

	public abstract int ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol);

	public abstract int ProcessReportLoad(ReportLoad protocol);

	public abstract int ProcessSend(Send protocol);

	public abstract int ProcessSetUserState(SetUserState protocol);

	public abstract int ProcessTransmit(Transmit protocol);

	public abstract int ProcessUnBindRequest(UnBind rpc);

}