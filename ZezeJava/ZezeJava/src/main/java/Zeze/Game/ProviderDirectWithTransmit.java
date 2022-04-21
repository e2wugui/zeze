package Zeze.Game;

import Zeze.Arch.ProviderDirect;
import Zeze.Builtin.ProviderDirect.Transmit;
import Zeze.Transaction.Procedure;

public class ProviderDirectWithTransmit extends ProviderDirect {
	@Override
	protected long ProcessTransmit(Transmit p) throws Throwable {
		var provider = (ProviderImplementWithOnline)ProviderApp.ProviderImplement;
		Zeze.Serialize.Serializable parameter = null;
		/*
		Transmit 参数 Decode 处理方式。
		if (!p.Argument.getParameterBeanName().isEmpty()) {
			var factory = provider.Online.getTransmitParameterFactorys().get(p.Argument.getParameterBeanName());
			if (factory == null)
				return ErrorCode(ErrorTransmitParameterFactoryNotFound);

			parameter = factory.call(p.Argument.getParameterBeanName());
		}
			*/
		provider.Online.processTransmit(p.Argument.getSender(), p.Argument.getActionName(),
				p.Argument.getRoles().keySet(), parameter);
		return Procedure.Success;
	}
}
