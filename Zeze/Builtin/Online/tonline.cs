// auto-generated
using Zeze.Serialize;

namespace Zeze.Builtin.Online
{
    public sealed class tonline : Zeze.Transaction.Table<string, Zeze.Builtin.Online.BOnlines>
    {
        public tonline() : base("Zeze_Builtin_Online_tonline")
        {
        }

        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_All = 0;
        public const int VAR_Logins = 1;

        public override string DecodeKey(ByteBuffer _os_)
        {
            string _v_;
            _v_ = _os_.ReadString();
            return _v_;
        }

        public override ByteBuffer EncodeKey(string _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _os_.WriteString(_v_);
            return _os_;
        }

        public override Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId)
        {
            return variableId switch
            {
                0 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                1 => new Zeze.Transaction.ChangeVariableCollectorMap(() => new Zeze.Transaction.ChangeNoteMap2<string, Zeze.Builtin.Online.BOnline>(null)),
                _ => null,
            };
        }
    }
}
