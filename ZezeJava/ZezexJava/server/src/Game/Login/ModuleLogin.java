package Game.Login;

import Game.App;
import Zeze.Arch.ProviderUserSession;
import Zeze.Component.AutoKey;
import Zeze.Hot.HotService;
import Zeze.Transaction.Procedure;

public final class ModuleLogin extends AbstractModule implements IModuleLogin {
	private AutoKey autoKey;

	public void Start(App app) {
		autoKey = app.Zeze.getAutoKey("roleId");
	}

	public void Stop(App app) {
	}

	@Override
	protected long ProcessCreateRoleRequest(CreateRole rpc) {
		var session = ProviderUserSession.get(rpc);

		var role = new BRole();

		long roleId = autoKey.nextId();
		role.setId(roleId);
		role.setName(rpc.Argument.getName());

		_trole.insert(roleId, role);

		// duplicate name check
		BRoleId tempVar2 = new BRoleId();
		tempVar2.setId(roleId);
		if (!_trolename.tryAdd(rpc.Argument.getName(), tempVar2)) {
			return errorCode(ResultCodeCreateRoleDuplicateRoleName);
		}

		var account = _taccount.getOrAdd(session.getAccount());
		account.getRoles().add(roleId);

		// initialize role data
		// ...

		rpc.Result = role;
		session.sendResponseWhileCommit(rpc);
		return Procedure.Success;
	}

	@Override
	protected long ProcessGetRoleListRequest(GetRoleList rpc) {
		var session = ProviderUserSession.get(rpc);

		var account = _taccount.get(session.getAccount());
		if (null != account) {
			for (var roleId : account.getRoles()) {
				BRole role = _trole.get(roleId);
				if (null != role) {
					BRole tempVar = new BRole();
					tempVar.setId(roleId);
					tempVar.setName(role.getName());
					rpc.Result.getRoleList().add(tempVar);
				}
			}
			rpc.Result.setLastLoginRoleId(account.getLastLoginRoleId());
		}

		session.sendResponseWhileCommit(rpc);
		return Procedure.Success;
	}

	@Override
	public void start() throws Exception {
		Start(App);
	}

	@Override
	public void stop() throws Exception {
		Stop(App);
	}

	@Override
	public void upgrade(HotService old) throws Exception {

	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLogin(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
