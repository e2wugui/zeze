﻿using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using Zeze.Transaction;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestDatabaseMySql
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
        public async Task Test1()
        {
            string url = "server=localhost;database=devtest;uid=dev;pwd=devtest12345";
            var databaseConf = new global::Zeze.Config.DatabaseConf()
            {
                DatabaseType = global::Zeze.Config.DbType.MySql,
                DatabaseUrl = url,
            };
            var sqlserver = new DatabaseMySql(demo.App.Instance.Zeze, databaseConf);
            var table = sqlserver.OpenTable("test_1");
            {
                using var trans = sqlserver.BeginTransaction();
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(1);
                    table.ITable.Remove(trans.ITransaction, key);
                }
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(2);
                    table.ITable.Remove(trans.ITransaction, key);
                }
                await trans.CommitAsync();
            }
            Assert.AreEqual(0, table.ITable.Walk(PrintRecord));
            {
                using var trans = sqlserver.BeginTransaction();
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(1);
                    ByteBuffer value = ByteBuffer.Allocate();
                    value.WriteInt(1);
                    table.ITable.Replace(trans.ITransaction, key, value);
                }
                {
                    ByteBuffer key = ByteBuffer.Allocate();
                    key.WriteInt(2);
                    ByteBuffer value = ByteBuffer.Allocate();
                    value.WriteInt(2);
                    table.ITable.Replace(trans.ITransaction, key, value);
                }
                await trans.CommitAsync();
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
