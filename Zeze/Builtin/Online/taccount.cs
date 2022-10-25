// auto-generated
using Zeze.Serialize;
using Zeze.Transaction;
using System.Threading.Tasks;

namespace Zeze.Builtin.Online
{
    public sealed class taccount : Table<string, Zeze.Builtin.Online.BAccount>, TableReadOnly<string, Zeze.Builtin.Online.BAccount, Zeze.Builtin.Online.BAccountReadOnly>
    {
        public taccount() : base("Zeze_Builtin_Online_taccount")
        {
        }

        public override int Id => 1419906985;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_LastLoginVersion = 1;

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

        async Task<Zeze.Builtin.Online.BAccountReadOnly> TableReadOnly<string, Zeze.Builtin.Online.BAccount, Zeze.Builtin.Online.BAccountReadOnly>.GetAsync(string key)
        {
            return await GetAsync(key);
        }
    }
}
