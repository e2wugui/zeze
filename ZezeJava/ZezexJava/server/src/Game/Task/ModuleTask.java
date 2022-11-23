package Game.Task;

import Zeze.Game.Task;

public class ModuleTask extends AbstractModule {
    Task.Module module;

    public Task.Module getModule() {
        return module;
    }

    public void Start(Game.App app) throws Throwable {
    }

    public void Stop(Game.App app) throws Throwable {
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleTask(Game.App app) {
        super(app);
        module = new Task.Module(app.Zeze);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
