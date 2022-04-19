package Game;

import Zeze.Beans.ProviderDirect.Transmit;
import Zeze.Transaction.Procedure;

public class ProviderDirectMy extends Zeze.Arch.ProviderDirect {
	@Override
	protected long ProcessTransmit(Transmit p) throws Throwable {
		Zeze.Serialize.Serializable parameter = null;
		if (!p.Argument.getParameterBeanName().isEmpty()) {
			var factory = App.Instance.Game_Login.getOnlines().getTransmitParameterFactorys().get(p.Argument.getParameterBeanName());
			if (factory == null)
				return ErrorCode(ErrorTransmitParameterFactoryNotFound);

			parameter = factory.call(p.Argument.getParameterBeanName());
		}
		App.Instance.Game_Login.getOnlines().ProcessTransmit(
				p.Argument.getSender(),
				p.Argument.getActionName(),
				p.Argument.getRoles().keySet(),
				parameter);
		return Procedure.Success;
	}
}
