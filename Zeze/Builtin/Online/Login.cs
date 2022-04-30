// auto-generated

namespace Zeze.Builtin.Online
{
    public sealed class Login : Zeze.Net.Rpc<Zeze.Builtin.Online.BLogin, Zeze.Transaction.EmptyBean>
    {
        public const int ModuleId_ = 11100;
        public const int ProtocolId_ = -1498951762;
        public const long TypeId_ = (long)ModuleId_ << 32 | (ProtocolId_ & 0xffff_ffff);

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
