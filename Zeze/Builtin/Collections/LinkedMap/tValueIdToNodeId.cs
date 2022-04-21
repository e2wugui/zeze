// auto-generated
using Zeze.Serialize;

namespace Zeze.Builtin.Collections.LinkedMap
{
    public sealed class tValueIdToNodeId : Zeze.Transaction.Table<Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId>
    {
        public tValueIdToNodeId() : base("Zeze_Builtin_Collections_LinkedMap_tValueIdToNodeId")
        {
        }

        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_All = 0;
        public const int VAR_NodeId = 1;

        public override Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey DecodeKey(ByteBuffer _os_)
        {
            Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey _v_ = new Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey();
            _v_.Decode(_os_);
            return _v_;
        }

        public override ByteBuffer EncodeKey(Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _v_.Encode(_os_);
            return _os_;
        }

        public override Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId)
        {
            return variableId switch
            {
                0 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                1 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                _ => null,
            };
        }
    }
}
