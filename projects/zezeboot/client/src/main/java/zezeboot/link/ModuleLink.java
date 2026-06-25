package zezeboot.link;

import Zeze.Net.AsyncSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleLink extends AbstractModule {
	private static final Logger logger = LogManager.getLogger(ModuleLink.class);

	public void Start(zezeboot.App app) {
	}

	public void Stop(zezeboot.App app) {
	}

	public void onHandshakeDone(AsyncSocket so) {
		var r = new Auth(new BAuth("TestAccount", "TestToken", "0.1.0.0")).Send(so, rpc -> {
			var rc = rpc.getResultCode();
			logger.info("recv Auth resp({})", rc);
			if (rc == 0)
				App.zezeboot_login.onAuthed(rpc.getSender());
			return 0;
		});
		logger.info("send Auth = {}", r);
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLink(zezeboot.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
