using System;
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
    public class TestSocketBeginXXX
    {
        [TestMethod]
        public void Test()
        {
            ThreadPool.SetMinThreads(500, 500);
            ServerSocket = new Socket(SocketType.Stream, ProtocolType.Tcp);
            ServerSocket.Blocking = false;
            ServerSocket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
            var localEP = new IPEndPoint(IPAddress.Parse("127.0.0.1"), 9090);
            ServerSocket.Bind(localEP);
            ServerSocket.Listen();

            ServerSocket.BeginAccept(OnAsyncAccept, null);

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
            ClientSocket?.Close();
            ServerSocket.Close();
        }

        private Socket ServerSocket;
        private Socket ClientSocket;
        private byte[] inputBuffer = new byte[8192];

        private void OnAsyncAccept(IAsyncResult ar)
        {
            ClientSocket = ServerSocket.EndAccept(ar);
            ClientSocket.BeginReceive(inputBuffer, 0, inputBuffer.Length, 0, OnAsyncReceive, null);
        }

        private void OnAsyncReceive(IAsyncResult ar)
        {
            try
            {
                Console.WriteLine($"{DateTime.Now} OnAsyncReceive: {ClientSocket.LocalEndPoint}-{ClientSocket.RemoteEndPoint}");
                int bytesRead = ClientSocket.EndReceive(ar);

                if (bytesRead <= 0)
                {
                    ClientSocket.Close();
                    ClientSocket = null;
                    return;
                }

                var copy = new byte[bytesRead];
                Array.Copy(inputBuffer, 0, copy, 0, copy.Length);
                SimulateUserSendAndWait(copy);

                inputBuffer = new byte[8192];
                ClientSocket.BeginReceive(inputBuffer, 0, inputBuffer.Length, 0, OnAsyncReceive, null);
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }
        }

        private List<ArraySegment<byte>> _outputBufferList = null;
        private List<ArraySegment<byte>> _outputBufferListSending = null; // 正在发送的 buffers.

        private void Send(byte[] bytes, int offset, int length)
        {
            lock (this)
            {
                if (null == _outputBufferList)
                    _outputBufferList = new List<ArraySegment<byte>>();
                _outputBufferList.Add(new ArraySegment<byte>(bytes, offset, length));

                if (null == _outputBufferListSending)
                {
                    // 没有在发送中，马上请求发送，否则等回调处理。
                    _outputBufferListSending = _outputBufferList;
                    _outputBufferList = null;

                    ClientSocket.BeginSend(_outputBufferListSending, 0, OnAsyncSend, null);
                }
            }
        }

        private void OnAsyncSend(IAsyncResult ar)
        {
            try
            {
                Console.WriteLine($"{DateTime.Now} OnAsyncSend");
                ClientSocket.EndSend(ar);

                lock (this)
                {
                    _outputBufferListSending = _outputBufferList;
                    _outputBufferList = null;
                    if (null != _outputBufferListSending)
                    {
                        ClientSocket.BeginSend(_outputBufferListSending, 0, OnAsyncSend, null);
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }
        }

        private void SimulateUserSendAndWait(byte[] copy)
        {
            Task.Run(() =>
            {
                Send(copy, 0, copy.Length); // 里面掉了 SendAsync                
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
    }
}
