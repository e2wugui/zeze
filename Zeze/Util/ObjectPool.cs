using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Util
{
    public class ObjectPool<T> where T : class, new()
    {
        private ConcurrentQueue<T> Pool = new();

        public T Create()
        {
            if (Pool.TryDequeue(out var item))
                return item;
            return new T();
        }

        public void Reclaim(T obj)
        {
            Pool.Enqueue(obj);
        }
    }
}
