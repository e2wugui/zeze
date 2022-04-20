// auto-generated

namespace Zeze.Beans.Game.Online
{
    public sealed class Login : Zeze.Net.Rpc<Zeze.Beans.Game.Online.BLogin, Zeze.Transaction.EmptyBean>
    {
        public const int ModuleId_ = 11013;
        public const int ProtocolId_ = 311370305;
        public const long TypeId_ = (long)ModuleId_ << 32 | (ProtocolId_ & 0xffff_ffff);

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
