// auto-generated
using Zeze.Serialize;

namespace Zeze.Beans.Game.Bag
{
    public sealed class tItemClasses : Zeze.Transaction.Table<int, Zeze.Beans.Game.Bag.BItemClasses>
    {
        public tItemClasses() : base("Zeze_Beans_Game_Bag_tItemClasses")
        {
        }

        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_All = 0;
        public const int VAR_ItemClasses = 1;

        public override int DecodeKey(ByteBuffer _os_)
        {
            int _v_;
            _v_ = _os_.ReadInt();
            return _v_;
        }

        public override ByteBuffer EncodeKey(int _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _os_.WriteInt(_v_);
            return _os_;
        }

        public override Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId)
        {
            return variableId switch
            {
                0 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                1 => new Zeze.Transaction.ChangeVariableCollectorSet(),
                _ => null,
            };
        }
    }
}
