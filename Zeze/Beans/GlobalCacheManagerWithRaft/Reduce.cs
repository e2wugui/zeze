// auto-generated

namespace Zeze.Beans.GlobalCacheManagerWithRaft
{
    public sealed class Reduce : Zeze.Raft.RaftRpc<Zeze.Beans.GlobalCacheManagerWithRaft.ReduceParam, Zeze.Beans.GlobalCacheManagerWithRaft.ReduceParam>
    {
        public const int ModuleId_ = 11001;
        public const int ProtocolId_ = -627817142;
        public const long TypeId_ = (long)ModuleId_ << 32 | (ProtocolId_ & 0xffff_ffff);

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
