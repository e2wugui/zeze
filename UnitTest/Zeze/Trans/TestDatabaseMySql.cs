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
            sqlserver.Flush(null, () =>
            {
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(1);
                    table.Remove(key);
                }
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(2);
                    table.Remove(key);
                }
            }
            );
            Walker walker = new Walker();
            table.Walk(walker);
            Assert.AreEqual(0, walker.count);
            sqlserver.Flush(null, () =>
            {
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(1);
                    ByteBuffer value = ByteBuffer.Allocate();
                    value.WriteInt(1);
                    table.Replace(key, value);
                }
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(2);
                    ByteBuffer value = ByteBuffer.Allocate();
                    value.WriteInt(2);
                    table.Replace(key, value);
                }
            }
            );
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
            walker.count = 0;
            table.Walk(walker);
            Assert.AreEqual(2, walker.count);
        }

        class Walker : Database.Table.IWalk
        {
            public int count = 0;

            public bool OnRecord(byte[] key, byte[] value)
            {
                int ikey = ByteBuffer.Wrap(key).ReadInt();
                int ivalue = ByteBuffer.Wrap(value).ReadInt();
                Console.WriteLine($"key={ikey} value={ivalue}");
                ++count;
                return true;
            }
        }
    }
}
