// auto-generated
using Zeze.Serialize;
using Zeze.Transaction;
using System.Threading.Tasks;

// ReSharper disable JoinDeclarationAndInitializer RedundantNameQualifier
namespace Zeze.Builtin.Online
{
    public sealed class tLocal : Table<string, Zeze.Builtin.Online.BLocals>, TableReadOnly<string, Zeze.Builtin.Online.BLocals, Zeze.Builtin.Online.BLocalsReadOnly>
    {
        public tLocal() : base("Zeze_Builtin_Online_tLocal")
        {
        }

        public override int Id => -63628292;
        public override bool IsMemory => true;
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

        async Task<Zeze.Builtin.Online.BLocalsReadOnly> TableReadOnly<string, Zeze.Builtin.Online.BLocals, Zeze.Builtin.Online.BLocalsReadOnly>.GetAsync(string key)
        {
            return await GetAsync(key);
        }
    }
}
