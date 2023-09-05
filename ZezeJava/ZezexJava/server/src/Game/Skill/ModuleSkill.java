package Game.Skill;

import Game.*;
import Zeze.Hot.HotService;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleSkill extends AbstractModule implements IModuleSkill {
	public void Start(App app) {
	}

	@Override
	public void StartLast() {
	}

	public void Stop(App app) {
	}

	public Skills GetSkills(long roleId) {
		return new Skills(roleId, _tskills.getOrAdd(roleId));
	}

	@Override
	public void start() throws Exception {
		Start(App);
	}

	@Override
	public void startLast() throws Exception {
		StartLast();
	}

	@Override
	public void stop() throws Exception {
		Stop(App);
	}

	@Override
	public void upgrade(HotService old) throws Exception {

	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public ModuleSkill(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
