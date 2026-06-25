namespace Zeze.Net
{
    public class FamilyClass
    {
        // FamilyClass 定义的枚举，这个是Protocol的属性，目前没有开放。
        public const int Protocol = 2;
        public const int Request = 1;
        public const int Response = 0;
        public const int BitResultCode = 1 << 5; // 压缩ResultCode。设置这个Bit表示有ResultCode。
        public const int FamilyClassMask = BitResultCode - 1;

        /*
        Protocol.Encode(ByteBuffer bb)
        {
            var compress = this.FamilyClass;
            if (ResultCode != 0)
                compress |= Zeze.Net.FamilyClass.BitResultCode;
            bb.WriteInt(compress);
            if (ResultCode != 0)
                bb.WriteLong(ResultCode);
            Argument.Encode(bb);
        }
        Protocol.Decode(ByteBuffer bb)
        {
            var compress = bb.ReadInt();
            FamilyClass = compress & Zeze.Net.FamilyClass.FamilyClassMask;
            ResultCode = ((compress & Zeze.Net.FamilyClass.BitResultCode) != 0) ? bb.ReadLong() : 0;
            Argument.Decode(bb);
        }
        Rpc.Encode(ByteBuffer bb)
        {
            // skip value of this.FamilyClass
            var compress = IsRequest ? Zeze.Net.FamilyClass.Request : Zeze.Net.FamilyClass.Response;
            if (ResultCode != 0)
                compress |= Zeze.Net.FamilyClass.BitResultCode;
            bb.WriteInt(compress);
            if (ResultCode != 0)
                bb.WriteLong(ResultCode);
            bb.WriteLong(SessionId);
            if (IsRequest)
                Argument.Encode(bb);
            else
                Result.Encode(bb);
        }
        Rpc.Decode(ByteBuffer bb)
        {
            var compress = bb.ReadInt();
            FamilyClass = compress & Zeze.Net.FamilyClass.FamilyClassMask;
            IsRequest = FamilyClass == Zeze.Net.FamilyClass.Request;
            ResultCode = ((compress & Zeze.Net.FamilyClass.BitResultCode) != 0) ? bb.ReadLong() : 0;
            SessionId = bb.ReadLong();
            if (IsRequest)
                Argument.Decode(bb);
            else
                Result.Decode(bb);
        }
        */
    }
}
