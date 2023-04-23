package Zeze.Game;

import Zeze.Arch.ProviderDirect;
import Zeze.Builtin.ProviderDirect.Transmit;
import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProviderDirectWithTransmit extends ProviderDirect {
	private static final Logger logger = LogManager.getLogger(ProviderDirectWithTransmit.class);
	/*
	private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
	private static final MethodType voidType = MethodType.methodType(void.class);

	private final ConcurrentHashMap<String, MethodHandle> objectFactory = new ConcurrentHashMap<>();

	private Serializable createObject(String className) {
		try {
			return (Serializable)objectFactory.computeIfAbsent(className, cn -> {
				try {
					Class<?> cls = lookup.findClass(cn);
					if (!Serializable.class.isAssignableFrom(cls))
						throw new RuntimeException("not based on " + Serializable.class.getName());
					return lookup.findConstructor(cls, voidType);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			}).invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			logger.error("createObject failed: " + className, e);
			return null;
		}
	}
	*/

	@Override
	protected long ProcessTransmit(Transmit p) {
		var defaultOnline = ((ProviderWithOnline)providerApp.providerImplement).getOnline();
		var name = p.Argument.getOnlineSetName();
		var online = defaultOnline.getOnlineSet(name);
		if (online != null)
			online.processTransmit(p.Argument.getSender(), p.Argument.getActionName(), p.Argument.getRoles(), p.Argument.getParameter());
		else
			logger.error("unknown onlineSetName: {}", name);
		return Procedure.Success;
	}
}
