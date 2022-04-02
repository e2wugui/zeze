package Game;

import Zeze.Arch.*;
import Zeze.Beans.Provider.*;
import Zeze.Beans.ProviderDirect.*;
import Zeze.Transaction.Procedure;

public final class Provider extends Zeze.Arch.ProviderImplement {

	public Game.App App;

	public Provider(Game.App app) {
		super(app.Server, app.ServerDirect);

		App = app;
	}

	public void Start(Game.App app) {
	}

	public void Stop(Game.App app) {
	}

	@Override
	protected long ProcessLinkBroken(LinkBroken protocol) throws Throwable {
		// 目前仅需设置online状态。
		if (false == protocol.Argument.getStates().isEmpty()) {
			var roleId = protocol.Argument.getStates().get(0);
			Game.App.getInstance().Game_Login.getOnlines().OnLinkBroken(roleId);
		}
		return Procedure.Success;
	}

	@Override
	protected long ProcessTransmit(Transmit p) throws Throwable {
		Zeze.Serialize.Serializable parameter = null;
		if (false == p.Argument.getParameterBeanName().isEmpty())
		{
			var factory = App.Game_Login.getOnlines().getTransmitParameterFactorys().get(p.Argument.getParameterBeanName());
			if (null == factory)
				return ErrorCode(ErrorTransmitParameterFactoryNotFound);

			parameter = factory.call(p.Argument.getParameterBeanName());
		}
		App.Game_Login.getOnlines().ProcessTransmit(
				p.Argument.getSender(),
				p.Argument.getActionName(),
				p.Argument.getRoles().keySet(),
				parameter);
		return Procedure.Success;
	}

	@Override
	protected long ProcessSendConfirm(SendConfirm protocol) throws Throwable {
		var linkSession = (ProviderService.LinkSession)protocol.getSender().getUserState();
		var ctx = App.Server.<Game.Login.Onlines.ConfirmContext>TryGetManualContext(
				protocol.Argument.getConfirmSerialId());
		if (ctx != null) {
			ctx.ProcessLinkConfirm(linkSession.getName());
		}
		// linkName 也可以从 protocol.Sender.Connector.Name 获取。
		return Procedure.Success;
	}
}
