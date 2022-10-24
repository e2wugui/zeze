// auto-generated
using Zeze.Serialize;

// key is bag name
namespace Zeze.Builtin.Game.Bag
{
    public sealed class tbag : Zeze.Transaction.Table<string, Zeze.Builtin.Game.Bag.BBag, Zeze.Builtin.Game.Bag.BBagReadOnly>
    {
        public tbag() : base("Zeze_Builtin_Game_Bag_tbag")
        {
        }

        public override int Id => 863603985;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

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
    }
}
