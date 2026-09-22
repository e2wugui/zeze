package Game.Login;

public class ModuleLogin extends AbstractModule {
    // private static final Logger logger = LogManager.getLogger(ModuleLogin.class);
    public void Start(Game.App app) {
    }

    public void Stop(Game.App app) {
    }

    @Override
    protected long ProcessCreateRoleRequest(Game.Login.CreateRole r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessGetRoleListRequest(Game.Login.GetRoleList r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLogin(Game.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
