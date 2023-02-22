package Zeze.Game;

import Zeze.AppBase;
import Zeze.Arch.ProviderImplement;
import Zeze.Arch.ProviderService;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;

public class ProviderWithOnline extends ProviderImplement {
	protected Online online; // 需要外面初始化。App.Start.

	public Online getOnline() {
		return online;
	}

	@Override
	protected long ProcessLinkBroken(LinkBroken p) throws Exception {
		// 目前仅需设置online状态。
		if (!p.Argument.getContext().isEmpty()) {
			var roleId = Long.parseLong(p.Argument.getContext());
			online.linkBroken(p.Argument.getAccount(), roleId, ProviderService.getLinkName(p.getSender()), p.Argument.getLinkSid());
		}
		return Procedure.Success;
	}

	public void create(AppBase app) {
		online = Online.create(app);
		online.Initialize(app);
	}

	public void create(AppBase app, Class<? extends Bean> userDataClass) {
		create(app);
		Online.register(userDataClass);
	}

	public void start() {
		online.start();
	}

	public void stop() {
		if (online != null)
			online.stop();
	}
}
