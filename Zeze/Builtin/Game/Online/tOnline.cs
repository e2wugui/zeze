// auto-generated
using Zeze.Serialize;
using Zeze.Transaction;
using System.Threading.Tasks;

// ReSharper disable JoinDeclarationAndInitializer RedundantNameQualifier
namespace Zeze.Builtin.Game.Online
{
    public sealed class tOnline : Table<long, Zeze.Builtin.Game.Online.BOnline>, TableReadOnly<long, Zeze.Builtin.Game.Online.BOnline, Zeze.Builtin.Game.Online.BOnlineReadOnly>
    {
        public tOnline() : base("Zeze_Builtin_Game_Online_tOnline")
        {
        }

        public override int Id => -1094649995;
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

        async Task<Zeze.Builtin.Game.Online.BOnlineReadOnly> TableReadOnly<long, Zeze.Builtin.Game.Online.BOnline, Zeze.Builtin.Game.Online.BOnlineReadOnly>.GetAsync(long key)
        {
            return await GetAsync(key);
        }
    }
}
