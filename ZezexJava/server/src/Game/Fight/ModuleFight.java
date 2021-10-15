package Game.Fight;

import Zeze.Transaction.*;
import Game.*;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleFight extends AbstractModule {
	public void Start(App app) {
	}

	public void Stop(App app) {
	}

	public Fighter GetFighter(BFighterId fighterId) {
		return new Fighter(fighterId, _tfighters.GetOrAdd(fighterId));
	}

	public int CalculateFighter(BFighterId fighterId) {
		// fighter 计算属性现在不主动通知客户端，需要客户端需要的时候来读取。

		Fighter fighter = new Fighter(fighterId, new BFighter());
		switch (fighterId.getType()) {
			case BFighterId.TypeRole:
				App.getInstance().getGameBuf().GetBufs(fighterId.getInstanceId()).CalculateFighter(fighter);
				App.getInstance().getGameEquip().CalculateFighter(fighter);
				break;
		}
		_tfighters.GetOrAdd(fighterId).Assign(fighter.getBean());
		return Procedure.Success;
	}

	public void StartCalculateFighter(long roleId) {
		BFighterId fighterId = new BFighterId(BFighterId.TypeRole, roleId);
		Task.Run(App.getInstance().getZeze().NewProcedure(() -> CalculateFighter(fighterId), "CalculateFighter", null).Call);
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
	public static final int ModuleId = 4;

	private tfighters _tfighters = new tfighters();

	private App App;
	public App getApp() {
		return App;
	}

	public ModuleFight(App app) {
		App = app;
		// register protocol factory and handles
		// register table
		getApp().getZeze().AddTable(getApp().getZeze().Config.GetTableConf(_tfighters.Name).DatabaseName, _tfighters);
	}

	@Override
	public void UnRegister() {
		getApp().getZeze().RemoveTable(getApp().getZeze().Config.GetTableConf(_tfighters.Name).DatabaseName, _tfighters);
	}
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}