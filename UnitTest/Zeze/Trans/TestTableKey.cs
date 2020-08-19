using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Transaction;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestTableKey
    {
        [TestMethod]
        public void Test1()
        {
            {
                TableKey tkey1 = new TableKey(1, 1);
                TableKey tkey2 = new TableKey(1, 1);

                int c = tkey1.CompareTo(tkey2);
                Assert.AreEqual(c, 0);
            }

            {
                TableKey tkey1 = new TableKey(1, 1);
                TableKey tkey2 = new TableKey(2, 1);

                int c = tkey1.CompareTo(tkey2);
                Assert.AreEqual(c, -1);
            }

            {
                TableKey tkey1 = new TableKey(1, 1L);
                TableKey tkey2 = new TableKey(1, 1L);

                int c = tkey1.CompareTo(tkey2);
                Assert.AreEqual(c, 0);
            }

            {
                TableKey tkey1 = new TableKey(1, 1L);
                TableKey tkey2 = new TableKey(1, 2L);

                int c = tkey1.CompareTo(tkey2);
                Assert.AreEqual(c, -1);
            }

            {
                TableKey tkey1 = new TableKey(1, false);
                TableKey tkey2 = new TableKey(1, true);

                int c = tkey1.CompareTo(tkey2);
                Assert.AreEqual(c, -1);
            }

            {
                TableKey tkey1 = new TableKey(1, 1);
                TableKey tkey2 = new TableKey(1, 2);

                int c = tkey1.CompareTo(tkey2);
                Assert.AreEqual(c, -1);
            }

            {
                demo.Module1.Key k1 = new demo.Module1.Key(1);
                demo.Module1.Key k2 = new demo.Module1.Key(1);

                TableKey tkey1 = new TableKey(1, k1);
                TableKey tkey2 = new TableKey(1, k2);

                int c = tkey1.CompareTo(tkey2);
                Assert.AreEqual(c, 0);
            }

            {
                demo.Module1.Key k1 = new demo.Module1.Key(1);
                demo.Module1.Key k2 = new demo.Module1.Key(2);

                TableKey tkey1 = new TableKey(1, k1);
                TableKey tkey2 = new TableKey(1, k2);

                int c = tkey1.CompareTo(tkey2);
                Assert.AreEqual(c, -1);
            }
        }
    }
}
