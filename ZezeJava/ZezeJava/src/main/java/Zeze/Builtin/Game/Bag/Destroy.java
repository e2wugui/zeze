// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

public class Destroy extends Zeze.Net.Rpc<Zeze.Builtin.Game.Bag.BDestroy, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11014;
    public static final int ProtocolId_ = -1194800685; // 3100166611
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47307869964755
    static { register(TypeId_, Destroy.class); }

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

    public Destroy() {
        Argument = new Zeze.Builtin.Game.Bag.BDestroy();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Destroy(Zeze.Builtin.Game.Bag.BDestroy arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
