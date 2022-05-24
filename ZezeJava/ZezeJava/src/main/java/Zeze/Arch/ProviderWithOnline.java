package Zeze.Arch;

import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Transaction.Procedure;

public class ProviderWithOnline extends ProviderImplement {
	public Online Online; // 需要外面初始化。App.Start.

	@Override
	protected long ProcessLinkBroken(LinkBroken p) throws Throwable {
		// 目前仅需设置online状态。
		if (!p.Argument.getContext().isEmpty()) {
			Online.OnLinkBroken(p.Argument.getAccount(), p.Argument.getContext(), p.Argument);
		}
		return Procedure.Success;
	}
}
