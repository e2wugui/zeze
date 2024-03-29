﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Net.Sockets;
using System.Net;
using System.Threading;

namespace UnitTest.Zeze.Net
{
    [TestClass]
    public class TestSocketXXXAsync
    {
        [TestMethod]
        public void TestReceiveAsync()
        {
            // Async 模式下，io事件看起来没有使用独立线程池，
            // 而是共享了worker线程池，这个原因可能是Async
            // 不会有线程在同步等待中，而且Task调度上也需要使用worker线程池。
            // 当多线程都在同步等待，没有空闲线程时，
            // io事件会由于worker线程池最快一秒启动一个新线程，响应变慢。
            ThreadPool.SetMinThreads(500, 500);

            ServerSocket = new Socket(SocketType.Stream, ProtocolType.Tcp);
            ServerSocket.Blocking = false;
            ServerSocket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
            var localEP = new IPEndPoint(IPAddress.Parse("127.0.0.1"), 9090);
            ServerSocket.Bind(localEP);
            ServerSocket.Listen();

            eventArgsAccept = new SocketAsyncEventArgs();
            eventArgsAccept.Completed += OnAsyncIOCompleted;
            BeginAcceptAsync();

            var TestClientSocket = new Socket(SocketType.Stream, ProtocolType.Tcp);
            TestClientSocket.Connect(localEP);
            int count = 500;
            for (int i = 0; i < count; ++i)
            {
                var s = $"{i} {new string('_', 30)}";
                var sendbytes = Encoding.UTF8.GetBytes(s);
                TestClientSocket.Send(sendbytes);
                byte[] buffer = new byte[8192];
                int recvlength = 0;
                while (recvlength < sendbytes.Length)
                {
                    int rc = TestClientSocket.Receive(buffer, recvlength, buffer.Length - recvlength, SocketFlags.None);
                    if (rc <= 0)
                    {
                        Console.WriteLine($"{DateTime.Now} ClientSocket.Receive Peer Close");
                        break;
                    }
                    recvlength += rc;
                }
                Console.WriteLine($"{DateTime.Now} ClientSocket Recv {i} Ok!");
                //Thread.Sleep(1);
            }
            TestClientSocket.Close();
            this.ClientSocket?.Close();
            ServerSocket.Close();
        }

        private SocketAsyncEventArgs eventArgsAccept;
        private SocketAsyncEventArgs eventArgsReceive;
        private Socket ServerSocket;
        private Socket ClientSocket;
        private byte[] inputBuffer = new byte[8192];

        private void BeginAcceptAsync()
        {
            eventArgsAccept.AcceptSocket = null;
            if (false == ServerSocket.AcceptAsync(eventArgsAccept))
                ProcessAccept(eventArgsAccept);
        }

        private void BeginReceiveAsync()
        {
            if (null == eventArgsReceive)
            {
                eventArgsReceive = new SocketAsyncEventArgs();
                eventArgsReceive.Completed += OnAsyncIOCompleted;
            }

            eventArgsReceive.SetBuffer(inputBuffer, 0, inputBuffer.Length);
            Console.WriteLine($"{DateTime.Now} BeginReceiveAsync: {ClientSocket.LocalEndPoint}-{ClientSocket.RemoteEndPoint}");
            if (false == this.ClientSocket.ReceiveAsync(eventArgsReceive))
                ProcessReceive(eventArgsReceive);
        }

        private void BeginSendAsync(byte[] bytes, int offset, int length)
        {
            var eventArgsSend = new SocketAsyncEventArgs();
            eventArgsSend.Completed += OnAsyncIOCompleted;
            var outputList = new List<System.ArraySegment<byte>>();
            outputList.Add(new ArraySegment<byte>(bytes, offset, length));
            eventArgsSend.BufferList = outputList;
            Console.WriteLine($"{DateTime.Now} BeginSendAsync {bytes.Length}");
            if (false == ClientSocket.SendAsync(eventArgsSend))
                ProcessSend(eventArgsSend);
        }

