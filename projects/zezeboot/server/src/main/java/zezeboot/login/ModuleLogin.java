package zezeboot.login;

import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Provider.BSetUserState;
import Zeze.Builtin.Provider.BUserState;
import Zeze.Builtin.Provider.SetUserState;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleLogin extends AbstractModule {
	private static final Logger logger = LogManager.getLogger(ModuleLogin.class);

	public void Start(zezeboot.App app) {
	}

	public void Stop(zezeboot.App app) {
	}

	@Override
	protected long ProcessGetRoleListRequest(zezeboot.login.GetRoleList r) {
		var session = ProviderUserSession.get(r);
		var accountName = session.getAccount();
		logger.info("recv GetRoleList: account={}", accountName);
		var account = _tAccount.getOrAdd(accountName);
		var res = r.Result;
		var roleList = res.getRoleList();
		for (var roleId : account.getRoles()) {
			var role = _tRole.get(roleId);
			if (role != null)
				roleList.add(new BRole(roleId, role.getRoleName()));
		}
		res.setLastLoginRoleId(account.getLastLoginRoleId());
		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	@Override
	protected long ProcessCreateRoleRequest(zezeboot.login.CreateRole r) {
		var session = ProviderUserSession.get(r);
		var accountName = session.getAccount();
		var arg = r.Argument;
		logger.info("recv CreateRole: account={}, arg={}", accountName, AsyncSocket.toStr(arg));
		var roleName = arg.getName();
		// check roleName is valid
		if (_tRoleName.get(roleName) != null)
			return ResultCodeCreateRoleDuplicateRoleName;

		var roleId = App.Zeze.getAutoKey("RoleId").nextId();
		_tRole.put(roleId, new BRole(roleId, roleName));
		_tRoleName.put(roleName, new BRoleId(roleId));
		_tAccount.getOrAdd(accountName).getRoles().add(roleId);

		var res = r.Result;
		res.setRoleId(roleId);
		res.setRoleName(roleName);
		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	@Override
	protected long ProcessLoginRoleRequest(zezeboot.login.LoginRole r) {
		var session = ProviderUserSession.get(r);
		var accountName = session.getAccount();
		var arg = r.Argument;
		logger.info("recv LoginRole: account={}, linkId={}, linkSid={}, arg={}",
				accountName, session.getLink().getSessionId(), session.getLinkSid(), AsyncSocket.toStr(arg));

		var roleId = arg.getRoleId();
		var account = _tAccount.getOrAdd(accountName);
		if (!account.getRoles().contains(roleId))
			return Procedure.LogicError;
		account.setLastLoginRoleId(roleId);

		Transaction.whileCommit(() -> {
			var success = new SetUserState(new BSetUserState.Data(session.getLinkSid(),
					new BUserState.Data(String.valueOf(roleId), null, null))).Send(session.getLink());
			logger.log(success ? Level.INFO : Level.ERROR, "send SetUserState = {}", success);
		});

		//TODO

		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	@Override
	protected long ProcessHelloWorld(zezeboot.login.HelloWorld p) {
		var session = ProviderUserSession.get(p);
		logger.info("recv HelloWorld: account={}, roleId={}, linkId={}, linkSid={}",
				session.getAccount(), session.getRoleId(), session.getLink().getSessionId(), session.getLinkSid());
		return Procedure.Success;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLogin(zezeboot.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
