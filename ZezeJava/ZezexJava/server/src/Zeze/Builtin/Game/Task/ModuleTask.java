package Zeze.Builtin.Game.Task;

import Zeze.Game.Task;

public class ModuleTask extends AbstractModule {
    Task.Module module;

    public Task.Module getModule() {
        return module;
    }

    public void Start(Game.App app) throws Throwable {
        module = new Task.Module(app.getZeze());
    }

    public void Stop(Game.App app) throws Throwable {
    }

    @Override
    protected long ProcessTriggerTaskEventRequest(Zeze.Builtin.Game.Task.TriggerTaskEvent r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleTask(Game.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
