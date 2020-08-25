using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Collections.Concurrent;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestConcurrentDictionary
    {
        [TestMethod]
        public void TestRemoveInForeach()
        {
            ConcurrentDictionary<int, int> cd = new ConcurrentDictionary<int, int>();

            cd.TryAdd(1, 1);
            cd.TryAdd(2, 2);
            cd.TryAdd(3, 3);
            cd.TryAdd(4, 4);
            cd.TryAdd(5, 5);

            int i = 6;
            foreach (var e in cd)
            {
                if (e.Key < 3)
                {
                    cd.TryRemove(e.Key, out var _);
                    Console.WriteLine("remove key=" + e.Key);
                }
                else
                {
                    if (i < 10)
                    {
                        cd.TryAdd(i, i);
                        ++i;
                    }
                    Console.WriteLine("key=" + e.Key);
                }
            }
        }
    }
}
