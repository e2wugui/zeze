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

	public abstract int ProcessAnnounceLinkInfo(AnnounceLinkInfo protocol);

	public abstract int ProcessDispatch(Dispatch protocol);

	public abstract int ProcessLinkBroken(LinkBroken protocol);

	public abstract int ProcessModuleRedirectRequest(ModuleRedirect rpc);

	public abstract int ProcessModuleRedirectAllRequest(ModuleRedirectAllRequest protocol);

	public abstract int ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol);

	public abstract int ProcessSendConfirm(SendConfirm protocol);

	public abstract int ProcessTransmit(Transmit protocol);

}