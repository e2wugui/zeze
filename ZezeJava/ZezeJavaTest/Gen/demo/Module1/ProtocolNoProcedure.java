// auto-generated @formatter:off
package demo.Module1;

// protocol的注释
public class ProtocolNoProcedure extends Zeze.Net.Protocol<Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 1;
    public static final int ProtocolId_ = 1350939829;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 5645907125
    static { register(TypeId_, ProtocolNoProcedure.class); }

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

    public ProtocolNoProcedure() {
        Argument = Zeze.Transaction.EmptyBean.instance;
    }
}
