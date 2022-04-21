// auto-generated

namespace Zeze.Builtin.RedoQueue
{
    public sealed class RunTask : Zeze.Net.Rpc<Zeze.Builtin.RedoQueue.BQueueTask, Zeze.Builtin.RedoQueue.BTaskId>
    {
        public const int ModuleId_ = 11010;
        public const int ProtocolId_ = 1530872255;
        public const long TypeId_ = (long)ModuleId_ << 32 | (ProtocolId_ & 0xffff_ffff);

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
    }
}
