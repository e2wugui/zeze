using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Gen
{
    public class StreamWriterOverwriteWhenChange : StreamWriter
    {
        public static Encoding EncodingUtf8NoBom = new UTF8Encoding(false);

        public string FileName { get; }

        private MemoryStream Buffered { get; }

        public StreamWriterOverwriteWhenChange(string file)
            : base (new MemoryStream(), EncodingUtf8NoBom)
        {
            FileName = Path.GetFullPath(file);
            Buffered = (MemoryStream)base.BaseStream;
            NewLine = "\n";
        }

        protected override void Dispose(bool disposing)
        {
            base.Flush();

            if (File.Exists(FileName))
            {
                var exist = File.ReadAllBytes(FileName);
                var now = Buffered.ToArray();
                // 二进制比较，编码不同也认为改变。
                if (Zeze.Serialize.ByteBuffer.Compare(exist, now) != 0)
                {
                    Program.Print($"Overwrite File: {FileName}");
                    File.WriteAllBytes(FileName, now);
                }
            }
            else
            {
                Program.Print($"      New File: {FileName}");
                File.WriteAllBytes(FileName, Buffered.ToArray());
            }
        }
    }
}
