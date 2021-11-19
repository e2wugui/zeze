using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using Zeze.Transaction;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestDatabaseRocksDb
    {
        [TestInitialize]
        public void TestInit()
        {
            // rocksdb can not work with global.
            var cfg = global::Zeze.Config.Load();
            cfg.GlobalCacheManagerHostNameOrAddress = "";
            demo.App.Instance.Start(cfg);
        }

        [TestCleanup]
        public void TestCleanup()
        {
            demo.App.Instance.Stop();
        }

        [TestMethod]
        public void Test()
        {
            // RocksDbSharp 没有Transaction_Families_Open的接口包装，暂不做这个测试了。
            if (demo.App.Instance.Zeze.Config.GlobalCacheManagerHostNameOrAddress.Length == 0)
                return;

            string url = "./rocksdb";
            var db = new DatabaseRocksDb(demo.App.Instance.Zeze, url);
            Database.Table table = db.OpenTable("test_1");
            {
                using var trans = db.BeginTransaction();
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(1);
                    table.Remove(trans, key);
                }
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(2);
                    table.Remove(trans, key);
                }
                trans.Commit();
            }
            Assert.AreEqual(0, table.Walk(PrintRecord));
            {
                using var trans = db.BeginTransaction();
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(1);
                    ByteBuffer value = ByteBuffer.Allocate();
                    value.WriteInt(1);
                    table.Replace(trans, key, value);
                }
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(2);
                    ByteBuffer value = ByteBuffer.Allocate();
                    value.WriteInt(2);
                    table.Replace(trans, key, value);
                }
                trans.Commit();
            }
            {
                ByteBuffer key = ByteBuffer.Allocate();
                key.WriteInt(1);
                ByteBuffer value = table.Find(key);
                Assert.IsNotNull(value);
                Assert.AreEqual(1, value.ReadInt());
                Assert.IsTrue(value.ReadIndex == value.WriteIndex);
            }
            {
                ByteBuffer key = ByteBuffer.Allocate();
                key.WriteInt(2);
                ByteBuffer value = table.Find(key);
                Assert.IsNotNull(value);
                Assert.AreEqual(2, value.ReadInt());
                Assert.IsTrue(value.ReadIndex == value.WriteIndex);
            }
            Assert.AreEqual(2, table.Walk(PrintRecord));
        }

        public bool PrintRecord(byte[] key, byte[] value)
        {
            int ikey = ByteBuffer.Wrap(key).ReadInt();
            int ivalue = ByteBuffer.Wrap(value).ReadInt();
            Console.WriteLine($"key={ikey} value={ivalue}");
            return true;
        }
    }
}
