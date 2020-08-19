using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Transaction;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestLock
    {
        [TestMethod]
        public void Test1()
        {
            // 因为 TableKey 包装了 Lock 引用的查询。
            // 下面的测试没有必要了。
            // 下面的测试需要 Lock 开放构造来验证。
            // 已经验证过。
            Locks locks = Locks.Instance;

            TableKey tk1 = new TableKey(1, 1);
            TableKey tk2 = new TableKey(1, 1);

            Lock lock1 = tk1.Lock;
            Lock lock2 = tk2.Lock;

            Assert.AreEqual(lock1, lock2);

            Lock lock1ref = locks.Get(lock1);
            Assert.IsTrue(lock1ref == lock1); // first Get. self

            Lock lock2ref = locks.Get(lock2);
            Assert.IsTrue(lock2ref == lock1); // second Get. the exist

            TableKey tk3 = new TableKey(1, 2);
            Lock lock3 = tk3.Lock;
            Lock lock3ref = locks.Get(lock3);
            Assert.IsTrue(lock3ref == lock3);
            Assert.IsFalse(lock3ref == lock1);
        }
    }
}
