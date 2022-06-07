// auto-generated

namespace Zeze.Builtin.GlobalCacheManagerWithRaft
{
    public sealed class ReLogin : Zeze.Raft.RaftRpc<Zeze.Builtin.GlobalCacheManagerWithRaft.LoginParam, Zeze.Transaction.EmptyBean>
    {
        public const int ModuleId_ = 11001;
        public const int ProtocolId_ = -1422572442; // 2872394854
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 47251807618150

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
