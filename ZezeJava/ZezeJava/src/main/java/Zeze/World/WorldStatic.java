package Zeze.World;

import Zeze.Arch.ProviderUserSession;
import Zeze.Serialize.Vector3;

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
        var position = Vector3.ZERO; // todo 根据上下文得到初始位置：1. 地图的初始位置；2. 传送时到达的位置。
        var instanceId = world.getMapManager().enterMap(session, r.Argument.getMapId(), position);
        r.Result.setMapInstanceId(instanceId);

        session.sendResponseDirect(r);
        return 0;
    }
}
