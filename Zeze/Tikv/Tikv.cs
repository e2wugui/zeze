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
        public int Len { get; set; }

        public GoString(string str)
        {
            var utf8 = Encoding.UTF8.GetBytes(str);
            Len = utf8.Length;
            Str = Marshal.AllocHGlobal(Len);
            Marshal.Copy(utf8, 0, Str, Len);
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

    public class Tikv
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

        private static string GetErrorString(long rc, GoSlice outerr)
        {
            if (rc >= 0)
                return string.Empty;
            int len = (int)-rc;
            return Marshal.PtrToStringUTF8(outerr.Data, len);
        }

        public static int NewClient(string pdAddrs)
        {
            using var _pdAddrs = new GoString(pdAddrs);
            using var error = new GoSlice(1024);
            int rc = NewClient(_pdAddrs, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public static int CloseClient(int clientId)
        {
            using var error = new GoSlice(1024);
            int rc = CloseClient(clientId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public static int Begin(int clientId)
        {
            using var error = new GoSlice(1024);
            int rc = Begin(clientId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public static int Commit(int txnId)
        {
            using var error = new GoSlice(1024);
            int rc = Commit(txnId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public static int Rollback(int txnId)
        {
            using var error = new GoSlice(1024);
            int rc = Rollback(txnId, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public static int Put(int txnId, Serialize.ByteBuffer key, Serialize.ByteBuffer value)
        {
            using var _key = new GoSlice(key.Bytes, key.ReadIndex, key.Size);
            using var _value = new GoSlice(value.Bytes, value.ReadIndex, value.Size);
            using var error = new GoSlice(1024);
            int rc = Put(txnId, _key, _value, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public static Serialize.ByteBuffer Get(int txnId, Serialize.ByteBuffer key)
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
                byte[] rcvalue = new byte[rc];
                Marshal.Copy(outvalue.Data, rcvalue, 0, rc);
                return Serialize.ByteBuffer.Wrap(rcvalue);
            }
        }

        public static int Delete(int txnId, Serialize.ByteBuffer key)
        {
            using var _key = new GoSlice(key.Bytes, key.ReadIndex, key.Size);
            using var error = new GoSlice(1024);
            int rc = Delete(txnId, _key, error);
            if (rc < 0)
                throw new Exception(GetErrorString(rc, error));
            return rc;
        }

        public static void Test()
        {
            var clientId = NewClient("172.21.0.229:2379");
            try
            {
                var txnId = Begin(clientId);
                try
                {
                    var key = Zeze.Serialize.ByteBuffer.Allocate(64);
                    key.WriteString("key");
                    var outvalue = Get(txnId, key);
                    Console.WriteLine("1 " + outvalue);
                    var value = Zeze.Serialize.ByteBuffer.Allocate(64);
                    value.WriteString("value");
                    Put(txnId, key, value);
                    outvalue = Get(txnId, key);
                    Console.WriteLine("2 " + outvalue);
                    Delete(txnId, key);
                    outvalue = Get(txnId, key);
                    Console.WriteLine("3 " + outvalue);
                }
                finally
                {
                    Commit(txnId);
                }
            }
            finally
            {
                CloseClient(clientId);
            }
        }
    }
}
