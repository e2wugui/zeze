package Game.Skill;

import Game.*;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleSkill extends AbstractModule {
	public void Start(App app) {
	}

	public void Stop(App app) {
	}

	public Skills GetSkills(long roleId) throws Throwable {
		return new Skills(roleId, _tskills.getOrAdd(roleId));
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public static final int ModuleId = 5;

    private tskills _tskills = new tskills();

    public Game.App App;

    public ModuleSkill(Game.App app) {
        App = app;
        // register protocol factory and handles
        // register table
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_tskills.getName()).getDatabaseName(), _tskills);
    }

    public void UnRegister() {
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_tskills.getName()).getDatabaseName(), _tskills);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
