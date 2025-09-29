using System;
using System.Security.Cryptography;
using Zeze.Serialize;

namespace Zeze.Net
{
    public interface Codec : IDisposable
    {
        void update(byte c);
        void update(byte[] data, int off, int len);
        void flush();
    }

    /// <summary>
    /// 用来接收 Codec 结果。
    /// </summary>
    public sealed class BufferCodec : Codec
    {
        public ByteBuffer Buffer { get; } = ByteBuffer.Allocate();

        public BufferCodec()
        {
        }

        public BufferCodec(ByteBuffer buffer)
        {
            Buffer = buffer;
        }

        public void Dispose()
        {
        }

        public void flush()
        {
        }

        public void update(byte c)
        {
            Buffer.Append(c);
        }

        public void update(byte[] data, int off, int len)
        {
            Buffer.Append(data, off, len);
        }
    }

    public static class Digest
    {
        public static byte[] Md5(byte[] message)
        {
            using var md = MD5.Create();
            return md.ComputeHash(message);
        }

        public static byte[] HmacMd5(byte[] key, byte[] data, int offset, int length)
        {
            using HashAlgorithm hash = new HMACMD5(key);
            hash.TransformFinalBlock(data, offset, length);
            return hash.Hash;
        }
    }

    public sealed class Encrypt : Codec
    {
        private readonly Codec sink;
        private readonly ICryptoTransform cipher;
        private readonly byte[] _iv;

        private readonly byte[] _in = new byte[16];

        //private readonly byte[] _out = new byte[16];
        private int count;

        public Encrypt(Codec sink, byte[] key) : this(sink, Digest.Md5(key), null)
        {
        }

        public Encrypt(Codec sink, byte[] key, byte[] iv)
        {
            this.sink = sink;
            iv ??= key;
            Aes aes = Aes.Create();
            aes.Mode = CipherMode.ECB;
            cipher = aes.CreateEncryptor(key, iv);
            _iv = iv;
        }

        private void succeed()
        {
            cipher.TransformBlock(_iv, 0, 16, _iv, 0);
        }

        public void update(byte c)
        {
            if (count < 0)
            {
                sink.update(_iv[count++ + 16] ^= c);
                return;
            }
            _in[count++] = c;
            if (count < 16)
                return;
            succeed();
            for (int i = 0; i < 16; i++)
                _iv[i] ^= _in[i];
            sink.update(_iv, 0, 16);
            count = 0;
        }

        public void update(byte[] data, int off, int len)
        {
            int i = off;
            len += off;
            if (count < 0)
            {
                for (; i < len && count < 0; i++, count++)
                    sink.update(_iv[count + 16] ^= data[i]);
            }
            else if (count > 0)
            {
                for (; i < len && count < 16; i++, count++)
                    _in[count] = data[i];
                if (count < 16)
                    return;
                succeed();
                for (int j = 0; j < 16; j++)
                    _iv[j] ^= _in[j];
                sink.update(_iv, 0, 16);
                count = 0;
            }
            int nBlocks = (len - i) >> 4;
            for (int j = 0; j < nBlocks; j++)
            {
                succeed();
                for (int k = 0; k < 16; k++)
                    _iv[k] ^= data[i + j * 16 + k];
                sink.update(_iv, 0, 16);
            }
            for (i += nBlocks << 4; i < len; i++)
                _in[count++] = data[i];
        }

        public void flush()
        {
            if (count > 0)
            {
                succeed();
                for (int i = 0; i < count; i++)
                    sink.update(_iv[i] ^= _in[i]);
                count -= 16;
            }
            sink.flush();
        }

        public void Dispose()
        {
            cipher.Dispose();
            sink.Dispose();
        }
    }

    public sealed class Decrypt : Codec
    {
        private readonly Codec sink;
        private readonly ICryptoTransform cipher;
        private readonly byte[] _iv;
        private readonly byte[] _in = new byte[16];
        private readonly byte[] _out = new byte[16];
        private int count;

