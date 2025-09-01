package Zezex.Linkd;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Arch.*;
import Zeze.Util.Str;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModuleLinkd extends AbstractModule {
	public static final Logger logger = LogManager.getLogger(ModuleLinkd.class);

	@SuppressWarnings("RedundantThrows")
	public void Start(@SuppressWarnings("unused") Zezex.App app) throws Exception {
	}

	@SuppressWarnings("RedundantThrows")
	public void Stop(@SuppressWarnings("unused") Zezex.App app) throws Exception {
		if (null != timer) {
			timer.cancel(true);
			timer = null;
		}
	}

	@Override
	protected long ProcessAuthRequest(Auth rpc) throws Exception {
		/*
		BAccount account = _taccount.Get(protocol.Argument.Account);
		if (null == account || false == account.Token.Equals(protocol.Argument.Token))
		{
		    result.Send(protocol.Sender);
		    return Zeze.Transaction.Procedure.LogicError;
		}

		Game.App.Instance.LinkdService.GetSocket(account.SocketSessionId)?.Dispose(); // kick, 最好发个协议再踢。如果允许多个连接，去掉这行。
		account.SocketSessionId = protocol.Sender.SessionId;
		*/
		var linkSession = (LinkdUserSession)rpc.getSender().getUserState();
		linkSession.setAccount(rpc.Argument.getAccount());
		linkSession.setClientAppVersion(Str.parseVersion(rpc.Argument.getAppVersion()));
		linkSession.setAuthed();
		int r = App.LinkdProvider.choiceProvider(rpc.getSender(), rpc.Argument.getLoginQueueToken());
		if (r != 0) {
			logger.error("Auth account:{} ip:{} r:{}", linkSession.getAccount(), rpc.getSender().getRemoteAddress(), r);
			return errorCode(Auth.Error);
		}
		logger.info("Auth account:{} ip:{}", linkSession.getAccount(), rpc.getSender().getRemoteAddress());
		rpc.SendResult();
		return 0;
	}

	@Override
	protected long ProcessCs(Zezex.Linkd.Cs p) {
		logger.info("Cs {}", p.Argument);
		var res = new Sc();
		res.Argument.setAccount("Response");
		res.Send(p.getSender());
		if (null == timer) {
			timer = Task.scheduleUnsafe(1000, 1000, this::sendSc);
			clientId = p.getSender().getSessionId();
		}
		return 0;
	}

	AtomicInteger sendScCount = new AtomicInteger();
	long clientId;
	Future<?> timer;

	void sendSc() {
		var count = sendScCount.incrementAndGet();
		if (count > 3) {
			sendScCount.set(0);
			timer.cancel(true);
			timer = null;
			return;
		}
		var res = new Sc();
		res.Argument.setAccount("Response" + count);
		logger.info("sendSc: {}", res.Argument.getAccount());
		res.Send(App.LinkdService.GetSocket(clientId));
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkd(Zezex.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
