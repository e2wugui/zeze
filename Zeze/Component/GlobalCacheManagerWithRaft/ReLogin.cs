// auto-generated

namespace Zeze.Component.GlobalCacheManagerWithRaft
{
    public sealed class ReLogin : Zeze.Net.Rpc<Zeze.Component.GlobalCacheManagerWithRaft.LoginParam, Zeze.Transaction.EmptyBean>
    {
        public const int ModuleId_ = 11001;
        public const int ProtocolId_ = 699372078;
        public const long TypeId_ = (long)ModuleId_ << 32 | (ProtocolId_ & 0xffff_ffff);

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
