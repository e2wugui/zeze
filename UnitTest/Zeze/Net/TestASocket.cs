using System;
using System.Collections.Generic;
using System.Reflection.PortableExecutable;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Net;
using Zeze.Util;

namespace UnitTest.Zeze.Net
{
    [TestClass]
    public class TestASocket
    {
        [TestMethod]
        public void TestConnect()
        {
            Manager manager = new Manager();
            using ASocket so = new ASocket(manager, "www.163.com", 80);
            Thread.Sleep(2000);
        }

        [TestMethod]
        public void TestAsync()
        {
            Console.WriteLine("start " + System.Threading.Thread.CurrentThread.ManagedThreadId + " " + Time.NowUnixMillis);
            Task.Delay(10).Wait();
            Console.WriteLine("Task.Wait End " + System.Threading.Thread.CurrentThread.ManagedThreadId + " " + Time.NowUnixMillis);
            AAsync();
            Console.WriteLine("B " + System.Threading.Thread.CurrentThread.ManagedThreadId + " " + Time.NowUnixMillis);
            System.Threading.Thread.Sleep(2000);
        }

        async Task AAsync()
        {
            Console.WriteLine("A " + System.Threading.Thread.CurrentThread.ManagedThreadId + " " + Time.NowUnixMillis);
            Task d1 = Task.Run(Job);
            Task d2 = Task.Run(Job);
            await d1;
            Console.WriteLine("A d1 await end " + System.Threading.Thread.CurrentThread.ManagedThreadId + " " + Time.NowUnixMillis);
            await d2;
            Console.WriteLine("A d2 await end " + System.Threading.Thread.CurrentThread.ManagedThreadId + " " + Time.NowUnixMillis);
        }

        void Job()
        {
            Console.WriteLine("Job start " + System.Threading.Thread.CurrentThread.ManagedThreadId);
            long sum = 0;
            for (int i = 0; i < 100000000; ++i)
            {
                sum += i;
            }
            Console.WriteLine("Job end " + System.Threading.Thread.CurrentThread.ManagedThreadId);
        }
    }
}
