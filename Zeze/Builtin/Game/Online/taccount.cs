// auto-generated
using Zeze.Serialize;
using Zeze.Transaction;
using System.Threading.Tasks;

namespace Zeze.Builtin.Game.Online
{
    public sealed class taccount : Table<string, Zeze.Builtin.Game.Online.BAccount>, TableReadOnly<string, Zeze.Builtin.Game.Online.BAccount, Zeze.Builtin.Game.Online.BAccountReadOnly>
    {
        public taccount() : base("Zeze_Builtin_Game_Online_taccount")
        {
        }

        public override int Id => -1097404497;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_Name = 1;
        public const int VAR_Roles = 2;
        public const int VAR_LastLoginRoleId = 3;
        public const int VAR_LastLoginVersion = 4;

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

        async Task<Zeze.Builtin.Game.Online.BAccountReadOnly> TableReadOnly<string, Zeze.Builtin.Game.Online.BAccount, Zeze.Builtin.Game.Online.BAccountReadOnly>.GetAsync(string key)
        {
            return await GetAsync(key);
        }
    }
}
