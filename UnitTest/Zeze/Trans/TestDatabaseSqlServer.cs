﻿using System;
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
        [TestMethod]
        public void Test1()
        {
            string url = "Server=(localdb)\\MSSQLLocalDB;Integrated Security=true";
            DatabaseSqlServer sqlserver = new DatabaseSqlServer(url);
            Database.Table table = sqlserver.OpenTable("test1");
            sqlserver.Flush(null,
                () =>
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
            Assert.AreEqual(0, table.Walk(PrintRecord));
            sqlserver.Flush(null,
                () =>
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
