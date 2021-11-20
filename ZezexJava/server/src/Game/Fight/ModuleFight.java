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

	public Fighter GetFighter(BFighterId fighterId) throws Throwable {
		return new Fighter(fighterId, _tfighters.getOrAdd(fighterId));
	}

	public long CalculateFighter(BFighterId fighterId) throws Throwable {
		// fighter 计算属性现在不主动通知客户端，需要客户端需要的时候来读取。

		Fighter fighter = new Fighter(fighterId, new BFighter());
		switch (fighterId.getType()) {
			case BFighterId.TypeRole:
				App.getInstance().Game_Buf.GetBufs(fighterId.getInstanceId()).CalculateFighter(fighter);
				App.getInstance().Game_Equip.CalculateFighter(fighter);
				break;
		}
		_tfighters.getOrAdd(fighterId).Assign(fighter.getBean());
		return Procedure.Success;
	}

	public void StartCalculateFighter(long roleId) {
		BFighterId fighterId = new BFighterId(BFighterId.TypeRole, roleId);
		Zeze.Util.Task.Run(App.getInstance().Zeze.NewProcedure(() -> CalculateFighter(fighterId), "CalculateFighter", null));
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public static final int ModuleId = 4;

    private tfighters _tfighters = new tfighters();

    public Game.App App;

    public ModuleFight(Game.App app) {
        App = app;
        // register protocol factory and handles
        // register table
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_tfighters.getName()).getDatabaseName(), _tfighters);
    }

    public void UnRegister() {
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_tfighters.getName()).getDatabaseName(), _tfighters);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
