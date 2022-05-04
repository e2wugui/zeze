package Zeze.Game;

import Zeze.Arch.ProviderImplement;
import Zeze.Arch.ProviderService;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Transaction.Procedure;

public class ProviderImplementWithOnline extends ProviderImplement {
	public Online Online; // 需要外面初始化。App.Start.

	@Override
	protected long ProcessLinkBroken(LinkBroken p) throws Throwable {
		// 目前仅需设置online状态。
		if (!p.Argument.getStates().isEmpty()) {
			var roleId = p.Argument.getStates().get(0);
			Online.onLinkBroken(roleId, p.Argument);
		}
		return Procedure.Success;
	}
}
