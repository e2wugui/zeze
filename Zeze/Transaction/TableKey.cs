using System;
using System.Collections.Generic;
using Zeze.Serialize;

namespace Zeze.Transaction
{
    public sealed class TableKey : IComparable<TableKey>
    {
        public string Name { get; }
        public object Key { get; } // 只能是简单变量(bool,byte,short,int,long)和BeanKey

        public TableKey(string name, object key)
        {
            Name = name;
            Key = key;
        }

        public int CompareTo(TableKey other)
        {
            int c = this.Name.CompareTo(other.Name);
            if (c != 0)
            {
                return c;
            }
            return Comparer<IComparable>.Default.Compare((IComparable)Key, (IComparable)other.Key);
        }

        public override string ToString()
        {
            return $"tkey{{{Name},{Key}}}";
        }

        public override int GetHashCode()
        {
            const int prime = 31;
            int result = 17;
            result = prime * result + Name.GetHashCode();
            result = prime * result + Key.GetHashCode();
            return result;
        }

        public override bool Equals(object obj)
        {
            if (this == obj)
                return true;

            if (obj is TableKey another)
            {
                return Name.Equals(another.Name) && Key.Equals(another.Key);
            }
            return false;
        }
    }
}
