package Game.Equip;

public class ModuleEquip extends AbstractModule {
    // private static final Logger logger = LogManager.getLogger(ModuleEquip.class);
    public void Start(Game.App app) {
    }

    public void Stop(Game.App app) {
    }

    @Override
    protected long ProcessEquipementRequest(Game.Equip.Equipement r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessUnequipementRequest(Game.Equip.Unequipement r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessReportLoginRequest(Game.Equip.ReportLogin r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessSendHotRequest(Game.Equip.SendHot r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessSendHotRemoveRequest(Game.Equip.SendHotRemove r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleEquip(Game.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