        private void ProcessSend(SocketAsyncEventArgs e)
        {
            Console.WriteLine($"{DateTime.Now} ProcessSend {e.BytesTransferred}");
            if (e.BytesTransferred >= 0 && e.SocketError == SocketError.Success)
            {
                var bytesTransferred = e.BytesTransferred;
                var outputList = e.BufferList as List<System.ArraySegment<byte>>;
                for (int i = 0; i < outputList.Count; ++i)
                {
                    int bytesCount = outputList[i].Count;
                    if (bytesTransferred >= bytesCount)
                    {
                        bytesTransferred -= bytesCount;
                        if (bytesTransferred > 0)
                            continue;

                        outputList.RemoveRange(0, i + 1);
                        break;
                    }
                    // 已经发送的数据比数组中的少。
                    ArraySegment<byte> segment = e.BufferList[i];
                    // Slice .net framework 没有定义。
                    outputList[i] = new ArraySegment<byte>(segment.Array, bytesTransferred, segment.Count - bytesTransferred);
                    outputList.RemoveRange(0, i);
                    break;
                }

                if (outputList.Count == 0)
                {
                    Console.WriteLine($"{DateTime.Now} ProcessSend Done");
                }
                else
                {
                    Console.WriteLine($"{DateTime.Now} ProcessSend Retry");
                    e.BufferList = outputList;
                    if (false == ClientSocket.SendAsync(e))
                        ProcessSend(e);
                }
            }
            else
            {
                Console.WriteLine($"{DateTime.Now} ProcessSend: Fail!");
                ClientSocket?.Close();
            }
        }

        private void ProcessAccept(SocketAsyncEventArgs e)
        {
            if (e.SocketError == SocketError.Success)
            {
                ClientSocket = e.AcceptSocket;
                Console.WriteLine($"{DateTime.Now} ProcessAccept: {ClientSocket.LocalEndPoint}-{ClientSocket.RemoteEndPoint}");
                BeginReceiveAsync();
                //BeginAcceptAsync();
            }
        }

        private void SimulateUserSendAndWait(byte[] copy)
        {
            Task.Run(() =>
            {
                BeginSendAsync(copy, 0, copy.Length); // 里面掉了 SendAsync                
                // 下面这个会挂起.net socket
                Thread.Sleep(10000);
                /*
                var future = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
                //future.Task.ConfigureAwait(false);
                Task.Run(() => { Thread.Sleep(10000); future.SetResult(true); });
                future.Task.Wait();
                // */
            });
        }

        private void ProcessReceive(SocketAsyncEventArgs e)
        {
            Console.WriteLine($"{DateTime.Now} ProcessReceive: {ClientSocket.LocalEndPoint}-{ClientSocket.RemoteEndPoint} {e.BytesTransferred}");
            if (e.BytesTransferred > 0 && e.SocketError == SocketError.Success)
            {
                var s = Encoding.UTF8.GetString(inputBuffer, 0, e.BytesTransferred);
                Console.WriteLine($"{DateTime.Now} ProcessReceive: {s}");
                var copy = new byte[e.BytesTransferred];
                Array.Copy(inputBuffer, 0, copy, 0, copy.Length);
                BeginReceiveAsync();
                SimulateUserSendAndWait(copy);
                return;
            }
            Console.WriteLine($"{DateTime.Now} ProcessReceive: Peer Close?");
            ClientSocket?.Close();
            ClientSocket = null;
        }

        private void OnAsyncIOCompleted(object sender, SocketAsyncEventArgs e)
        {
            try
            {
                switch (e.LastOperation)
                {
                    case SocketAsyncOperation.Accept:
                        ProcessAccept(e);
                        break;
                    case SocketAsyncOperation.Send:
                        ProcessSend(e);
                        break;
                    case SocketAsyncOperation.Receive:
                        ProcessReceive(e);
                        break;
                    default:
                        throw new ArgumentException();
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"{DateTime.Now} OnAsyncIOCompleted {ex}");
            }
        }
    }
}
