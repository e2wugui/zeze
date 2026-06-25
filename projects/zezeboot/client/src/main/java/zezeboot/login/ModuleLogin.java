package zezeboot.login;

import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleLogin extends AbstractModule {
	private static final Logger logger = LogManager.getLogger(ModuleLogin.class);

	public void Start(zezeboot.App app) {
	}

	public void Stop(zezeboot.App app) {
	}

	public void onAuthed(AsyncSocket so) {
		var r = new GetRoleList().Send(so, rpc -> {
			var rc = rpc.getResultCode();
			var res = rpc.Result;
			logger.info("recv GetRoleList resp({}): {}", rc, AsyncSocket.toStr(res));
			if (rc == 0) {
				if (res.getRoleList().isEmpty())
					createRole(rpc.getSender());
				else
					loginRole(rpc.getSender(), res.getRoleList().getFirst().getRoleId());
			}
			return 0;
		});
		logger.info("send GetRoleList: {}", r);
	}

	private void createRole(AsyncSocket so) {
		var r = new CreateRole(new BCreateRole("TestRoleName")).Send(so, rpc -> {
			var rc = rpc.getResultCode();
			var res = rpc.Result;
			logger.info("recv CreateRole resp({}): {}", rc, AsyncSocket.toStr(res));
			if (rc == 0)
				loginRole(rpc.getSender(), res.getRoleId());
			return 0;
		});
		logger.info("send CreateRole: {}", r);
	}

	private void loginRole(AsyncSocket so, long roleId) {
		var r = new LoginRole(new BRoleId(roleId)).Send(so, rpc -> {
			var rc = rpc.getResultCode();
			logger.info("recv LoginRole resp({})", rc);
			if (rc == 0) {
				var success = new HelloWorld().Send(rpc.getSender());
				logger.log(success ? Level.INFO : Level.ERROR, "send HelloWorld = {}", success);
			}
			return 0;
		});
		logger.info("send LoginRole: {}", r);
	}

	@Override
	protected long ProcessKick(zezeboot.login.Kick p) throws Exception {
		logger.info("recv Kick");
		App.LinkClient.stop();
		return Procedure.Success;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLogin(zezeboot.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
