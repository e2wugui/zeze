package Zege.Notify;

public class ModuleNotify extends AbstractModule {
    public void Start(Zege.App app) throws Exception {
    }

    public void Stop(Zege.App app) throws Exception {
    }

    @Override
    protected long ProcessNotifyNodeLogBeanNotify(Zege.Notify.NotifyNodeLogBeanNotify p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleNotify(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
