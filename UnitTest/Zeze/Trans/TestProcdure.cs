using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using Zeze.Transaction;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestProcdure
    {
        TestBegin.MyBean bean = new TestBegin.MyBean();

        public int ProcTrue()
        {
            bean.I = 123;
            Assert.AreEqual(bean.I, 123);
            return Procedure.Success;
        }

        public int ProcFalse()
        {
            bean.I = 456;
            Assert.AreEqual(bean.I, 456);
            return Procedure.Unknown;
        }

        public int ProcNest()
        {
            Assert.AreEqual(bean.I, 0);
            bean.I = 1;
            Assert.AreEqual(bean.I, 1);
            {
                int r = new Procedure(ProcFalse).Call();
                Assert.IsTrue(r != Procedure.Success);
                Assert.AreEqual(bean.I, 1);
            }

            {
                int r = new Procedure(ProcTrue).Call();
                Assert.IsTrue(r == Procedure.Success);
                Assert.AreEqual(bean.I, 123);
            }

            return Procedure.Success;
        }

        [TestMethod]
        public void Test1()
        {
            TableKey root = new TableKey(1, 1);
            bean.InitTableKey(root);
            int r = new Procedure(ProcNest).Call();
            Assert.IsTrue(r == Procedure.Success);
            // 最后一个 Call，事务外，bean 已经没法访问事务支持的属性了。直接访问内部变量。
            Assert.AreEqual(bean._i, 123);
        }
    }
}
