package ClientGame.Equip;

import java.util.Objects;
import Zeze.IModule;

public class ModuleEquip extends AbstractModule {
	public void reportLogin(long roleId) {
		var r = new ReportLogin();
		r.Argument.setRoleId(roleId);
		r.SendForWait(Objects.requireNonNull(App.ClientService).GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("report login error=" + IModule.getErrorCode(r.getResultCode()));
	}

	public void Start(ClientGame.App app) throws Exception {
	}

	public void Stop(ClientGame.App app) throws Exception {
	}

	@Override
	protected long ProcessSEquipement(ClientGame.Equip.SEquipement p) {
		return Zeze.Transaction.Procedure.NotImplement;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleEquip(ClientGame.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
