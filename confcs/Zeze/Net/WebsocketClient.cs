
using System;
using System.Net.WebSockets;
using System.Threading.Tasks;
using System.Threading;
using Zeze.Serialize;
using System.Collections.Concurrent;
using System.Net;
using NLog;

namespace Zeze.Net
{
    public class WebsocketClient : AsyncSocket
    {
        private readonly ClientWebSocket _clientWebSocket = new ClientWebSocket();
        private readonly CancellationTokenSource _cts = new CancellationTokenSource();
        private readonly BlockingCollection<ArraySegment<byte>> _sendQueue = new BlockingCollection<ArraySegment<byte>>();
        private readonly Uri _uri;

        public WebsocketClient(Service service, string wsUrl, object userState, Connector connector) : base(service) 
        {
            base.Connector = connector;
            base.Type = AsyncSocketType.eClient;

            _uri = new Uri(wsUrl);
            base.RemoteAddress = new IPEndPoint(IPAddress.Parse(_uri.Host), _uri.Port);
            base.UserState = userState;
            // LocalAddress = null; // 得不到。

            // 接收循环放到后台。
            Task.Run(ConnectReceive);
        }

        private async Task ConnectReceive()
        {
            try
            {
                _clientWebSocket.Options.KeepAliveInterval = TimeSpan.FromSeconds(20);
                using var connectCts = new CancellationTokenSource(TimeSpan.FromSeconds(5));
                await _clientWebSocket.ConnectAsync(_uri, connectCts.Token);
                Service.AddSocket(this);
                Service.OnHandshakeDone(this);
                // 连接成功，发送循环放到后台。
                _ = Task.Run(SendLoop);

                var buffer = ByteBuffer.Allocate(4096);
                while (_clientWebSocket.State == WebSocketState.Open && !_cts.IsCancellationRequested)
                {
                    buffer.EnsureWrite(4096);
                    var segment = new ArraySegment<byte>(buffer.Bytes, buffer.WriteIndex, buffer.Capacity - buffer.WriteIndex);
                    var result = await _clientWebSocket.ReceiveAsync(segment, _cts.Token);
                    if (result.MessageType == WebSocketMessageType.Close)
                    {
                        Dispose();
                        break;
                    }
                    buffer.WriteIndex += result.Count;
                    if (result.EndOfMessage)
                    {
                        Service.OnSocketProcessInputBuffer(this, buffer);
                        buffer.Campact();
                    }
                }
            }
            catch (Exception ex)
            {
                Close(ex);
            }
        }

        public override void CloseGracefully()
        {
            Dispose();
        }

        public override async void Dispose()
        {
            if (base.ClosedState(1) != 0)
                return;
            try
            {
                _cts.Cancel();
                await _clientWebSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, null, CancellationToken.None);
            }
            catch (Exception ex)
            {
                logger.Error(ex);
            }
            try
            {
                Service.OnSocketClose(this, LastException);
            }
            catch (Exception ex)
            {
                logger.Error(ex);
            }
        }

        public override bool Send(byte[] bytes, int offset, int length)
        {
            lock(this)
            {
                _sendQueue.Add(new ArraySegment<byte>(bytes, offset, length));
                return true;
            }
        }

        private async Task SendLoop()
        {
            try
            {
                while (_clientWebSocket.State == WebSocketState.Open && !_cts.IsCancellationRequested)
                {
                    var buffer = _sendQueue.Take(_cts.Token);
                    await _clientWebSocket.SendAsync(buffer, WebSocketMessageType.Binary, true, CancellationToken.None);
                }
            }
            catch(Exception ex)
            {
                Close(ex);
            }
        }
    }
}
