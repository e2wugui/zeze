// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class SetDisableChoice extends Zeze.Net.Rpc<Zeze.Builtin.Provider.BSetDisableChoice.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = -2032655885; // 2262311411
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47281262305779
    static { register(TypeId_, SetDisableChoice.class); }

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

    public SetDisableChoice() {
        Argument = new Zeze.Builtin.Provider.BSetDisableChoice.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public SetDisableChoice(Zeze.Builtin.Provider.BSetDisableChoice.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