        public Decrypt(Codec sink, byte[] key) : this(sink, Digest.Md5(key), null)
        {
        }

        public Decrypt(Codec sink, byte[] key, byte[] iv)
        {
            this.sink = sink;
            iv ??= key;
            Aes aes = Aes.Create();
            aes.Mode = CipherMode.ECB;
            cipher = aes.CreateEncryptor(key, iv);
            _iv = iv;
        }

        private void succeed()
        {
            cipher.TransformBlock(_iv, 0, 16, _iv, 0);
        }

        public void update(byte c)
        {
            if (count < 0)
            {
                sink.update((byte)(_iv[count + 16] ^ c));
                _iv[count++ + 16] = c;
                return;
            }
            _in[count++] = c;
            if (count < 16)
                return;
            succeed();
            for (int i = 0; i < 16; i++)
            {
                _out[i] = (byte)(_iv[i] ^ _in[i]);
                _iv[i] = _in[i];
            }
            sink.update(_out, 0, 16);
            count = 0;
        }

        public void update(byte[] data, int off, int len)
        {
            int i = off;
            len += off;
            if (count < 0)
            {
                for (; i < len && count < 0; i++, count++)
                {
                    sink.update((byte)(_iv[count + 16] ^ data[i]));
                    _iv[count + 16] = data[i];
                }
            }
            else if (count > 0)
            {
                for (; i < len && count < 16; i++, count++)
                    _in[count] = data[i];
                if (count < 16)
                    return;
                succeed();
                for (int j = 0; j < 16; j++)
                {
                    _out[j] = (byte)(_iv[j] ^ _in[j]);
                    _iv[j] = _in[j];
                }
                sink.update(_out, 0, 16);
                count = 0;
            }
            int nBlocks = (len - i) >> 4;
            for (int j = 0; j < nBlocks; j++)
            {
                succeed();
                for (int k = 0; k < 16; k++)
                {
                    byte c = data[i + j * 16 + k];
                    _out[k] = (byte)(_iv[k] ^ c);
                    _iv[k] = c;
                }
                sink.update(_out, 0, 16);
            }
            for (i += nBlocks << 4; i < len; i++)
                _in[count++] = data[i];
        }

        public void flush()
        {
            if (count > 0)
            {
                succeed();
                for (int i = 0; i < count; i++)
                {
                    sink.update((byte)(_iv[i] ^ _in[i]));
                    _iv[i] = _in[i];
                }
                count -= 16;
            }
            sink.flush();
        }

        public void Dispose()
        {
            cipher.Dispose();
            sink.Dispose();
        }
    }

    // RFC2118
    public sealed class Compress : Codec
    {
        private readonly Codec sink;

        private int pos;
        private int rem;
        private readonly byte[] dict = new byte[8192];
        private readonly short[] hash = new short[65536];
        private int idx;
        private int match_idx;
        private int match_off = -1;
        private int match_len;
        private bool flushed = true;

        public Compress(Codec sink)
        {
            this.sink = sink;
            for (int i = 0; i < hash.Length; i++)
                hash[i] = -1;
        }

        private void putBits(int val, int nBits)
        {
            pos += nBits;
            rem |= val << (32 - pos);
            while (pos > 7)
            {
                sink.update((byte)(rem >> 24));
                pos -= 8;
                rem <<= 8;
            }
        }

        private void putLiteral(byte c)
        {
            if ((c & 0x80) == 0)
                putBits(c, 8);
            else
                putBits(c & 0x7f | 0x100, 9);
        }

