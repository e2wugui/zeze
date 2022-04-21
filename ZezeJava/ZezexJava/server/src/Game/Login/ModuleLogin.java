package Game.Login;

import Game.App;
import Game.Server;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Provider.SetUserState;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;

public final class ModuleLogin extends AbstractModule {

	public void Start(App app) {

	}

	public void Stop(App app) {
	}


	@Override
	protected long ProcessCreateRoleRequest(CreateRole rpc) {
		var session = ProviderUserSession.Get(rpc);

		BRoleData tempVar = new BRoleData();
		tempVar.setName(rpc.Argument.getName());
		long roleId = _trole.insert(tempVar);

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

		session.SendResponse(rpc);
		return Procedure.Success;
	}

	@Override
	protected long ProcessGetRoleListRequest(GetRoleList rpc) {
		var session = ProviderUserSession.Get(rpc);

		var account = App.getProvider().Online.getTableAccount().get(session.getAccount());
		if (null != account) {
			for (var roleId : account.getRoles()) {
				BRoleData roleData = _trole.get(roleId);
				if (null != roleData) {
					BRole tempVar = new BRole();
					tempVar.setId(roleId);
					tempVar.setName(roleData.getName());
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
