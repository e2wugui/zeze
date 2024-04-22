// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Services.ServiceManager.BEditService;public class Edit extends Zeze.Raft.RaftRpc<BEditService, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 1821169203;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47340950705715
    static { register(TypeId_, Edit.class); }

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

    public Edit() {
        Argument = new BEditService();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Edit(BEditService arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
