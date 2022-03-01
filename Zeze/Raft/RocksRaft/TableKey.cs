using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
    public class TableKey : Serializable
    {
        public string Name { get; set; }
        public object Key { get; set; }

        public TableKey()
        {

        }

        public TableKey(string name, object key)
        {
            Name = name;
            Key = key;
        }

        public void Decode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }

        public void Encode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }

        public override string ToString()
        {
            return $"({Name},{Key})";
        }
    }
}
