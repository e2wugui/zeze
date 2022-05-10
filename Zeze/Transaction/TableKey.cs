using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using Zeze.Serialize;

namespace Zeze.Transaction
{
    public sealed class TableKey : IComparable<TableKey>
    {
        // 用来记录名字转换，不检查Table.Id唯一性。
        public static ConcurrentDictionary<int, string> Tables { get; } = new();

        public int Id { get; }
        public object Key { get; } // 只能是简单变量(bool,byte,short,int,long)和BeanKey

        public TableKey(int id, object key)
        {
            Id = id;
            Key = key;
        }

        public int CompareTo(TableKey other)
        {
            int c = this.Id.CompareTo(other.Id);
            if (c != 0)
            {
                return c;
            }
            return Comparer<IComparable>.Default.Compare((IComparable)Key, (IComparable)other.Key);
        }

        public override string ToString()
        {
            Tables.TryGetValue(Id, out var name);
            return $"tkey{{{name},{Key}}}";
        }

        public override int GetHashCode()
        {
            const int prime = 31;
            int result = 17;
            result = prime * result + Id.GetHashCode();
            result = prime * result + Key.GetHashCode();
            return result;
        }

        public override bool Equals(object obj)
        {
            if (this == obj)
                return true;

            if (obj is TableKey another)
            {
                return Id.Equals(another.Id) && Key.Equals(another.Key);
            }
            return false;
        }
    }
}
