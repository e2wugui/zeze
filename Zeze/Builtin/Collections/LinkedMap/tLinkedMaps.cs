// auto-generated
using Zeze.Serialize;

// key: LinkedMap的Name
namespace Zeze.Builtin.Collections.LinkedMap
{
    public sealed class tLinkedMaps : Zeze.Transaction.Table<string, Zeze.Builtin.Collections.LinkedMap.BLinkedMap>
    {
        public tLinkedMaps() : base("Zeze_Builtin_Collections_LinkedMap_tLinkedMaps")
        {
        }

        public override int Id => -72689413;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_HeadNodeId = 1;
        public const int VAR_TailNodeId = 2;
        public const int VAR_Count = 3;
        public const int VAR_LastNodeId = 4;

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
