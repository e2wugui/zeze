using System;
using System.IO;
using System.Text;

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
                if (CompareBytes(exist, now) != 0)
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

        // 二进制比较，替代 Zeze.Serialize.ByteBuffer.Compare。
        private static int CompareBytes(byte[] left, byte[] right)
        {
            if (left == null || right == null)
            {
                if (left == right) // both null
                    return 0;
                if (left == null) // null is small
                    return -1;
                return 1;
            }
            if (left.Length != right.Length)
                return left.Length.CompareTo(right.Length); // shorter is small

            for (int i = 0; i < left.Length; i++)
            {
                int c = left[i].CompareTo(right[i]);
                if (c != 0)
                    return c;
            }
            return 0;
        }
    }
}
