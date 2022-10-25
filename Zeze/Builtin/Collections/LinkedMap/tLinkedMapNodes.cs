// auto-generated
using Zeze.Serialize;
using Zeze.Transaction;
using System.Threading.Tasks;

namespace Zeze.Builtin.Collections.LinkedMap
{
    public sealed class tLinkedMapNodes : Table<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode>, TableReadOnly<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeReadOnly>
    {
        public tLinkedMapNodes() : base("Zeze_Builtin_Collections_LinkedMap_tLinkedMapNodes")
        {
        }

        public override int Id => -1295098614;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_PrevNodeId = 1;
        public const int VAR_NextNodeId = 2;
        public const int VAR_Values = 3;

        public override Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey DecodeKey(ByteBuffer _os_)
        {
            var _v_ = new Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey();
            _v_.Decode(_os_);
            return _v_;
        }

        public override ByteBuffer EncodeKey(Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _v_.Encode(_os_);
            return _os_;
        }

        async Task<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeReadOnly> TableReadOnly<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeReadOnly>.GetAsync(Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey key)
        {
            return await GetAsync(key);
        }
    }
}
