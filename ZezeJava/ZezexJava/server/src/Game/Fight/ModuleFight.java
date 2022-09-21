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
		return new Fighter(fighterId, _tfighters.getOrAdd(fighterId));
	}

	public long CalculateFighter(BFighterId fighterId) {
		// fighter 计算属性现在不主动通知客户端，需要客户端需要的时候来读取。

		Fighter fighter = new Fighter(fighterId, new BFighter());
		switch (fighterId.getType()) {
			case BFighterId.TypeRole:
				Game.App.getInstance().Game_Buf.GetBufs(fighterId.getInstanceId()).CalculateFighter(fighter);
				Game.App.getInstance().Game_Equip.CalculateFighter(fighter);
				break;
		}
		_tfighters.getOrAdd(fighterId).assign(fighter.getBean());
		return Procedure.Success;
	}

	public void StartCalculateFighter(long roleId) {
		BFighterId fighterId = new BFighterId(BFighterId.TypeRole, roleId);
		Zeze.Util.Task.run(Game.App.getInstance().Zeze.newProcedure(() -> CalculateFighter(fighterId),
				"CalculateFighter"), null, null, DispatchMode.Normal);
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public ModuleFight(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
