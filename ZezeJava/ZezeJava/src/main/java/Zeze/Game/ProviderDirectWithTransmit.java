package Zeze.Game;

import Zeze.Arch.ProviderDirect;
import Zeze.Builtin.ProviderDirect.Transmit;
import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Game 版 Provider 直连模块：实现 Transmit 协议，把跨服动作转发给对应 OnlineSet 的 Online 处理。
 */
public class ProviderDirectWithTransmit extends ProviderDirect {
	private static final Logger logger = LogManager.getLogger(ProviderDirectWithTransmit.class);

	@Override
	protected long ProcessTransmit(Transmit p) {
		var name = p.Argument.getOnlineSetName();
		var online = ((ProviderWithOnline)providerApp.providerImplement).getOnline(name);
		if (online != null)
			online.processTransmit(p.Argument.getSender(), p.Argument.getActionName(), p.Argument.getRoles(), p.Argument.getParameter());
		else
			logger.error("unknown onlineSetName: '{}'", name);
		return Procedure.Success;
	}
}
