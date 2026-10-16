// auto-generated @formatter:off
package Game.Map;

// no argument. all in server
public class CEnterWorld extends Zeze.Net.Protocol<Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 8;
    public static final int ProtocolId_ = 1154620598;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 35514358966
    static { register(TypeId_, CEnterWorld.class); }

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    @Override
    public long getTypeId() {
        return TypeId_;
    }

    public CEnterWorld() {
        Argument = Zeze.Transaction.EmptyBean.instance;
    }
}
