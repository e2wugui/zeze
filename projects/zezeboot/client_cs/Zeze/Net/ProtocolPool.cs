using System;
using System.Collections.Concurrent;
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
            return Pool.TryDequeue(out var result) ? result : pfh.Factory();
        }

        // see Service.ProtocolFactoryHandle
        public async Task<long> Process(Protocol p)
        {
            var result = await Handle(p);
            if (result == 0 && p.Recycle && Pool.Count < 10000)
            {
                p.ClearParameters(Level);
                Pool.Enqueue(p);
            }
            return result;
        }
    }
}
