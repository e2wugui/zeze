package zezeboot.link;

import Zeze.Arch.LinkdUserSession;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Procedure;
import Zeze.Util.Str;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleLink extends AbstractModule {
	private static final Logger logger = LogManager.getLogger(ModuleLink.class);

	public void Start(zezeboot.App app) {
	}

	public void Stop(zezeboot.App app) {
	}

	@Override
	protected long ProcessAuthRequest(zezeboot.link.Auth r) {
		var so = r.getSender();
		var arg = r.Argument;
		logger.info("recv Auth from {}: {}", so, AsyncSocket.toStr(arg));
		var session = (LinkdUserSession)so.getUserState();
		session.setAccount(arg.getAccount());
		session.setClientAppVersion(Str.parseVersion(arg.getVersion()));
		//TODO: check arg.getToken();
		session.setAuthed();
		r.SendResultCode(0);
		return Procedure.Success;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLink(zezeboot.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
