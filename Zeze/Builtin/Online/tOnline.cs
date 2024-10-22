// auto-generated
using Zeze.Serialize;
using Zeze.Transaction;
using System.Threading.Tasks;

// ReSharper disable JoinDeclarationAndInitializer RedundantNameQualifier
namespace Zeze.Builtin.Online
{
    public sealed class tOnline : Table<string, Zeze.Builtin.Online.BOnlines>, TableReadOnly<string, Zeze.Builtin.Online.BOnlines, Zeze.Builtin.Online.BOnlinesReadOnly>
    {
        public tOnline() : base("Zeze_Builtin_Online_tOnline")
        {
        }

        public override int Id => -1967867653;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_Logins = 1;

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

        async Task<Zeze.Builtin.Online.BOnlinesReadOnly> TableReadOnly<string, Zeze.Builtin.Online.BOnlines, Zeze.Builtin.Online.BOnlinesReadOnly>.GetAsync(string key)
        {
            return await GetAsync(key);
        }
    }
}
