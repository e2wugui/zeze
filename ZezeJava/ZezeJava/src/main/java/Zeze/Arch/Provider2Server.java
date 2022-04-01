package Zeze.Arch;

import Zeze.Net.Protocol;
import Zeze.Transaction.TransactionLevel;
import Zeze.Beans.Provider2.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Provider2Server extends Zeze.Services.HandshakeBoth {
	private static final Logger logger = LogManager.getLogger(Provider2Server.class);

	public Provider2Server(String name, Zeze.Application zeze) throws Throwable {
		super(name, zeze);
	}

	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) throws Throwable {
		// 防止Client不进入加密，直接发送用户协议。
		if (!IsHandshakeProtocol(p.getTypeId())) {
			p.getSender().VerifySecurity();
		}

		if (p.getTypeId() == ModuleRedirect.TypeId_) {
			if (null != factoryHandle.Handle) {
				var moduleRedirect = (ModuleRedirect)p;
				if (null != getZeze()) {
					if (factoryHandle.Level != TransactionLevel.None) {
						getZeze().getTaskOneByOneByKey().Execute(
								moduleRedirect.Argument.getHashCode(),
								() -> Zeze.Util.Task.Call(getZeze().NewProcedure(() -> factoryHandle.Handle.handle(p),
												p.getClass().getName(), factoryHandle.Level, p.getUserState()),
										p, Protocol::SendResultCode));
					} else {
						getZeze().getTaskOneByOneByKey().Execute(moduleRedirect.Argument.getHashCode(),
								() -> Zeze.Util.Task.Call(() -> factoryHandle.Handle.handle(p), p,
										Protocol::SendResultCode));
					}
				}
			} else
				logger.warn("Protocol Handle Not Found: {}", p);
			return;
		}

		super.DispatchProtocol(p, factoryHandle);
	}
}
