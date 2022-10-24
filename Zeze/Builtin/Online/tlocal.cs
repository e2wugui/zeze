// auto-generated
using Zeze.Serialize;

namespace Zeze.Builtin.Online
{
    public sealed class tlocal : Zeze.Transaction.Table<string, Zeze.Builtin.Online.BLocals, Zeze.Builtin.Online.BLocalsReadOnly>
    {
        public tlocal() : base("Zeze_Builtin_Online_tlocal")
        {
        }

        public override int Id => -1858917951;
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
    }
}
