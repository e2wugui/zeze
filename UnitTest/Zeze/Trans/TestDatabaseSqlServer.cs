using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using Zeze.Transaction;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestDatabaseSqlServer
    {
        [TestInitialize]
        public void TestInit()
        {
            demo.App.Instance.Start();
        }

        [TestCleanup]
        public void TestCleanup()
        {
            demo.App.Instance.Stop();
        }


        [TestMethod]
        public void Test1()
        {
            string url = "Server=(localdb)\\MSSQLLocalDB;Integrated Security=true";
            var sqlserver = new DatabaseSqlServer(demo.App.Instance.Zeze, url);
            var table = sqlserver.OpenTable("test1");
            {
                using var trans = sqlserver.BeginTransaction();
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(1);
                    table.ITable.Remove(trans, key);
                }
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(2);
                    table.ITable.Remove(trans, key);
                }
                trans.Commit();
            }
            Assert.AreEqual(0, table.ITable.Walk(PrintRecord));
            {
                using var trans = sqlserver.BeginTransaction();
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(1);
                    ByteBuffer value = ByteBuffer.Allocate();
                    value.WriteInt(1);
                    table.ITable.Replace(trans, key, value);
                }
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(2);
                    ByteBuffer value = ByteBuffer.Allocate();
                    value.WriteInt(2);
                    table.ITable.Replace(trans, key, value);
                }
                trans.Commit();
            }
            {
                ByteBuffer key = ByteBuffer.Allocate();
                key.WriteInt(1);
                ByteBuffer value = table.ITable.Find(key);
                Assert.IsNotNull(value);
                Assert.AreEqual(1, value.ReadInt());
                Assert.IsTrue(value.ReadIndex == value.WriteIndex);
            }
            {
                ByteBuffer key = ByteBuffer.Allocate();
                key.WriteInt(2);
                ByteBuffer value = table.ITable.Find(key);
                Assert.IsNotNull(value);
                Assert.AreEqual(2, value.ReadInt());
                Assert.IsTrue(value.ReadIndex == value.WriteIndex);
            }
            Assert.AreEqual(2, table.ITable.Walk(PrintRecord));
        }

        public static bool PrintRecord(byte[] key, byte[] value)
        {
            int ikey = ByteBuffer.Wrap(key).ReadInt();
            int ivalue = ByteBuffer.Wrap(value).ReadInt();
            Console.WriteLine($"key={ikey} value={ivalue}");
            return true;
        }
    }
}
