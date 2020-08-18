using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using Microsoft.VisualBasic.CompilerServices;

namespace Zeze.Transaction
{
    public class TableKey : IComparable<TableKey>
    {
        public int TableId { get; }
        public object Key { get; }

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
            return $"tkey{{{Table.GetTable(TableId).Name},{Key}}}";
        }
    }
}
