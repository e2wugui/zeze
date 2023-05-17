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
        internal ConcurrentQueue<Protocol> Pool = new ConcurrentQueue<Protocol>();
        internal Func<Protocol, Task<long>> Handle;

        public ProtocolPool(Func<Protocol, Task<long>> handle)
        {
            Handle = handle;
        }

        public Protocol Acquire(Service.ProtocolFactoryHandle pfh)
        {
            if (Pool.TryDequeue(out var result))
                return result;

            return pfh.Factory();
        }

        // see Service.ProtocolFactoryHandle
    }
}
