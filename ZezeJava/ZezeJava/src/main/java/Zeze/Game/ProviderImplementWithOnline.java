package Zeze.Game;

import Zeze.Arch.ProviderImplement;
import Zeze.Arch.ProviderService;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Builtin.Provider.SendConfirm;
import Zeze.Transaction.Procedure;

public class ProviderImplementWithOnline extends ProviderImplement {
	public Online Online; // 需要外面初始化。App.Start.

	@Override
	protected long ProcessLinkBroken(LinkBroken p) {
		// 目前仅需设置online状态。
		if (!p.Argument.getStates().isEmpty()) {
			var roleId = p.Argument.getStates().get(0);
			Online.onLinkBroken(roleId, p.Argument);
		}
		return Procedure.Success;
	}

	@Override
	protected long ProcessSendConfirm(SendConfirm p) {
		var linkSession = (ProviderService.LinkSession)p.getSender().getUserState();
		var ctx = ProviderApp.ProviderService.<Online.ConfirmContext>TryGetManualContext(
				p.Argument.getConfirmSerialId());
		if (ctx != null) {
			ctx.processLinkConfirm(linkSession.getName());
		}
		// linkName 也可以从 protocol.Sender.Connector.Name 获取。
		return Procedure.Success;
	}
}
