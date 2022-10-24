// auto-generated
using Zeze.Serialize;

namespace Zeze.Builtin.Game.Online
{
    public sealed class tonline : Zeze.Transaction.Table<long, Zeze.Builtin.Game.Online.BOnline, Zeze.Builtin.Game.Online.BOnlineReadOnly>
    {
        public tonline() : base("Zeze_Builtin_Game_Online_tonline")
        {
        }

        public override int Id => -1571889602;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_LinkName = 1;
        public const int VAR_LinkSid = 2;

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
