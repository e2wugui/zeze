// auto-generated
using Zeze.Serialize;

namespace Zeze.Builtin.Game.Bag
{
    public sealed class tItemClasses : Zeze.Transaction.Table<int, Zeze.Builtin.Game.Bag.BItemClasses>
    {
        public tItemClasses() : base("Zeze_Builtin_Game_Bag_tItemClasses")
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
    }
}
