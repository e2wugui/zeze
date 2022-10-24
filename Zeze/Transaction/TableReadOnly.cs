using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Serialize;

namespace Zeze.Transaction
{
    // V is BeanReadOnly Interface
    public interface TableReadOnly<K, V, VReadOnly>
    {
        public Binary EncodeGlobalKey(K key);
        public Task<VReadOnly> GetAsync(K key);
        public Task<bool> ContainsKey(K key);
        public ByteBuffer EncodeKey(K key);
        public ByteBuffer EncodeKey(Object key);
        public K DecodeKey(ByteBuffer bb);
        public V NewValue();
        public V DecodeValue(ByteBuffer bb);
        public Task<long> WalkAsync(Func<K, V, bool> callback);
        public Task<long> WalkAsync(Func<K, V, bool> callback, Action afterLock);
        public long WalkCacheKey(Func<K, bool> callback);
        public Task<long> WalkDatabaseKeyAsync(Func<K, bool> callback);
        public Task<long> WalkDatabaseAsync(Func<byte[], byte[], bool> callback);
        public Task<long> WalkDatabaseAsync(Func<K, V, bool> callback);
        public long WalkCache(Func<K, V, bool> callback);
        public long WalkCache(Func<K, V, bool> callback, Action afterLock);
        public Task<V> SelectCopyAsync(K key);
        public Task<VReadOnly> SelectDirtyAsync(K key);
        public ByteBuffer EncodeChangeListenerWithSpecialName(string specialName, object key, Changes.Record r);
    }
}
