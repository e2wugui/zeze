// auto-generated
using Zeze.Serialize;

namespace Zeze.Builtin.AutoKey
{
    public sealed class tAutoKeys : Zeze.Transaction.Table<Zeze.Builtin.AutoKey.BSeedKey, Zeze.Builtin.AutoKey.BAutoKey, Zeze.Builtin.AutoKey.BAutoKeyReadOnly>
    {
        public tAutoKeys() : base("Zeze_Builtin_AutoKey_tAutoKeys")
        {
        }

        public override int Id => -716529252;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_NextId = 1;

        public override Zeze.Builtin.AutoKey.BSeedKey DecodeKey(ByteBuffer _os_)
        {
            var _v_ = new Zeze.Builtin.AutoKey.BSeedKey();
            _v_.Decode(_os_);
            return _v_;
        }

        public override ByteBuffer EncodeKey(Zeze.Builtin.AutoKey.BSeedKey _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _v_.Encode(_os_);
            return _os_;
        }
    }
}
