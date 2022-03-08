// auto-generated

namespace Zeze.Component.GlobalCacheManagerWithRaft
{
    public sealed class Reduce : Zeze.Raft.RaftRpc<Zeze.Component.GlobalCacheManagerWithRaft.ReduceParam, Zeze.Component.GlobalCacheManagerWithRaft.ReduceParam>
    {
        public const int ModuleId_ = 11001;
        public const int ProtocolId_ = 331750857;
        public const long TypeId_ = (long)ModuleId_ << 32 | (ProtocolId_ & 0xffff_ffff);

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
