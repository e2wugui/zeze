package Zeze.World;

public class WorldStatic extends AbstractWorldStatic {
    public final World world;

    public WorldStatic(World world) {
        this.world = world;
        var providerApp = world.providerApp;

        RegisterProtocols(providerApp.providerService);
        RegisterZezeTables(providerApp.zeze);
    }

    @Override
    protected long ProcessSwitchWorldRequest(Zeze.Builtin.World.Static.SwitchWorld r) {
        r.Result.setMapInstanceId(world.getMapManager().enterMap(r.Argument.getMapId()));
        r.Result.setServerId(world.providerApp.zeze.getConfig().getServerId());
        r.SendResult();
        return 0;
    }
}
