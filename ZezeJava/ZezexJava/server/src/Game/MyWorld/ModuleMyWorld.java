package Game.MyWorld;

import Game.App;
import Zeze.AppBase;
import Zeze.Hot.HotService;
import Zeze.World.World;

public class ModuleMyWorld extends AbstractModule implements IModuleMyWorld {
    private World world;

    public World getWorld() {
        return world;
    }

    @Override
    public void Initialize(AppBase app) throws Exception {
        world = World.create(app);
        world.initializeDefaultMmo();
    }

    public void Start(Game.App app) throws Exception {
        world.start();
    }

    @Override
    public void StartLast() {
    }

    public void Stop(Game.App app) throws Exception {
        world.stop();
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

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleMyWorld(Game.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
