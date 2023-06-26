using System.Collections.Concurrent;
using System.Collections.Generic;

namespace Zeze.Util
{
    public static class ConcurrentDictionaryExtension
    {
        // .NET 4.x 不支持这个接口，先实现一个不太安全的
        public static bool TryRemove<TKey, TValue>(this ConcurrentDictionary<TKey, TValue> map,
            KeyValuePair<TKey, TValue> pair)
        {
            for (;;)
            {
                if (!map.TryGetValue(pair.Key, out var v1) || !v1.Equals(pair.Value))
                    return false;
                if (!map.TryRemove(pair.Key, out var v2))
                    return false;
                if (v2.Equals(pair.Value))
                    return true;
                if (map.TryAdd(pair.Key, v2))
                    return false;
            }
        }
    }
}
