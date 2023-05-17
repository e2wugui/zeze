using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Net
{
    public class ProtocolPool
    {
        private ConcurrentQueue<Protocol> Pool = new ConcurrentQueue<Protocol>();
        private Func<Protocol, Task<long>> Handle;

        public ProtocolPool(Func<Protocol, Task<long>> handle)
        {
            Handle = handle;
        }

        // see Protocol.Decode
        public Protocol Acquire(Service.ProtocolFactoryHandle pfh)
        {
            if (Pool.TryDequeue(out var result))
                return result;

            return pfh.Factory();
        }

        // see Service.ProtocolFactoryHandle
        public async Task<long> Process(Protocol p)
        {
            var result = await Handle(p);
            if (result == 0 && p.Recyle && Pool.Count < 10000)
            {
                p.ClearParameters();
                Pool.Enqueue(p);
            }
            return result;
        }
    }
}
