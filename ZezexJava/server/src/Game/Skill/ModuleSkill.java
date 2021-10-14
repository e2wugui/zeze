package Game.Skill;

import Game.*;

// auto-generated


public final class ModuleSkill extends AbstractModule {
	public void Start(App app) {
	}

	public void Stop(App app) {
	}

	public Skills GetSkills(long roleId) {
		return new Skills(roleId, _tskills.GetOrAdd(roleId));
	}


	public static final int ModuleId = 5;

	private tskills _tskills = new tskills();

	private App App;
	public App getApp() {
		return App;
	}

	public ModuleSkill(App app) {
		App = app;
		// register protocol factory and handles
		// register table
		getApp().getZeze().AddTable(getApp().getZeze().Config.GetTableConf(_tskills.Name).DatabaseName, _tskills);
	}

	@Override
	public void UnRegister() {
		getApp().getZeze().RemoveTable(getApp().getZeze().Config.GetTableConf(_tskills.Name).DatabaseName, _tskills);
	}

}