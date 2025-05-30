
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

        public WebsocketClient(Service service, string wsUrl, Connector connector) : base(service) 
        {
            base.Connector = connector;
            Type = AsyncSocketType.eClient;
            _uri = new Uri(wsUrl);
            RemoteAddress = new IPEndPoint(IPAddress.Parse(_uri.Host), _uri.Port);
            // LocalAddress = null; // 得不到。

            // 接收循环，发送循环都放到后台。
            _ = ConnectReceive(wsUrl);
            _ = SendLoop();
        }

        private async Task ConnectReceive(string wsUrl)
        {
            try
            {
                _clientWebSocket.Options.KeepAliveInterval = TimeSpan.FromSeconds(20);
                using var connectCts = new CancellationTokenSource(TimeSpan.FromSeconds(5));
                await _clientWebSocket.ConnectAsync(_uri, connectCts.Token);
                Service.OnHandshakeDone(this);

                var buffer = ByteBuffer.Allocate(4096);
                while (_clientWebSocket.State == WebSocketState.Open && !_cts.IsCancellationRequested)
                {
                    buffer.EnsureWrite(4096);
                    var result = await _clientWebSocket.ReceiveAsync(
                        new ArraySegment<byte>(buffer.Bytes, buffer.WriteIndex, buffer.Capacity - buffer.WriteIndex), _cts.Token);
                    if (result.MessageType == WebSocketMessageType.Close)
                        Dispose();
                    else if (result.EndOfMessage)
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
                Service.OnSocketClose(this, LastException);
                _cts.Cancel();
                await _clientWebSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, null, CancellationToken.None);
            }
            catch (Exception ex)
            {
                logger.Error(ex);
            }
        }

        public override bool Send(byte[] bytes, int offset, int length)
        {
            Monitor.Enter(this);
            try
            {
                _sendQueue.Add(new ArraySegment<byte>(bytes, offset, length));
                return true;
            }
            finally
            {
                Monitor.Exit(this);
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
