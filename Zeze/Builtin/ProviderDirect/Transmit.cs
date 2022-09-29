// auto-generated

// 默认不启用事务，由协议实现自己控制。
namespace Zeze.Builtin.ProviderDirect
{
    public sealed class Transmit : Zeze.Net.Protocol<Zeze.Builtin.ProviderDirect.BTransmit>
    {
        public const int ModuleId_ = 11009;
        public const int ProtocolId_ = 902147088;
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 47284197108752

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;

        public Transmit()
        {
        }
    }
}
