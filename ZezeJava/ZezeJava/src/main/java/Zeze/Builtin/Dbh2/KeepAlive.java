// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class KeepAlive extends Zeze.Raft.RaftRpc<Zeze.Transaction.EmptyBeanDaTa, Zeze.Transaction.EmptyBeanDaTa> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = -1803428904; // 2491538392
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47358800944088

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

    public KeepAlive() {
        Argument = Zeze.Transaction.EmptyBeanDaTa.instance;
        Result = Zeze.Transaction.EmptyBeanDaTa.instance;
    }
}
