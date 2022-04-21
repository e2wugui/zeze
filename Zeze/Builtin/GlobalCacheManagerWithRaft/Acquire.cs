// auto-generated

namespace Zeze.Builtin.GlobalCacheManagerWithRaft
{
    public sealed class Acquire : Zeze.Raft.RaftRpc<Zeze.Builtin.GlobalCacheManagerWithRaft.AcquireParam, Zeze.Builtin.GlobalCacheManagerWithRaft.ReduceParam>
    {
        public const int ModuleId_ = 11001;
        public const int ProtocolId_ = -1825434690;
        public const long TypeId_ = (long)ModuleId_ << 32 | (ProtocolId_ & 0xffff_ffff);

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