        private void putTuple(int off, int len)
        {
            if (off < 64)
                putBits(0x3c0 | off, 10);
            else if (off < 320)
                putBits(0xe00 | (off - 64), 12);
            else
                putBits(0xc000 | (off - 320), 16);
            if (len < 4)
                putBits(0, 1);
            else if (len < 8)
                putBits(0x08 | (len & 0x03), 4);
            else if (len < 16)
                putBits(0x30 | (len & 0x07), 6);
            else if (len < 32)
                putBits(0xe0 | (len & 0x0f), 8);
            else if (len < 64)
                putBits(0x3c0 | (len & 0x1f), 10);
            else if (len < 128)
                putBits(0xf80 | (len & 0x3f), 12);
            else if (len < 256)
                putBits(0x3f00 | (len & 0x7f), 14);
            else if (len < 512)
                putBits(0xfe00 | (len & 0xff), 16);
            else if (len < 1024)
                putBits(0x3fc00 | (len & 0x1ff), 18);
            else if (len < 2048)
                putBits(0xff800 | (len & 0x3ff), 20);
            else if (len < 4096)
                putBits(0x3ff000 | (len & 0x7ff), 22);
            else if (len < 8192)
                putBits(0xffe000 | (len & 0xfff), 24);
        }

        private void _flush()
        {
            if (match_off > 0)
            {
                if (match_len == 2)
                {
                    putLiteral(dict[match_idx - 2]);
                    putLiteral(dict[match_idx - 1]);
                }
                else
                    putTuple(match_off, match_len);
                match_off = -1;
            }
            else
                putLiteral(dict[idx - 1]);
            flushed = true;
        }

        public void update(byte c)
        {
            if (idx == dict.Length)
            {
                if (!flushed)
                    _flush();
                for (int i = 0; i < hash.Length; i++)
                    hash[i] = -1;
                idx = 0;
            }
            dict[idx++] = c;
            if (flushed)
            {
                flushed = false;
                return;
            }
            int key = ((c & 0xff) << 8) | dict[idx - 2] & 0xff;
            int tmp = hash[key];
            hash[key] = (short)idx;
            if (match_off > 0)
            {
                if (dict[match_idx] == c)
                {
                    match_idx++;
                    match_len++;
                }
                else
                {
                    if (match_len == 2)
                    {
                        putLiteral(dict[match_idx - 2]);
                        putLiteral(dict[match_idx - 1]);
                    }
                    else
                        putTuple(match_off, match_len);
                    match_off = -1;
                }
            }
            else
            {
                if (tmp != -1)
                {
                    match_idx = tmp;
                    match_off = idx - tmp;
                    match_len = 2;
                }
                else
                    putLiteral(dict[idx - 2]);
            }
        }

        public void update(byte[] data, int off, int len)
        {
            len += off;
            for (int i = off; i < len; i++)
                update(data[i]);
        }

        public void flush()
        {
            if (!flushed)
            {
                _flush();
                if (pos > 0)
                    putBits(0x3c0, 10);
            }
            sink.flush();
        }

        public void Dispose()
        {
            sink.Dispose();
        }
    }

    // RFC2118
    public sealed class Decompress : Codec
    {
        private readonly Codec sink;

        private int rem;
        private int pos;
        private int off = -1;
        private int len;
        private readonly byte[] hist = new byte[8192 * 3];
        private int hPos;

        public sealed class DecompressException : Exception
        {
        }

        public Decompress(Codec sink)
        {
            this.sink = sink;
        }

        private void drain()
        {
            if (hPos >= 8192 * 2)
            {
                Buffer.BlockCopy(hist, hPos - 8192, hist, 0, 8192);
                hPos = 8192;
            }
        }

        private void copy(int dstPos, int srcPos, int length)
        {
            for (int i = 0; i < length; i++)
                hist[dstPos++] = hist[srcPos++];
        }

        private void output(byte c)
        {
            sink.update(hist[hPos++] = c);
            drain();
        }

        private void output(int off, int len)
        {
            if (hPos < off)
                throw new DecompressException();
            copy(hPos, hPos - off, len);
            sink.update(hist, hPos, len);
            hPos += len;
            drain();
        }

