package Zezex.Linkd;

import Zeze.Arch.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModuleLinkd extends AbstractModule {
	public static final Logger logger = LogManager.getLogger(ModuleLinkd.class);

	@SuppressWarnings("RedundantThrows")
	public void Start(@SuppressWarnings("unused") Zezex.App app) throws Exception {
	}

	@SuppressWarnings("RedundantThrows")
	public void Stop(@SuppressWarnings("unused") Zezex.App app) throws Exception {
	}

	@Override
	protected long ProcessAuthRequest(Auth rpc) {
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
		linkSession.setClientAppVersion(rpc.Argument.getAppVersion());
		linkSession.setAuthed();
		rpc.SendResultCode(Auth.Success);
		logger.info("Auth accout:{} ip:{}", linkSession.getAccount(), rpc.getSender().getRemoteAddress());
		return Zeze.Transaction.Procedure.Success;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkd(Zezex.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
