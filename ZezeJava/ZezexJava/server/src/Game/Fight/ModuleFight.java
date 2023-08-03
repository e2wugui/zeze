package Game.Fight;

import Game.Buf.IModuleBuf;
import Game.Equip.IModuleEquip;
import Zeze.Hot.HotService;
import Zeze.Transaction.*;
import Game.*;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleFight extends AbstractModule implements IModuleFight {
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
				var contextBuf = App.HotManager.getModuleContext("Game.Buf", IModuleBuf.class);
				var contextEquip = App.HotManager.getModuleContext("Game.Equip", IModuleEquip.class);
				contextBuf.getService().getBufs(fighterId.getInstanceId()).calculateFighter(fighter);
				contextEquip.getService().calculateFighter(fighter);
				break;
		}
		_tfighters.getOrAdd(fighterId).assign(fighter.getBean());
		return Procedure.Success;
	}

	@Override
	public void StartCalculateFighter(long roleId) {
		BFighterId fighterId = new BFighterId(BFighterId.TypeRole, roleId);
		Zeze.Util.Task.run(Game.App.getInstance().Zeze.newProcedure(() -> CalculateFighter(fighterId),
				"CalculateFighter"), null, null, DispatchMode.Normal);
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

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public ModuleFight(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
