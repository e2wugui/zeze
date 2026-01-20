
using System;
using System.Net.WebSockets;
using System.Threading.Tasks;
using System.Threading;
using Zeze.Serialize;
using System.Collections.Concurrent;
using System.Net;
#if UNITY_WEBSOCKET
using UnityEngine;
using UnityWebSocket;
using WebSocket = UnityWebSocket.WebSocket;
using WebSocketState = UnityWebSocket.WebSocketState;
#endif

namespace Zeze.Net
{
    public class WebsocketClient : AsyncSocket
    {
        // 公共字段
        private ByteBuffer receiveBuffer;

#if !UNITY_WEBSOCKET
        private readonly ClientWebSocket _clientWebSocket = new ClientWebSocket();
        private readonly CancellationTokenSource _cts = new CancellationTokenSource();
        private readonly BlockingCollection<ArraySegment<byte>> _sendQueue = new BlockingCollection<ArraySegment<byte>>();
        private readonly Uri _uri;
#else
        private WebSocket _webSocket = null;
        private bool isWebSocketOpen = false;
        private readonly List<byte[]> sendBuffer = new List<byte[]>();
#endif

        public WebsocketClient(Service service, string wsUrl, object userState, Connector connector) : base(service)
        {
            base.Connector = connector;
            base.Type = AsyncSocketType.eClient;

            var _uri = new Uri(wsUrl);
            base.RemoteAddress = new IPEndPoint(IPAddress.Parse(_uri.Host), _uri.Port);
            base.UserState = userState;

#if !UNITY_WEBSOCKET
            // LocalAddress = null; // 得不到。
            // 接收循环放到后台。
            _ = ConnectReceive();
#else
            _webSocket = new WebSocket(wsUrl);
            _webSocket.OnOpen += OnWebSocketOpen;
            _webSocket.OnMessage += OnMessageReceived;
            _webSocket.OnClose += OnClose;
            _webSocket.OnError += OnError;
            _webSocket.ConnectAsync();
#endif
        }

#if !UNITY_WEBSOCKET
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

                receiveBuffer = ByteBuffer.Allocate(4096);
                while (_clientWebSocket.State == WebSocketState.Open && !_cts.IsCancellationRequested)
                {
                    receiveBuffer.EnsureWrite(4096);
                    var segment = new ArraySegment<byte>(receiveBuffer.Bytes, receiveBuffer.WriteIndex, receiveBuffer.Capacity - receiveBuffer.WriteIndex);
                    var result = await _clientWebSocket.ReceiveAsync(segment, _cts.Token);
                    if (result.MessageType == WebSocketMessageType.Close)
                    {
                        Dispose();
                        break;
                    }
                    receiveBuffer.WriteIndex += result.Count;
                    if (result.EndOfMessage)
                    {
                        Service.OnSocketProcessInputBuffer(this, receiveBuffer);
                        receiveBuffer.Campact();
                    }
                }
            }
            catch (Exception ex)
            {
                Close(ex);
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

        public override bool Send(byte[] bytes, int offset, int length)
        {
            lock(this)
            {
                _sendQueue.Add(new ArraySegment<byte>(bytes, offset, length));
                return true;
            }
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

#else
        private void OnError(object sender, ErrorEventArgs e)
        {
            Debug.LogError("zeze: websocket收到错误消息："+e.Message);
            var exception = e.Exception;
            if (exception != null)
            {
                Debug.LogError("zeze: websocket收到异常："+exception.ToString());
            }
        }

        private void OnClose(object sender, CloseEventArgs e)
        {
            Dispose();
        }

        private void OnWebSocketOpen(object sender, OpenEventArgs e)
        {
            isWebSocketOpen = true;

            Service.AddSocket(this);
            Service.OnHandshakeDone(this);
            receiveBuffer = ByteBuffer.Allocate(4096);

            lock(sendBuffer)
            {
                for (int i = 0; i < sendBuffer.Count; i++)
                {
                    _webSocket.SendAsync(sendBuffer[i]);
                }
                sendBuffer.Clear();  // 清空已发送的缓存
            }
        }

        private void OnMessageReceived(object sender, MessageEventArgs e)
        {
            if (receiveBuffer == null)
            {
                Debug.LogError("zeze: receiveBuffer is null");
                return;
            }

            if (!e.IsBinary)
            {
                if (e.IsText)
                {
                    Debug.LogError("zeze: 消息是Text:"+e.Data);
                }
                else
                {
                    Debug.LogError("zeze: 未知消息类型");
                }
                return;
            }

            var rawData = e.RawData;
            receiveBuffer.EnsureWrite(rawData.Length);

            Buffer.BlockCopy(rawData, 0, receiveBuffer.Bytes, receiveBuffer.WriteIndex, rawData.Length);
            receiveBuffer.WriteIndex += rawData.Length;

            Service.OnSocketProcessInputBuffer(this, receiveBuffer);
            receiveBuffer.Campact();
        }

        public override bool Send(byte[] bytes, int offset, int length)
        {
            byte[] targetBytes = null;
            if (offset != 0 || bytes.Length != length)
            {
                targetBytes =  new byte[length];
                Buffer.BlockCopy(bytes, offset, targetBytes, 0, length);
            }
            else
            {
                targetBytes = bytes;
            }

            lock(sendBuffer)
            {
                if (!isWebSocketOpen)
                {
                    sendBuffer.Add(targetBytes);
                    return false;
                }
            }

            // 锁外再次检查，防止 Dispose 后调用
            if (_webSocket == null)
            {
                return false;
            }

            _webSocket.SendAsync(targetBytes);
            return true;
        }

        public override void Dispose()
        {
            lock(sendBuffer)
            {
                isWebSocketOpen = false;
                sendBuffer.Clear();  // 清空未发送的缓存
            }

            if (base.ClosedState(1) != 0)
                return;

            try
            {
                if (_webSocket != null)
                {
                    _webSocket.OnOpen -= OnWebSocketOpen;
                    _webSocket.OnMessage -= OnMessageReceived;
                    _webSocket.OnClose -= OnClose;
                    _webSocket.OnError -= OnError;
                    _webSocket.CloseAsync();
                    _webSocket = null;
                }
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

#endif

        public override void CloseGracefully()
        {
            Dispose();
        }

    }
}
