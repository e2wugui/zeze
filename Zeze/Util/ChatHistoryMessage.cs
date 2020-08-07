using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
    public class ChatHistoryMessage : Zeze.Serialize.Serializable
    {
        public const int TypeString = 0;

        public long Id { get; set; }
        public uint Tag { get; set; }
        public long Time { get; set; }
        public String Sender { get; set; }
        public int Type { get; set; }
        public byte[] Content { get; set; }

        public void Decode(Zeze.Serialize.ByteBuffer bb)
        {
            throw new NotImplementedException();
        }

        public void Encode(Zeze.Serialize.ByteBuffer bb)
        {
            throw new NotImplementedException();
        }

        public string GetStringContent()
        {
            if (IsDeleted() || Type != TypeString)
                return "";
            return System.Text.Encoding.UTF8.GetString(Content);
        }

        public bool IsDeleted()
        {
            return (Tag & 1) != 0; // maybe more tag
        }
    }
}
