using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Transaction;

namespace Zeze.Collections
{
    internal class BeanFactory
    {
        private ConcurrentDictionary<long, Func<Bean>> Factorys = new();

        public void Register<T>()
            where T : Bean, new()
        {
            var seed = new T();
            // skip duplicate register
            Factorys.TryAdd(seed.TypeId, () => new T());
        }

        public Bean Create(long typeId)
        {
            if (Factorys.TryGetValue(typeId, out var factory))
                return factory();
            throw new Exception($"Unknown TypeId={typeId}");
        }
    }
}
