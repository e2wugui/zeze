using System;
using System.Collections.Generic;
using System.Data;
using System.Text;

namespace Zeze.Util
{
    // binary: msgsize4bytes + tag4bytes + others
    public class ChatHistoryMessage : Zeze.Serialize.Serializable
    {
        public const int TagDeleted = 1;
        public const int TagRefence = 2;

        public const int TypeString = 0; // >0 user defined, <0 reserved

        public int Tag { get; set; }
        public long Id { get; set; }
        public long Time { get; set; }
        public String Sender { get; set; }
        public int Type { get; set; }
        public byte[] Content { get; set; }
        public Dictionary<int, byte[]> Properties { get; set; } = new Dictionary<int, byte[]>();
        public string ContentStr  => (Type == TypeString) ? System.Text.Encoding.UTF8.GetString(Content) : BitConverter.ToString(Content);
        public bool IsDeleted => (Tag & 1) != 0;

        public void Decode(Zeze.Serialize.ByteBuffer bb)
        {
            this.Tag = bb.ReadInt4();
            this.Id = bb.ReadLong();
            this.Time = bb.ReadLong();
            this.Sender = bb.ReadString();
            this.Type = bb.ReadInt();
            this.Content = bb.ReadBytes();

            int propertiesSize = bb.ReadInt();
            for (int i = 0; i < propertiesSize; ++i)
            {
                int key = bb.ReadInt();
                byte[] value = bb.ReadBytes();
                Properties.Add(key, value);
            }
        }

        public void Encode(Zeze.Serialize.ByteBuffer bb)
        {
            bb.WriteInt4(this.Tag);
            bb.WriteLong(this.Id);
            bb.WriteLong(this.Time);
            bb.WriteString(this.Sender);
            bb.WriteInt(this.Type);
            bb.WriteBytes(this.Content);

            bb.WriteInt(Properties.Count);
            foreach (KeyValuePair<int, byte[]> pair in Properties)
            {
                bb.WriteInt(pair.Key);
                bb.WriteBytes(pair.Value);
            }
        }

        public int SizeHint()
        {
            return 4 + 9 + 9 + 128 + 5 + 5 + Content.Length; // tag + id + time + sender + type + contentsize + content
        }
    }
}
