
using System;
using System.Net.WebSockets;
using System.Threading.Tasks;
using System.Threading;
using Zeze.Serialize;
using System.Collections.Concurrent;
using System.Linq;
using System.Net;
#if UNITY_WEBSOCKET
using UnityEngine;
using UnityWebSocket;
using WebSocket = UnityWebSocket.WebSocket;
using WebSocketState = UnityWebSocket.WebSocketState;
using System.Collections.Generic;
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
#else
        private WebSocket _webSocket = null;
        private bool isWebSocketOpen = false;
        private readonly List<byte[]> sendBuffer = new List<byte[]>();
#endif

        public WebsocketClient(Service service, string wsUrl, object userState, Connector connector) : base(service)
        {
            Connector = connector;
            Type = AsyncSocketType.eClient;

            var url = new Uri(wsUrl);
            
            // 修改后的代码：支持 IP 和域名
            if (IPAddress.TryParse(url.Host, out var ip))
            {
                RemoteAddress = new IPEndPoint(ip, url.Port);
            }
            else
            {
                // 如果是域名，尝试 DNS 解析
                try
                {
                    var addresses = System.Net.Dns.GetHostAddresses(url.Host);
                    // 优先选择 IPv4 地址，如果没有则取第一个
                    ip = addresses.FirstOrDefault(a => a.AddressFamily == System.Net.Sockets.AddressFamily.InterNetwork) ??
                         addresses.FirstOrDefault();
                    if (ip != null)
                    {
                        RemoteAddress = new IPEndPoint(ip, url.Port);
                    }
                    else
                    {
                        // 解析失败，抛出异常
                        throw new Exception($"DNS resolution failed for {url.Host}: no valid IP address found.");
                    }
                }
                catch (Exception ex)
                {
                    // DNS 解析异常，记录日志并重新抛出
                    logger.Error($"DNS resolution failed for {url.Host}: {ex.Message}");
                    throw;  // 重新抛出原始异常
                }
            }
            UserState = userState;

#if !UNITY_WEBSOCKET
            // LocalAddress = null; // 得不到。
            // 接收循环放到后台。
            _ = ConnectReceive(url);
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
        private async Task ConnectReceive(Uri url)
        {
            try
            {
                _clientWebSocket.Options.KeepAliveInterval = TimeSpan.FromSeconds(20);
                using var connectCts = new CancellationTokenSource(TimeSpan.FromSeconds(5));
                await _clientWebSocket.ConnectAsync(url, connectCts.Token);
                Service.AddSocket(this);
                Service.OnHandshakeDone(this);
                // 连接成功，发送循环放到后台。
                _ = Task.Run(SendLoop, _cts.Token);

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
            _sendQueue.Add(new ArraySegment<byte>(bytes, offset, length));
            return true;
        }

        public override void Dispose()
        {
            if (ClosedState(1) != 0)
                return;
            try
            {
                _cts.Cancel();
                _clientWebSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Normal closure",
                    CancellationToken.None).ContinueWith(t =>
                {
                    if (t.Exception != null)
                        logger.Error(t.Exception.InnerException ?? t.Exception);
                }, TaskContinuationOptions.OnlyOnFaulted);
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
