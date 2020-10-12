using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using Microsoft.VisualBasic.CompilerServices;

namespace Zeze.Transaction
{
    public sealed class TableKey : IComparable<TableKey>
    {
        public int TableId { get; }
        public object Key { get; } // 只能是简单变量(bool,byte,short,int,long)和BeanKey

        public TableKey(int tableId, object key)
        {
            TableId = tableId;
            Key = key;
        }

        public int CompareTo([AllowNull] TableKey other)
        {
            int c = this.TableId.CompareTo(other.TableId);
            if (c != 0)
            {
                return c;
            }
            return Comparer<IComparable>.Default.Compare((IComparable)Key, (IComparable)other.Key);
        }

        public override string ToString()
        {
            return $"tkey{{{TableId}:{Table.GetTable(TableId).Name},{Key}}}";
        }

        public override int GetHashCode()
        {
            return TableId + Key.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (this == obj)
                return true;

            if (obj is TableKey another)
            {
                return TableId == another.TableId && Key.Equals(another.Key);
            }
            return false;
        }
    }
}
