package Zeze.Game;

import Zeze.Arch.ProviderImplement;
import Zeze.Arch.ProviderService;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Transaction.Procedure;

public class ProviderImplementWithOnline extends ProviderImplement {
	public Online online; // 需要外面初始化。App.Start.

	@Override
	protected long ProcessLinkBroken(LinkBroken p) throws Exception {
		// 目前仅需设置online状态。
		if (!p.Argument.getContext().isEmpty()) {
			var roleId = Long.parseLong(p.Argument.getContext());
			online.onLinkBroken(roleId, ProviderService.getLinkName(p.getSender()), p.Argument.getLinkSid());
		}
		return Procedure.Success;
	}
}