        private int bitCompute()
        {
            long val = (rem << (32 - pos)) & 0xffffffffL;
            if (off < 0)
            {
                if (val < 0x80000000L)
                    return 8;
                if (val < 0xc0000000L)
                    return 9;
                if (val < 0xe0000000L)
                    return 16;
                if (val < 0xf0000000L)
                    return 12;
                return 10;
            }
            if (val < 0x80000000L)
                return 1;
            if (val < 0xc0000000L)
                return 4;
            if (val < 0xe0000000L)
                return 6;
            if (val < 0xf0000000L)
                return 8;
            if (val < 0xf8000000L)
                return 10;
            if (val < 0xfc000000L)
                return 12;
            if (val < 0xfe000000L)
                return 14;
            if (val < 0xff000000L)
                return 16;
            if (val < 0xff800000L)
                return 18;
            if (val < 0xffc00000L)
                return 20;
            if (val < 0xffe00000L)
                return 22;
            if (val < 0xfff00000L)
                return 24;
            return 32;
        }

        private void process()
        {
            long val = (rem << (32 - pos)) & 0xffffffffL;
            if (off < 0)
            {
                if (val < 0x80000000L)
                {
                    output((byte)(val >> 24));
                    pos -= 8;
                }
                else if (val < 0xc0000000L)
                {
                    output((byte)((val >> 23) | 0x80));
                    pos -= 9;
                }
                else if (val < 0xe0000000L)
                {
                    off = (int)(((val >> 16) & 0x1fff) + 320);
                    pos -= 16;
                }
                else if (val < 0xf0000000L)
                {
                    off = (int)(((val >> 20) & 0xff) + 64);
                    pos -= 12;
                }
                else
                {
                    off = (int)((val >> 22) & 0x3f);
                    pos -= 10;
                    if (off == 0)
                        off = -1;
                }
            }
            else
            {
                if (val < 0x80000000L)
                {
                    len = 3;
                    pos -= 1;
                }
                else if (val < 0xc0000000L)
                {
                    len = (int)(4 | ((val >> 28) & 3));
                    pos -= 4;
                }
                else if (val < 0xe0000000L)
                {
                    len = (int)(8 | ((val >> 26) & 7));
                    pos -= 6;
                }
                else if (val < 0xf0000000L)
                {
                    len = (int)(16 | ((val >> 24) & 15));
                    pos -= 8;
                }
                else if (val < 0xf8000000L)
                {
                    len = (int)(32 | ((val >> 22) & 31));
                    pos -= 10;
                }
                else if (val < 0xfc000000L)
                {
                    len = (int)(64 | ((val >> 20) & 63));
                    pos -= 12;
                }
                else if (val < 0xfe000000L)
                {
                    len = (int)(128 | ((val >> 18) & 127));
                    pos -= 14;
                }
                else if (val < 0xff000000L)
                {
                    len = (int)(256 | ((val >> 16) & 255));
                    pos -= 16;
                }
                else if (val < 0xff800000L)
                {
                    len = (int)(512 | ((val >> 14) & 511));
                    pos -= 18;
                }
                else if (val < 0xffc00000L)
                {
                    len = (int)(1024 | ((val >> 12) & 1023));
                    pos -= 20;
                }
                else if (val < 0xffe00000L)
                {
                    len = (int)(2048 | ((val >> 10) & 2047));
                    pos -= 22;
                }
                else if (val < 0xfff00000L)
                {
                    len = (int)(4096 | ((val >> 8) & 4095));
                    pos -= 24;
                }
                else
                    throw new DecompressException();
                output(off, len);
                off = -1;
            }
        }

        public void update(byte c)
        {
            pos += 8;
            rem = (rem << 8) | (c & 0xff);
            while (pos > 24)
                process();
        }

        public void update(byte[] data, int off, int len)
        {
            len += off;
            for (int i = off; i < len; i++)
                update(data[i]);
        }

        public void flush()
        {
            while (pos >= bitCompute())
                process();
            sink.flush();
        }

        public void Dispose()
        {
            sink.Dispose();
        }
    }
}
