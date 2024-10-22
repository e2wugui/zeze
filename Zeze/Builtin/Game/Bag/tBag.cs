// auto-generated
using Zeze.Serialize;
using Zeze.Transaction;
using System.Threading.Tasks;

// key is bag name
// ReSharper disable JoinDeclarationAndInitializer RedundantNameQualifier
namespace Zeze.Builtin.Game.Bag
{
    public sealed class tBag : Table<string, Zeze.Builtin.Game.Bag.BBag>, TableReadOnly<string, Zeze.Builtin.Game.Bag.BBag, Zeze.Builtin.Game.Bag.BBagReadOnly>
    {
        public tBag() : base("Zeze_Builtin_Game_Bag_tBag")
        {
        }

        public override int Id => -694148490;
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

        async Task<Zeze.Builtin.Game.Bag.BBagReadOnly> TableReadOnly<string, Zeze.Builtin.Game.Bag.BBag, Zeze.Builtin.Game.Bag.BBagReadOnly>.GetAsync(string key)
        {
            return await GetAsync(key);
        }
    }
}
