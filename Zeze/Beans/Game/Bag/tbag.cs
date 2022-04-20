// auto-generated
using Zeze.Serialize;

namespace Zeze.Beans.Game.Bag
{
    public sealed class tbag : Zeze.Transaction.Table<string, Zeze.Beans.Game.Bag.BBag>
    {
        public tbag() : base("Zeze_Beans_Game_Bag_tbag")
        {
        }

        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_All = 0;
        public const int VAR_Capacity = 1;
        public const int VAR_Items = 2;

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
                2 => new Zeze.Transaction.ChangeVariableCollectorMap(() => new Zeze.Transaction.ChangeNoteMap2<int, Zeze.Beans.Game.Bag.BItem>(null)),
                _ => null,
            };
        }
    }
}
