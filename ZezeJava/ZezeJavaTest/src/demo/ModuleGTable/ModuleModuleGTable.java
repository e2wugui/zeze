package demo.ModuleGTable;

public class ModuleModuleGTable extends AbstractModule {
    // private static final Logger logger = LogManager.getLogger(ModuleModuleGTable.class);
    public void Start(demo.App app) {
    }

    public void Stop(demo.App app) {
    }

    public tGTable getGTable() {
        return _tGTable;
    }

    public tGTable2 getGTable2() {
        return _tGTable2;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleModuleGTable(demo.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
