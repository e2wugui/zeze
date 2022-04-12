// auto-generated
using Zeze.Serialize;

namespace Zeze.Beans.Collections.LinkedMap
{
    public sealed class tValueIdToNodeId : Zeze.Transaction.Table<Zeze.Beans.Collections.LinkedMap.BLinkedMapKey, Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeId>
    {
        public tValueIdToNodeId() : base("Zeze_Beans_Collections_LinkedMap_tValueIdToNodeId")
        {
        }

        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_All = 0;
        public const int VAR_NodeId = 1;

        public override Zeze.Beans.Collections.LinkedMap.BLinkedMapKey DecodeKey(ByteBuffer _os_)
        {
            Zeze.Beans.Collections.LinkedMap.BLinkedMapKey _v_ = new Zeze.Beans.Collections.LinkedMap.BLinkedMapKey();
            _v_.Decode(_os_);
            return _v_;
        }

        public override ByteBuffer EncodeKey(Zeze.Beans.Collections.LinkedMap.BLinkedMapKey _v_)
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
