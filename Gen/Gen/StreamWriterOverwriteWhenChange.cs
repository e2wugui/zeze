using System;
using System.IO;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Gen
{
    public sealed class StreamWriterOverwriteWhenChange : StreamWriter
    {
        public static readonly Encoding EncodingUtf8NoBom = new UTF8Encoding(false);

        public string FileName { get; }

        private MemoryStream Buffered { get; }

        public StreamWriterOverwriteWhenChange(string fileName)
            : base (new MemoryStream(), EncodingUtf8NoBom)
        {
            FileName = fileName;
            Buffered = (MemoryStream)BaseStream;
            NewLine = "\n";
        }

        protected override void Dispose(bool disposing)
        {
            Flush();

            if (File.Exists(FileName))
            {
                var exist = File.ReadAllBytes(FileName);
                var now = Buffered.ToArray();
                // 二进制比较，编码不同也认为改变。
                if (ByteBuffer.Compare(exist, now) != 0)
                {
                    Program.Print($"  Overwrite File: {FileName}", ConsoleColor.DarkYellow);
                    File.WriteAllBytes(FileName, now);
                }
            }
            else
            {
                Program.Print($"        New File: {FileName}", ConsoleColor.Green);
                File.WriteAllBytes(FileName, Buffered.ToArray());
            }
        }
    }
}
