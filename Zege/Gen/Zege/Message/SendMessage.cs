// auto-generated

// 发送消息。即时消息通知（电脑版？）
// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext
// ReSharper disable once CheckNamespace
namespace Zege.Message
{
    public sealed class SendMessage : Zeze.Net.Rpc<Zege.Message.BSendMessage, Zege.Message.BSendMessageResult>
    {
        public const int ModuleId_ = 3;
        public const int ProtocolId_ = 1198381937;
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 14083283825

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
