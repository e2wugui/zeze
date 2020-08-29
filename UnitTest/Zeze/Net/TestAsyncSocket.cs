﻿using System;
using System.Collections.Generic;
using System.Reflection.PortableExecutable;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Util;

namespace UnitTest.Zeze.Net
{
    [TestClass]
    public class TestAsyncSocket
    {
        public class ServiceClient : Service
        {
            public override void OnSocketConnected(AsyncSocket so)
            {
                base.OnSocketConnected(so);
                Console.WriteLine("OnSocketConnected: " + so.SerialNo);
                string head = "GET http://www.163.com/\r\nHost: www.163.com\r\nAccept:*/*\r\n\r\n";
                so.Send(head);
            }

            public override void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input)
            {
                Console.WriteLine("input size=" + input.Size);
                Console.WriteLine(Encoding.UTF8.GetString(input.Bytes, input.ReadIndex, input.Size));
                input.ReadIndex = input.WriteIndex;
            }
        }

        [TestMethod]
        public void TestConnect()
        {
            ServiceClient client = new ServiceClient();
            using AsyncSocket so = client.NewClientSocket("www.163.com", 80);
            Thread.Sleep(2000); // 异步的，等待结果
        }
        /*
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
        */
    }
}
