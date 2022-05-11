package Game.Login;

import Game.App;
import Game.AutoKey.ModuleAutoKey;
import Game.Server;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Provider.SetUserState;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;

public final class ModuleLogin extends AbstractModule {

	private ModuleAutoKey.AutoKey autoKey;

	public void Start(App app) {
		autoKey = ModuleAutoKey.getAutoKey("roleId");
	}

	public void Stop(App app) {
	}


	@Override
	protected long ProcessCreateRoleRequest(CreateRole rpc) {
		var session = ProviderUserSession.Get(rpc);

		var role = new BRole();

		long roleId = autoKey.nextId();
		role.setId(roleId);
		role.setName(rpc.Argument.getName());

		_trole.insert(roleId, role);

		// duplicate name check
		BRoleId tempVar2 = new BRoleId();
		tempVar2.setId(roleId);
		if (!_trolename.tryAdd(rpc.Argument.getName(), tempVar2)) {
			return ErrorCode(ResultCodeCreateRoleDuplicateRoleName);
		}

		var account = App.getProvider().Online.getTableAccount().getOrAdd(session.getAccount());
		account.getRoles().add(roleId);

		// initialize role data
		App.Game_Bag.GetBag(roleId).SetCapacity(50);

		rpc.Result = role;
		session.SendResponse(rpc);
		return Procedure.Success;
	}

	@Override
	protected long ProcessGetRoleListRequest(GetRoleList rpc) {
		var session = ProviderUserSession.Get(rpc);

		var account = App.getProvider().Online.getTableAccount().get(session.getAccount());
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

		session.SendResponse(rpc);
		return Procedure.Success;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLogin(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
