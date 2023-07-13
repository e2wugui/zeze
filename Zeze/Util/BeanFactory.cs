
using System;
using System.Collections.Concurrent;

namespace Zeze.Util
{
    public class BeanFactory<B>
#if !USE_CONFCS
        where B : Zeze.Transaction.Bean, new()
#else
        where B : Zeze.Util.ConfBean, new()
#endif
    {
        private readonly ConcurrentDictionary<long, Func<B>> factories = new();

        public B Create(long typeId)
        {
            if (factories.TryGetValue(typeId, out var factory))
                return factory();
            return null;
        }

        public void Register<T>()
            where T : B, new()
        {
            factories.TryAdd(new T().TypeId, () => new T());
        }
    }
}
