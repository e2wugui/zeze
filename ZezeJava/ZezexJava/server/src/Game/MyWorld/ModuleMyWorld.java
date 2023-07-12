package Game.MyWorld;

import Zeze.AppBase;
import Zeze.World.World;

public class ModuleMyWorld extends AbstractModule {
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

    public void Stop(Game.App app) throws Exception {
        world.stop();
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleMyWorld(Game.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
