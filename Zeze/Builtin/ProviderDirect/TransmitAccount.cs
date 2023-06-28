// auto-generated

// 默认不启用事务，由协议实现自己控制。
// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.ProviderDirect
{
    public sealed class TransmitAccount : Zeze.Net.Protocol<Zeze.Builtin.ProviderDirect.BTransmitAccount>
    {
        public const int ModuleId_ = 11009;
        public const int ProtocolId_ = 952255342;
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 47284247217006

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
