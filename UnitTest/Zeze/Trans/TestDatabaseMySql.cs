using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using Zeze.Transaction;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestDatabaseMySql
    {
        [TestMethod]
        public void Test1()
        {
            string url = "server=localhost;database=devtest;uid=dev;pwd=devtest12345";
            DatabaseMySql sqlserver = new DatabaseMySql(url);
            Database.Table table = sqlserver.OpenTable("test_1");
            {
                var trans = sqlserver.BeginTransaction();
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
                var trans = sqlserver.BeginTransaction();
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
