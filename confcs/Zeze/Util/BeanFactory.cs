using System;
using System.Collections.Concurrent;

namespace Zeze.Util
{
    public class BeanFactory<B>
#if !USE_CONFCS
        where B : Zeze.Transaction.Bean
#else
        where B : ConfBean
#endif
    {
        private readonly ConcurrentDictionary<long, Func<B>> factories = new ConcurrentDictionary<long, Func<B>>();

        public B Create(long typeId)
        {
            return factories.TryGetValue(typeId, out var factory) ? factory() : null;
        }

        public void Register<T>() where T : B, new()
        {
            factories.TryAdd(new T().TypeId, () => new T());
        }
    }
}
