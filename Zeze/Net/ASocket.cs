using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Net
{
    public class ASocket
    {
        private System.Net.Sockets.Socket _socket;

        public ASocket(System.Net.SocketAddress sa)
        {
            AsyncCallback ac = null;
            List<ArraySegment<byte>> buffers = null;
            System.Net.Sockets.SocketError err;
            _socket.BeginSend(buffers, System.Net.Sockets.SocketFlags.None, out err, ac, this);
            _socket.BeginAccept(ac, this);
            System.Net.EndPoint ep = null;
            _socket.BeginConnect(ep, ac, this);
            _socket.BeginDisconnect(false, ac, this);
            IAsyncResult ar = null;
            System.Net.Dns.BeginGetHostAddresses("", ac, this);
            System.Net.IPAddress[] ipa = System.Net.Dns.EndGetHostAddresses(ar);
            System.Net.Dns.BeginGetHostEntry(ipa[0], ac, this);
            System.Net.IPHostEntry iph = System.Net.Dns.EndGetHostEntry(ar);
        }
    }
}
