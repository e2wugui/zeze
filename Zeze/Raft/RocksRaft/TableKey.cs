using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
    public class TableKey : IComparable<TableKey>
    {
        public string Name;
        public object Key;

        public TableKey()
        {

        }

        public TableKey(string name, object key)
        {
            Name = name;
            Key = key;
        }

        public override string ToString()
        {
            return $"({Name},{Key})";
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

        public int CompareTo(TableKey other)
        {
            int c = this.Name.CompareTo(other.Name);
            if (c != 0)
            {
                return c;
            }
            return Comparer<IComparable>.Default.Compare((IComparable)Key, (IComparable)other.Key);
        }
    }
}
