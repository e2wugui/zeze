// auto-generated
using Zeze.Serialize;

namespace Zeze.Builtin.Collections.LinkedMap
{
    public sealed class tValueIdToNodeId : Zeze.Transaction.Table<Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId>
    {
        public tValueIdToNodeId() : base("Zeze_Builtin_Collections_LinkedMap_tValueIdToNodeId")
        {
        }

        public override int Id => -1128401683;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_NodeId = 1;

        public override Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey DecodeKey(ByteBuffer _os_)
        {
            var _v_ = new Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey();
            _v_.Decode(_os_);
            return _v_;
        }

        public override ByteBuffer EncodeKey(Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _v_.Encode(_os_);
            return _os_;
        }
    }
}
