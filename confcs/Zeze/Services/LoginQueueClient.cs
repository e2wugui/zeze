
using Zeze.Net;
using System.Threading.Tasks;
using Zeze.Builtin.LoginQueue;
using System;

namespace Zeze.Services
{
    public class LoginQueueClient : AbstractLoginQueueClient
    {
        private readonly LoginQueueClientService service;

        public LoginQueueClient()
        {
            service = new LoginQueueClientService();
        }

        public void Connect(string hostNameOrAddress, int port)
        {
            service.Connect(hostNameOrAddress, port, false);
        }

        public void Close()
        {
            service.Stop();
        }

        public sealed class LoginQueueClientService : Service
        {
            public LoginQueueClientService()
                : base("LoginQueueClient")
            {
            }
        }

        public Action<BQueuePosition> QueuePosition { get; set; }
        public Action<BLoginToken> LoginToken { get; set; }
        public Action QueueFull { get; set; }

        protected override Task<long> ProcessPutLoginToken(Zeze.Net.Protocol _p)
        {
            var p = _p as PutLoginToken;
            LoginToken?.Invoke(p.Argument);
            Close();
            return Task.FromResult(0L);
        }

        protected override Task<long> ProcessPutQueueFull(Zeze.Net.Protocol _p)
        {
            var p = _p as PutQueueFull;
            QueueFull?.Invoke();
            Close();
            return Task.FromResult(0L);
        }

        protected override Task<long> ProcessPutQueuePosition(Zeze.Net.Protocol _p)
        {
            var p = _p as PutQueuePosition;
            QueuePosition?.Invoke(p.Argument);
            return Task.FromResult(0L);
        }

    }
}
