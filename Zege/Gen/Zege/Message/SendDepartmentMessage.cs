// auto-generated

// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext
// ReSharper disable once CheckNamespace
namespace Zege.Message
{
    public sealed class SendDepartmentMessage : Zeze.Net.Rpc<Zege.Message.BSendDepartmentMessage, Zege.Message.BSendDepartmentMessageResult>
    {
        public const int ModuleId_ = 3;
        public const int ProtocolId_ = 63175624;
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 12948077512

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
