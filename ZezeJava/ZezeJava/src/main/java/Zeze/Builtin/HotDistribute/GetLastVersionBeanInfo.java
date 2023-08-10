// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

public class GetLastVersionBeanInfo extends Zeze.Net.Rpc<Zeze.Builtin.HotDistribute.BeanName.Data, Zeze.Builtin.HotDistribute.BLastVersionBeanInfo.Data> {
    public static final int ModuleId_ = 11033;
    public static final int ProtocolId_ = -1156173527; // 3138793769
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47389512970537
    static { register(TypeId_, GetLastVersionBeanInfo.class); }

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

    public GetLastVersionBeanInfo() {
        Argument = new Zeze.Builtin.HotDistribute.BeanName.Data();
        Result = new Zeze.Builtin.HotDistribute.BLastVersionBeanInfo.Data();
    }

    public GetLastVersionBeanInfo(Zeze.Builtin.HotDistribute.BeanName.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.HotDistribute.BLastVersionBeanInfo.Data();
    }
}
