// auto-generated
using Zeze.Serialize;
using Zeze.Transaction;
using System.Threading.Tasks;

// ReSharper disable JoinDeclarationAndInitializer RedundantNameQualifier
namespace Zeze.Builtin.Game.Online
{
    public sealed class tVersion : Table<long, Zeze.Builtin.Game.Online.BVersion>, TableReadOnly<long, Zeze.Builtin.Game.Online.BVersion, Zeze.Builtin.Game.Online.BVersionReadOnly>
    {
        public tVersion() : base("Zeze_Builtin_Game_Online_tVersion")
        {
        }

        public override int Id => -2147479904;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_LoginVersion = 1;
        public const int VAR_ReliableNotifyMark = 2;
        public const int VAR_ReliableNotifyConfirmIndex = 3;
        public const int VAR_ReliableNotifyIndex = 4;
        public const int VAR_ServerId = 5;
        public const int VAR_LogoutVersion = 6;

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

        async Task<Zeze.Builtin.Game.Online.BVersionReadOnly> TableReadOnly<long, Zeze.Builtin.Game.Online.BVersion, Zeze.Builtin.Game.Online.BVersionReadOnly>.GetAsync(long key)
        {
            return await GetAsync(key);
        }
    }
}
