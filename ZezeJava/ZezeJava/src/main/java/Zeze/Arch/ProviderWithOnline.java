package Zeze.Arch;

import Zeze.AppBase;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Transaction.Procedure;

public class ProviderWithOnline extends ProviderImplement {
	protected Online online; // 需要外面初始化。App.Start.

	public Online getOnline() {
		return online;
	}

	@Override
	protected long ProcessLinkBroken(LinkBroken p) throws Exception {
		// 目前仅需设置online状态。
		if (!p.Argument.getUserState().getContext().isEmpty()) {
			online.linkBroken(p.Argument.getAccount(), p.Argument.getUserState().getContext(),
					ProviderService.getLinkName(p.getSender()), p.Argument.getLinkSid(), p.Argument.getUserState().getLoginVersion());
		}
		return Procedure.Success;
	}

	public void create(AppBase app) {
		online = Online.create(app);
		online.Initialize(app);
	}

	public void start() {
		online.start();
	}

	public void stop() {
		if (online != null)
			online.stop();
	}
}
