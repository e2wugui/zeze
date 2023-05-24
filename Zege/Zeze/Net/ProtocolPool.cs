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
        public enum ReuseLevel
        {
            Protocol,
            Bean
        }

        private readonly ConcurrentQueue<Protocol> Pool = new ConcurrentQueue<Protocol>();
        private readonly Func<Protocol, Task<long>> Handle;
        private readonly ReuseLevel Level;

        public ProtocolPool(Func<Protocol, Task<long>> handle, ReuseLevel level)
        {
            Handle = handle;
            Level = level;
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
                p.ClearParameters(Level);
                Pool.Enqueue(p);
            }
            return result;
        }
    }
}
