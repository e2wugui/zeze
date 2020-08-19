using System;
using System.Collections.Generic;
using System.Text;

using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using Zeze.Transaction;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestTable
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        Table1 table1 = new Table1("table1");
        Table1 table2 = new Table1("table2");

        [TestMethod]
        public void Test3()
        {
            Assert.IsTrue(new Procedure(ProcGetOrAdd).Call());
        }

        bool ProcGetOrAdd()
        {
            return true;
        }

        [TestMethod]
        public void Test1()
        {
            Assert.IsTrue(new Procedure(ProcGet11).Call());
            Assert.IsTrue(new Procedure(ProcGet12).Call());
        }

        [TestMethod]
        public void Test2()
        {
            Assert.IsTrue(new Procedure(ProcGet21).Call());
            Assert.IsTrue(new Procedure(ProcGet22).Call());
        }

        bool ProcGet21()
        {
            ProcGet11();

            Assert.IsNull(table2.Get(1));
            demo.Module1.Value v = new demo.Module1.Value();

            v.Int1 = 1;
            v.Long2 = 2;
            v.String3 = "3";
            v.Bool4 = true;
            v.Short5 = 5;
            v.Float6 = 6;
            v.Double7 = 7;
            v.List9.Add(new demo.Bean1());
            v.Set10.Add(10);
            v.Map11.Add(1, new demo.Module2.Value());
            v.Bean12.Int1 = 12;
            v.Byte13 = 13;

            table2.Put(1, v);
            Assert.IsTrue(v == table2.Get(1));
            return true;
        }

        bool ProcGet22()
        {
            ProcGet12();

            var v = table2.Get(1);
            Assert.IsNotNull(v);

            Assert.IsTrue(v.Int1 == 1);
            Assert.IsTrue(v.Long2 == 2);
            Assert.IsTrue(v.String3.Equals("3"));
            Assert.IsTrue(v.Bool4);
            Assert.IsTrue(v.Short5 == 5);
            Assert.IsTrue(v.Float6 == 6);
            Assert.IsTrue(v.Double7 == 7);
            Assert.IsTrue(v.List9.Count == 1);
            Assert.IsTrue(v.Set10.Contains(10));
            Assert.IsTrue(v.Set10.Count == 1);
            Assert.IsTrue(v.Map11.Count == 1);
            Assert.IsTrue(v.Bean12.Int1 == 12);
            Assert.IsTrue(v.Byte13 == 13);

            table2.Remove(1);
            Assert.IsNull(table2.Get(1));
            return true;
        }

        bool ProcGet11()
        {
            Assert.IsNull(table1.Get(1));
            demo.Module1.Value v = new demo.Module1.Value();

            v.Int1 = 1;
            v.Long2 = 2;
            v.String3 = "3";
            v.Bool4 = true;
            v.Short5 = 5;
            v.Float6 = 6;
            v.Double7 = 7;
            v.List9.Add(new demo.Bean1());
            v.Set10.Add(10);
            v.Map11.Add(1, new demo.Module2.Value());
            v.Bean12.Int1 = 12;
            v.Byte13 = 13;

            table1.Put(1, v);
            Assert.IsTrue(v == table1.Get(1));
            return true;
        }

        bool ProcGet12()
        {
            var v = table1.Get(1);
            Assert.IsNotNull(v);

            Assert.IsTrue(v.Int1 == 1);
            Assert.IsTrue(v.Long2 == 2);
            Assert.IsTrue(v.String3.Equals("3"));
            Assert.IsTrue(v.Bool4);
            Assert.IsTrue(v.Short5 == 5);
            Assert.IsTrue(v.Float6 == 6);
            Assert.IsTrue(v.Double7 == 7);
            Assert.IsTrue(v.List9.Count == 1);
            Assert.IsTrue(v.Set10.Contains(10));
            Assert.IsTrue(v.Set10.Count == 1);
            Assert.IsTrue(v.Map11.Count == 1);
            Assert.IsTrue(v.Bean12.Int1 == 12);
            Assert.IsTrue(v.Byte13 == 13);

            table1.Remove(1);
            Assert.IsNull(table1.Get(1));
            return true;
        }
    }

    class Table1 : Table<long, demo.Module1.Value>
    {
        public Table1(string tablename) : base(tablename)
        {

        }

        public override long DecodeKey(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }

        public override ByteBuffer EncodeKey(long key)
        {
            throw new NotImplementedException();
        }
    }
}
