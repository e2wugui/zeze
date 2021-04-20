using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Tikv
{
    public class Test
    {
        public static readonly string URL = "172.21.15.68:2379";

        public static void RunWrap()
        {
            Console.WriteLine("RunWrap");

            var tikvDb = new DatabaseTikv(URL);
            var table = tikvDb.OpenTable("_testtable_");
            var key = Zeze.Serialize.ByteBuffer.Allocate(64);
            key.WriteString("key");
            var value = Zeze.Serialize.ByteBuffer.Allocate(64);
            key.WriteString("value");

            var outvalue = table.Find(key);
            Console.WriteLine("Find1 " + outvalue);
            tikvDb.Flush(null, () =>
            {
                table.Replace(key, value);
            });
            outvalue = table.Find(key);
            Console.WriteLine("Find2 " + outvalue);
            tikvDb.Flush(null, () =>
            {
                table.Remove(key);
            });
            outvalue = table.Find(key);
            Console.WriteLine("Find3 " + outvalue);
        }

        public static void RunBasic()
        {
            Console.WriteLine("RunBasic");

            var clientId = Tikv.NewClient(URL);
            try
            {
                var txnId = Tikv.Begin(clientId);
                try
                {
                    var key = Zeze.Serialize.ByteBuffer.Allocate(64);
                    key.WriteString("key");
                    var outvalue = Tikv.Get(txnId, key);
                    Console.WriteLine("1 " + outvalue);
                    var value = Zeze.Serialize.ByteBuffer.Allocate(64);
                    value.WriteString("value");
                    Tikv.Put(txnId, key, value);
                    outvalue = Tikv.Get(txnId, key);
                    Console.WriteLine("2 " + outvalue);
                    Tikv.Delete(txnId, key);
                    outvalue = Tikv.Get(txnId, key);
                    Console.WriteLine("3 " + outvalue);
                    Tikv.Commit(txnId);
                }
                catch (Exception)
                {
                    Tikv.Rollback(txnId);
                }
            }
            finally
            {
                Tikv.CloseClient(clientId);
            }
        }

        public static void Run()
        {
            RunBasic();
            RunWrap();
        }
    }
}
