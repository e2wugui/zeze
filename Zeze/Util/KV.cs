using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
    /// <summary>
    /// 默认的KeyValuePair有 .net core .net framework 版本api不同的问题，自己定义一个吧。
    /// </summary>

    public class KV<K, V>
    {
        public K Key { get; set; }
        public V Value { get; set; }
    }

    public class KV
    {
        public static KV<TK, TV> Create<TK, TV>(TK key, TV value)
        {
            return new KV<TK, TV>() { Key = key, Value = value };
        }
    }
}
