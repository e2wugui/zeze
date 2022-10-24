// auto-generated
using Zeze.Serialize;

namespace Zeze.Builtin.Game.Online
{
    public sealed class tlocal : Zeze.Transaction.Table<long, Zeze.Builtin.Game.Online.BLocal, Zeze.Builtin.Game.Online.BLocalReadOnly>
    {
        public tlocal() : base("Zeze_Builtin_Game_Online_tlocal")
        {
        }

        public override int Id => -1657900798;
        public override bool IsMemory => true;
        public override bool IsAutoKey => false;

        public const int VAR_LoginVersion = 1;
        public const int VAR_Datas = 2;

        public override long DecodeKey(ByteBuffer _os_)
        {
            long _v_;
            _v_ = _os_.ReadLong();
            return _v_;
        }

        public override ByteBuffer EncodeKey(long _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _os_.WriteLong(_v_);
            return _os_;
        }
    }
}
