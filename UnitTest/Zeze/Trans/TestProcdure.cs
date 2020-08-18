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

        public bool ProcTrue()
        {
            bean.I = 123;
            Assert.AreEqual(bean.I, 123);
            return true;
        }

        public bool ProcFalse()
        {
            bean.I = 456;
            Assert.AreEqual(bean.I, 456);
            return false;
        }

        public bool ProcNest()
        {
            Assert.AreEqual(bean.I, 0);
            bean.I = 1;
            Assert.AreEqual(bean.I, 1);
            {
                bool r = new Procedure(ProcFalse).Call();
                Assert.AreEqual(r, false);
                Assert.AreEqual(bean.I, 1);
            }

            {
                bool r = new Procedure(ProcTrue).Call();
                Assert.AreEqual(r, true);
                Assert.AreEqual(bean.I, 123);
            }

            return true;
        }

        [TestMethod]
        public void Test1()
        {
            bool r = new Procedure(ProcNest).Call();
            Assert.AreEqual(r, true);
            // 最后一个 Call，事务外，bean 已经没法访问事务支持的属性了。直接访问内部变量。
            Assert.AreEqual(bean._i, 123);
        }
    }
}
