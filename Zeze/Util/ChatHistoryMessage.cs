using System;
using System.Collections.Generic;

namespace Zeze.Util
{
    // binary: msgsize4bytes + tag4bytes + others
    public class ChatHistoryMessage : global::Zeze.Serialize.Serializable
    {
        public const int TagDeleted = 1;
        public const int TagSeparate = 2;

        public const int TypeString = 0; // >0 user defined, <0 reserved

        public int Tag { get; set; }
        public long Id { get; set; }
        public long TimeTicks { get; set; }
        public String Sender { get; set; }
        public int Type { get; set; }
        public byte[] Content { get; set; }
        public Dictionary<int, byte[]> Properties { get; set; } = new Dictionary<int, byte[]>();
        public string ContentStr  => (Type == TypeString) ? System.Text.Encoding.UTF8.GetString(Content) : BitConverter.ToString(Content);
        public bool IsDeleted => (Tag & TagDeleted) != 0;
        public bool IsSeparate => (Tag & TagSeparate) != 0;

        public void SaveContentToFile(string path)
        {
            using System.IO.FileStream fs = System.IO.File.Create(path);
            fs.Write(this.Content, 0, this.Content.Length);
        }

        public static byte[] LoadContentFromFile(string path)
        {
            if (false == System.IO.File.Exists(path))
                return Array.Empty<byte>();

            using System.IO.FileStream fs = System.IO.File.Open(path, System.IO.FileMode.Open);
            byte[] bytes = new byte[fs.Length];
            fs.Read(bytes, 0, bytes.Length);

            return bytes;
        }

        public void Decode(global::Zeze.Serialize.ByteBuffer bb)
        {
            this.Tag = bb.ReadInt4();
            this.Id = bb.ReadLong();
            this.TimeTicks = bb.ReadLong();
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

        public void Encode(global::Zeze.Serialize.ByteBuffer bb)
        {
            bb.WriteInt4(this.Tag);
            bb.WriteLong(this.Id);
            bb.WriteLong(this.TimeTicks);
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
