using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Transaction;
using System.Runtime.CompilerServices;
using System.Security.Cryptography;
using Zeze.Util;
using System.Threading;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestLock
    {
        [TestMethod]
        public void Test()
        {
            // DEBUG 下垃圾回收策略导致 WeakReference 不回收。
#if RELEASE
            WeakHashSet<demo.Module1.Key> keys = new WeakHashSet<demo.Module1.Key>();
            demo.Module1.Key key1 = new demo.Module1.Key(1);
            demo.Module1.Key key2 = new demo.Module1.Key(1);

            Assert.IsTrue(null == keys.get(key1));
            keys.add(key1);

            demo.Module1.Key exist1 = keys.get(key1);
            Assert.IsTrue(null != exist1);
            Assert.IsTrue(exist1 == key1);

            demo.Module1.Key exist2 = keys.get(key2);
            Assert.IsTrue(null != exist2);
            Assert.IsTrue(exist2 == key1);

            key1 = null;
            key2 = null;
            exist1 = null;
            exist2 = null;

            demo.Module1.Key k4 = new demo.Module1.Key(1);
            WeakReference<demo.Module1.Key> wref = new WeakReference<demo.Module1.Key>(k4);
            k4 = null;
            for (int i = 0; i < 10; ++i)
            {
                System.GC.Collect();
                GC.WaitForFullGCComplete();
                GC.WaitForPendingFinalizers();
                Thread.Sleep(200);

                if (false == wref.TryGetTarget(out var notusedk4ref))
                    break;
            }

            demo.Module1.Key k4ref;
            Assert.IsTrue(false == wref.TryGetTarget(out k4ref));
            Assert.IsTrue(null == k4ref);

            demo.Module1.Key key3 = new demo.Module1.Key(1);
            Console.WriteLine("test: is null.");
            Assert.IsTrue(null == keys.get(key3));
#endif
        }

        [TestMethod]
        public void Test1()
        {
            Locks locks = Locks.Instance;

            TableKey tk1 = new TableKey(1, 1);
            TableKey tk2 = new TableKey(1, 1);

            Lockey lock1 = new Lockey(tk1);
            Lockey lock2 = new Lockey(tk2);

            Assert.AreEqual(lock1, lock2);

            Lockey lock1ref = locks.Get(lock1);
            Assert.IsTrue(lock1ref == lock1); // first Get. self

            Lockey lock2ref = locks.Get(lock2);
            Assert.IsTrue(lock2ref == lock1); // second Get. the exist

            TableKey tk3 = new TableKey(1, 2);
            Lockey lock3 = new Lockey(tk3);
            Lockey lock3ref = locks.Get(lock3);
            Assert.IsTrue(lock3ref == lock3);
            Assert.IsFalse(lock3ref == lock1);
        }
    }
}
