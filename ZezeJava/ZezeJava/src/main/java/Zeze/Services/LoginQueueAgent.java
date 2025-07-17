package Zeze.Services;

import Zeze.Builtin.LoginQueueServer.AnnounceSecret;

public class LoginQueueAgent extends AbstractLoginQueueAgent {
	@Override
	protected long ProcessAnnounceSecret(AnnounceSecret r) throws Exception {
		return 0;
	}
}
