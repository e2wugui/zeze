package Zeze.Arch;

import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Transaction.Procedure;

public class ProviderWithOnline extends ProviderImplement {
	public Online online; // 需要外面初始化。App.Start.

	@Override
	protected long ProcessLinkBroken(LinkBroken p) throws Throwable {
		// 目前仅需设置online状态。
		if (!p.Argument.getContext().isEmpty()) {
			online.onLinkBroken(p.Argument.getAccount(), p.Argument.getContext(),
					ProviderService.getLinkName(p.getSender()), p.Argument.getLinkSid());
		}
		return Procedure.Success;
	}
}
