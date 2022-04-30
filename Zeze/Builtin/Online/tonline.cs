// auto-generated
using Zeze.Serialize;

namespace Zeze.Builtin.Online
{
    public sealed class tonline : Zeze.Transaction.Table<string, Zeze.Builtin.Online.BOnline>
    {
        public tonline() : base("Zeze_Builtin_Online_tonline")
        {
        }

        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_All = 0;
        public const int VAR_LinkName = 1;
        public const int VAR_LinkSid = 2;
        public const int VAR_State = 3;
        public const int VAR_ReliableNotifyMark = 4;
        public const int VAR_ReliableNotifyQueue = 5;
        public const int VAR_ReliableNotifyConfirmCount = 6;
        public const int VAR_ReliableNotifyTotalCount = 7;
        public const int VAR_ProviderId = 8;
        public const int VAR_ProviderSessionId = 9;
        public const int VAR_LoginVersion = 10;

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
                1 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                2 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                3 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                4 => new Zeze.Transaction.ChangeVariableCollectorSet(),
                5 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                6 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                7 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                8 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                9 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                10 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                _ => null,
            };
        }
    }
}
