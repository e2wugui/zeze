package Zeze.World;

import Zeze.Arch.ProviderUserSession;

public class WorldStatic extends AbstractWorldStatic {
    public final World world;

    public WorldStatic(World world) {
        this.world = world;
        var providerApp = world.providerApp;

        RegisterProtocols(providerApp.providerService);
        RegisterZezeTables(providerApp.zeze);
    }

    @Override
    protected long ProcessSwitchWorldRequest(Zeze.Builtin.World.Static.SwitchWorld r) throws Exception {
        var session = ProviderUserSession.get(r);

        var instanceId = world.getMapManager().enterMap(session, r.Argument);
        r.Result.setMapInstanceId(instanceId);

        session.sendResponseDirect(r);
        return 0;
    }
}
