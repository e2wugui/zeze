package Zeze.Arch;

import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Beans.Provider.*;

public abstract class ProviderImplement extends AbstractProviderImplement {
	//private static final Logger logger = LogManager.getLogger(ProviderImplement.class);

	private ProviderService service;
	public ProviderService getService() {
		return service;
	}

	public void setService(ProviderService service) {
		this.service = service;
		RegisterProtocols(service);
	}

	private void SendKick(AsyncSocket sender, long linkSid, int code, String desc) {
		var p = new Kick();
		p.Argument.setLinksid(linkSid);
		p.Argument.setCode(code);
		p.Argument.setDesc(desc);
		p.Send(sender);
	}

	@Override
	protected long ProcessDispatch(Dispatch p) throws Throwable {
		try {
			var factoryHandle = service.FindProtocolFactoryHandle(p.Argument.getProtocolType());
			if (null == factoryHandle) {
				SendKick(p.getSender(), p.Argument.getLinkSid(), BKick.ErrorProtocolUnkown, "unknown protocol");
				return Procedure.LogicError;
			}
			var p2 = factoryHandle.Factory.create();
			p2.Decode(Zeze.Serialize.ByteBuffer.Wrap(p.Argument.getProtocolData()));
			p2.setSender(p.getSender());

			var session = new ProviderSession(service, p.Argument.getAccount(), p.Argument.getStates(), p.getSender(), p.Argument.getLinkSid());

			p2.setUserState(session);
			if (Transaction.getCurrent() != null) {
				// 已经在事务中，嵌入执行。此时忽略p2的NoProcedure配置。
				Transaction.getCurrent().getTopProcedure().setActionName(p2.getClass().getName());
				Transaction.getCurrent().getTopProcedure().setUserState(p2.getUserState());
				return Zeze.Util.Task.Call(() -> factoryHandle.Handle.handleProtocol(p2), p2, (p3, code) -> {
						p3.setResultCode(code);
						session.SendResponse(p3);
				});
			}

			if (p2.getSender().getService().getZeze() == null || factoryHandle.Level == TransactionLevel.None) {
				// 应用框架不支持事务或者协议配置了"不需要事务”
				return Zeze.Util.Task.Call(() -> factoryHandle.Handle.handleProtocol(p2), p2, (p3, code) -> {
						p3.setResultCode(code);
						session.SendResponse(p3);
				});
			}

			// 创建存储过程并且在当前线程中调用。
			return Zeze.Util.Task.Call(
					p2.getSender().getService().getZeze().NewProcedure(
							() -> factoryHandle.Handle.handleProtocol(p2), p2.getClass().getName(), factoryHandle.Level, p2.getUserState()),
					p2, (p3, code) -> { p3.setResultCode(code); session.SendResponse(p3);
					});
		}
		catch (Throwable ex) {
			SendKick(p.getSender(), p.Argument.getLinkSid(), BKick.ErrorProtocolException, ex.toString());
			throw ex;
		}
	}


	@Override
	protected long ProcessAnnounceLinkInfo(AnnounceLinkInfo protocol) throws Throwable {
		var linkSession = (ProviderService.LinkSession)protocol.getSender().getUserState();
		linkSession.Setup(protocol.Argument.getLinkId(), protocol.Argument.getProviderSessionId());
		return Procedure.Success;
	}
}
