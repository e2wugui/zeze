using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Transaction;
using Zeze.Util;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Concurrent;

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
            var locks = new Locks();

            var tk1 = new TableKey("1", 1);
            var tk2 = new TableKey("1", 1);

            var lock1 = new Lockey(tk1);
            var lock2 = new Lockey(tk2);

            Assert.AreEqual(lock1, lock2);

            var lock1ref = locks.Get(lock1);
            Assert.IsTrue(lock1ref == lock1); // first Get. self

            var lock2ref = locks.Get(lock2);
            Assert.IsTrue(lock2ref == lock1); // second Get. the exist

            var tk3 = new TableKey("1", 2);
            var lock3 = new Lockey(tk3);
            var lock3ref = locks.Get(lock3);
            Assert.IsTrue(lock3ref == lock3);
            Assert.IsFalse(lock3ref == lock1);
        }

        [TestMethod]
        public void TestRecursion1()
        {
            /*
            TableKey tkey = new TableKey(1, 1);
            Lockey lockey = Locks.Instance.Get(tkey);
            lockey.EnterWriteLock();
            lockey.EnterReadLock();
            lockey.ExitReadLock();
            lockey.ExitWriteLock();
            */
        }

        [TestMethod]
        public void TestRecursion2()
        {
            /*
            TableKey tkey = new TableKey(1, 1);
            Lockey lockey = Locks.Instance.Get(tkey);
            lockey.EnterReadLock();
            lockey.EnterWriteLock();
            lockey.ExitWriteLock();
            lockey.ExitReadLock();
            */
        }

        volatile bool Running = true;
        readonly ConcurrentDictionary<string, AtomicLong> Counters = new();

        void Increase(string name)
        {
            Counters.GetOrAdd(name, (key) => new AtomicLong()).IncrementAndGet();
        }

        [TestMethod]
        public void TestTry()
        {
            var locks = new Locks();
            var tkey = new TableKey("1", 1);
            var tasks = new List<Task>
            {
                Task.Run(async () =>
                {
                    while (Running)
                    {
                        using (await locks.Get(tkey).ReaderLockAsync())
                        {
                            Increase("ReaderLockAsync 1");
                            await Task.Delay(global::Zeze.Util.Random.Instance.Next(20));
                        }
                        await Task.Delay(global::Zeze.Util.Random.Instance.Next(20));
                    }
                }),

                Task.Run(async () =>
                {
                    while (Running)
                    {
                        using (await locks.Get(tkey).ReaderLockAsync())
                        {
                            Increase("ReaderLockAsync 2");
                            await Task.Delay(global::Zeze.Util.Random.Instance.Next(20));
                        }
                        await Task.Delay(global::Zeze.Util.Random.Instance.Next(20));
                    }
                }),

                Task.Run(async () =>
                {
                    while (Running)
                    {
                        using (await locks.Get(tkey).ReaderLockAsync())
                        {
                            Increase("ReaderLockAsync 3");
                            await Task.Delay(global::Zeze.Util.Random.Instance.Next(20));
                        }
                        await Task.Delay(global::Zeze.Util.Random.Instance.Next(20));
                    }
                }),

                Task.Run(async () =>
                {
                    while (Running)
                    {
                        using (await locks.Get(tkey).WriterLockAsync())
                        {
                            Increase("WriterLockAsync 1");
                            await Task.Delay(global::Zeze.Util.Random.Instance.Next(20));
                        }
                        await Task.Delay(global::Zeze.Util.Random.Instance.Next(20));
                    }
                }),

                Task.Run(async () =>
                {
                    while (Running)
                    {
                        using (await locks.Get(tkey).WriterLockAsync())
                        {
                            Increase("WriterLockAsync 2");
                            await Task.Delay(global::Zeze.Util.Random.Instance.Next(20));
                        }
                        await Task.Delay(global::Zeze.Util.Random.Instance.Next(20));
                    }
                }),

                Task.Run(() =>
                {
                    while (Running)
                    {
                        var lockey = locks.Get(tkey);
                        if (lockey.TryEnterReadLock())
                        {
                            try
                            {
                                Increase("TryEnterReadLock 1");
                                Task.Delay(global::Zeze.Util.Random.Instance.Next(20)).Wait();
                            }
                            finally
                            {
                                lockey.Release();
                            }
                        }
                        Task.Delay(global::Zeze.Util.Random.Instance.Next(20)).Wait();
                    }
                }),

                Task.Run(() =>
                {
                    while (Running)
                    {
                        var lockey = locks.Get(tkey);
                        if (lockey.TryEnterReadLock())
                        {
                            try
                            {
                                Increase("TryEnterReadLock 2");
                                Task.Delay(global::Zeze.Util.Random.Instance.Next(20)).Wait();
                            }
                            finally
                            {
                                lockey.Release();
                            }
                        }
                        Task.Delay(global::Zeze.Util.Random.Instance.Next(20)).Wait();
                    }
                }),

                Task.Run(() =>
                {
                    while (Running)
                    {
                        var lockey = locks.Get(tkey);
                        if (lockey.TryEnterWriteLock())
                        {
                            try
                            {
                                Increase("TryEnterWriteLock 1");
                                Task.Delay(global::Zeze.Util.Random.Instance.Next(20)).Wait();
                            }
                            finally
                            {
                                lockey.Release();
                            }
                        }
                        Task.Delay(global::Zeze.Util.Random.Instance.Next(20)).Wait();
                    }
                }),

                Task.Run(() =>
                {
                    while (Running)
                    {
                        var lockey = locks.Get(tkey);
                        if (lockey.TryEnterWriteLock())
                        {
                            try
                            {
                                Increase("TryEnterWriteLock 2");
                                Task.Delay(global::Zeze.Util.Random.Instance.Next(20)).Wait();
                            }
                            finally
                            {
                                lockey.Release();
                            }
                        }
                        Task.Delay(global::Zeze.Util.Random.Instance.Next(20)).Wait();
                    }
                })
            };

            Thread.Sleep(60 * 1000);
            Running = false;
            Task.WaitAll(tasks.ToArray());
            var sb = new StringBuilder();
            Str.BuildString(sb, Counters, new ComparerString());
            Console.WriteLine(sb.ToString());
        }
    }
}
