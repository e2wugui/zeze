using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Tikv
{
    public struct GoString : IDisposable
    {
        public IntPtr Str { get; set; }
        public long Len { get; set; }

        public GoString(string str)
        {
            var utf8 = Encoding.UTF8.GetBytes(str);
            Len = utf8.Length;
            Str = Marshal.AllocHGlobal(utf8.Length);
            Marshal.Copy(utf8, 0, Str, utf8.Length);
        }

        public void Dispose()
        {
            Marshal.FreeHGlobal(Str);
        }
    }

    public struct GoSlice : IDisposable
    {
        public IntPtr Data { get; set; }
        public long Len { get; set; }
        public long Cap { get; set; }

        public GoSlice(byte[] bytes, int offset, int size, bool oneByte)
        {
            // tikv不能设置长度为0的数组，多分配一个字节。用于Tikv.Put. Tikv.Get返回时去掉。
            var allocate = size;
            if (oneByte)
                allocate++;
            Len = allocate;
            Cap = allocate;

            Data = Marshal.AllocHGlobal(allocate);
            Marshal.Copy(bytes, offset, Data, size);
        }

        public GoSlice(byte [] bytes, int offset, int size)
        {
            Len = size;
            Cap = size;
            Data = Marshal.AllocHGlobal(size);
            Marshal.Copy(bytes, offset, Data, size);
        }

        public GoSlice(int allcateOnly)
        {
            Len = allcateOnly; // 如果是0，传入go时是空的。本来还以为cap此时能被用上。
            Cap = allcateOnly;
            Data = Marshal.AllocHGlobal(allcateOnly);
        }

        public void Dispose()
        {
            Marshal.FreeHGlobal(Data);
        }
    }

    public abstract class Tikv
    {
        public static readonly Tikv Driver = Create();

        private static Tikv Create()
        {
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux))
                return new TikvLinux();
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
                return new TikvWindows();
            /*
            if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
                return TikvLinux();
            */
            throw new Exception("unknown platform.");
        }

        public abstract int NewClient(string pdAddrs);
        public abstract void CloseClient(int clientId);
        public abstract int Begin(int clientId);
        public abstract void Commit(int txnId);
        public abstract void Rollback(int txnId);
        public abstract void Put(int txnId, Serialize.ByteBuffer key, Serialize.ByteBuffer value);
        public abstract Serialize.ByteBuffer Get(int txnId, Serialize.ByteBuffer key);
        public abstract void Delete(int txnId, Serialize.ByteBuffer key);

        protected string GetErrorString(long rc, GoSlice outerr)
        {
            if (rc >= 0)
                return string.Empty;
            int len = (int)-rc;
            return Marshal.PtrToStringUTF8(outerr.Data, len);
        }
    }

    // 不同平台复制代码，比引入接口少调用一次函数。
    public sealed class TikvWindows : Tikv
    {
        [DllImport("tikv.dll")]
        private static extern int NewClient(GoString pdAddrs, GoSlice outerr);
        [DllImport("tikv.dll")]
        private static extern int CloseClient(int clientId, GoSlice outerr);
        [DllImport("tikv.dll")]
        private static extern int Begin(int clientId, GoSlice outerr);
        [DllImport("tikv.dll")]
        private static extern int Commit(int txnId, GoSlice outerr);
        [DllImport("tikv.dll")]
        private static extern int Rollback(int txnId, GoSlice outerr);
        [DllImport("tikv.dll")]
        private static extern int Put(int txnId, GoSlice key, GoSlice value, GoSlice outerr);
        [DllImport("tikv.dll")]
        private static extern int Get(int txnId, GoSlice key, GoSlice outvalue, GoSlice outerr);
        [DllImport("tikv.dll")]
        private static extern int Delete(int txnId, GoSlice key, GoSlice outerr);

        public override int NewClient(string pdAddrs)
        {
            using var _pdAddrs = new GoString(pdAddrs);
            using var error = new GoSlice(1024);
            int rc = NewClient(_pdAddrs, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public override void CloseClient(int clientId)
        {
            using var error = new GoSlice(1024);
            int rc = CloseClient(clientId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override int Begin(int clientId)
        {
            using var error = new GoSlice(1024);
            int rc = Begin(clientId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public override void Commit(int txnId)
        {
            using var error = new GoSlice(1024);
            int rc = Commit(txnId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override void Rollback(int txnId)
        {
            using var error = new GoSlice(1024);
            int rc = Rollback(txnId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override void Put(int txnId, Serialize.ByteBuffer key, Serialize.ByteBuffer value)
        {
            using var _key = new GoSlice(key.Bytes, key.ReadIndex, key.Size);
            // tikv不能设置长度为0的数组
            using var _value = new GoSlice(value.Bytes, value.ReadIndex, value.Size, true);
            using var error = new GoSlice(1024);
            int rc = Put(txnId, _key, _value, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override Serialize.ByteBuffer Get(int txnId, Serialize.ByteBuffer key)
        {
            int outValueBufferLen = 64 * 1024;
            while (true)
            {
                using var _key = new GoSlice(key.Bytes, key.ReadIndex, key.Size);
                using var error = new GoSlice(1024);
                using var outvalue = new GoSlice(outValueBufferLen);
                int rc = Get(txnId, _key, outvalue, error);
                if (rc < 0)
                {
                    var str = GetErrorString(rc, error);
                    if (str.Equals("key not exist")) // 这是tikv clieng.go 返回的错误。
                        return null;
                    if (str.Equals("ZezeSpecialError: value is nil."))
                        return null;
                    var strBufferNotEnough = "ZezeSpecialError: outvalue buffer not enough. BufferNeed=";
                    if (str.StartsWith(strBufferNotEnough))
                    {
                        outValueBufferLen = int.Parse(str.Substring(strBufferNotEnough.Length));
                        continue;
                    }
                    throw new Exception(str);
                }
                if (rc > 0) // tikv不能设置长度为0的数组。必须大于0. see Put. 这里判断一下吧。
                    rc--;
                byte[] rcvalue = new byte[rc];
                Marshal.Copy(outvalue.Data, rcvalue, 0, rcvalue.Length);
                return Serialize.ByteBuffer.Wrap(rcvalue);
            }
        }

        public override void Delete(int txnId, Serialize.ByteBuffer key)
        {
            using var _key = new GoSlice(key.Bytes, key.ReadIndex, key.Size);
            using var error = new GoSlice(1024);
            int rc = Delete(txnId, _key, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }
    }

    public sealed class TikvLinux : Tikv
    {
        [DllImport("tikv.so")]
        private static extern int NewClient(GoString pdAddrs, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern int CloseClient(int clientId, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern int Begin(int clientId, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern int Commit(int txnId, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern int Rollback(int txnId, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern int Put(int txnId, GoSlice key, GoSlice value, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern int Get(int txnId, GoSlice key, GoSlice outvalue, GoSlice outerr);
        [DllImport("tikv.so")]
        private static extern int Delete(int txnId, GoSlice key, GoSlice outerr);

        public override int NewClient(string pdAddrs)
        {
            using var _pdAddrs = new GoString(pdAddrs);
            using var error = new GoSlice(1024);
            int rc = NewClient(_pdAddrs, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public override void CloseClient(int clientId)
        {
            using var error = new GoSlice(1024);
            int rc = CloseClient(clientId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override int Begin(int clientId)
        {
            using var error = new GoSlice(1024);
            int rc = Begin(clientId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public override void Commit(int txnId)
        {
            using var error = new GoSlice(1024);
            int rc = Commit(txnId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override void Rollback(int txnId)
        {
            using var error = new GoSlice(1024);
            int rc = Rollback(txnId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override void Put(int txnId, Serialize.ByteBuffer key, Serialize.ByteBuffer value)
        {
            using var _key = new GoSlice(key.Bytes, key.ReadIndex, key.Size);
            // tikv不能设置长度为0的数组
            using var _value = new GoSlice(value.Bytes, value.ReadIndex, value.Size, true);
            using var error = new GoSlice(1024);
            int rc = Put(txnId, _key, _value, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }

        public override Serialize.ByteBuffer Get(int txnId, Serialize.ByteBuffer key)
        {
            int outValueBufferLen = 64 * 1024;
            while (true)
            {
                using var _key = new GoSlice(key.Bytes, key.ReadIndex, key.Size);
                using var error = new GoSlice(1024);
                using var outvalue = new GoSlice(outValueBufferLen);
                int rc = Get(txnId, _key, outvalue, error);
                if (rc < 0)
                {
                    var str = GetErrorString(rc, error);
                    if (str.Equals("key not exist")) // 这是tikv clieng.go 返回的错误。
                        return null;
                    if (str.Equals("ZezeSpecialError: value is nil."))
                        return null;
                    var strBufferNotEnough = "ZezeSpecialError: outvalue buffer not enough. BufferNeed=";
                    if (str.StartsWith(strBufferNotEnough))
                    {
                        outValueBufferLen = int.Parse(str.Substring(strBufferNotEnough.Length));
                        continue;
                    }
                    throw new Exception(str);
                }
                if (rc > 0) // tikv不能设置长度为0的数组。必须大于0. see Put. 这里判断一下吧。
                    rc--;
                byte[] rcvalue = new byte[rc];
                Marshal.Copy(outvalue.Data, rcvalue, 0, rcvalue.Length);
                return Serialize.ByteBuffer.Wrap(rcvalue);
            }
        }

        public override void Delete(int txnId, Serialize.ByteBuffer key)
        {
            using var _key = new GoSlice(key.Bytes, key.ReadIndex, key.Size);
            using var error = new GoSlice(1024);
            int rc = Delete(txnId, _key, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
        }
    }
}
