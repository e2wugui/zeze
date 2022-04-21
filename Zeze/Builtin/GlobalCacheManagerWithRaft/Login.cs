// auto-generated

namespace Zeze.Builtin.GlobalCacheManagerWithRaft
{
    public sealed class Login : Zeze.Raft.RaftRpc<Zeze.Builtin.GlobalCacheManagerWithRaft.LoginParam, Zeze.Transaction.EmptyBean>
    {
        public const int ModuleId_ = 11001;
        public const int ProtocolId_ = -1968616174;
        public const long TypeId_ = (long)ModuleId_ << 32 | (ProtocolId_ & 0xffff_ffff);

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
