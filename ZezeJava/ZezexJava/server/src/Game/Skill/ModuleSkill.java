package Game.Skill;

import Game.*;

public final class ModuleSkill extends AbstractModule {
	public void Start(App app) {
	}

	public void Stop(App app) {
	}

	public Skills GetSkills(long roleId) {
		return new Skills(roleId, _tskills.getOrAdd(roleId));
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleSkill(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
