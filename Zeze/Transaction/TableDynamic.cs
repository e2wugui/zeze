using System;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Transaction
{
    public class TableDynamic<K, V> : Table<K, V>
        where V : Bean, new()
    {
        private int id;
        public override int Id => id;

        public Func<K, ByteBuffer> KeyEncoder { get; }
        public Func<ByteBuffer, K> KeyDecoder { get; }
        private bool isAutoKey;

        public TableDynamic(Zeze.Application zeze, string tableName,
            Func<K, ByteBuffer> keyEncoder,
            Func<ByteBuffer, K> keyDecoder,
            bool isAutoKey,
            string confTableName)
            : base(tableName)
        {
            id = FixedHash.Hash32(tableName);

            KeyEncoder = keyEncoder;
            KeyDecoder = keyDecoder;
            this.isAutoKey = isAutoKey;

            confTableName = string.IsNullOrEmpty(confTableName) ? tableName : confTableName;
            zeze.OpenDynamicTable(zeze.Config.GetTableConf(confTableName).DatabaseName, this);
        }

        public override ByteBuffer EncodeKey(K key)
        {
            return KeyEncoder(key);
        }

        public override K DecodeKey(ByteBuffer bb)
        {
            return KeyDecoder(bb);
        }

        public override bool IsMemory => false;

        public override bool IsAutoKey => isAutoKey;
    }
}
