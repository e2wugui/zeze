using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Gen.Types;

namespace Zeze.Gen.rrjava
{
    public class LogName : Visitor
    {
        public static string GetName(Types.Type type)
        {
            var visitor = new LogName();
            type.Accept(visitor);
            return visitor.Name;
        }

        public void Visit(TypeBool type)
        {
            Name = "Zeze.Raft.RocksRaft.Log1.LogBool";
        }

        public void Visit(TypeByte type)
        {
            Name = "Zeze.Raft.RocksRaft.Log1.LogByte";
        }

        public void Visit(TypeShort type)
        {
            Name = "Zeze.Raft.RocksRaft.Log1.LogShort";
        }

        public void Visit(TypeInt type)
        {
            Name = "Zeze.Raft.RocksRaft.Log1.LogInt";
        }

        public void Visit(TypeLong type)
        {
            Name = "Zeze.Raft.RocksRaft.Log1.LogLong";
        }

        public void Visit(TypeFloat type)
        {
            Name = "Zeze.Raft.RocksRaft.Log1.LogFloat";
        }

        public void Visit(TypeDouble type)
        {
            Name = "Zeze.Raft.RocksRaft.Log1.LogDouble";
        }

        public void Visit(TypeBinary type)
        {
            Name = "Zeze.Raft.RocksRaft.Log1.LogBinary";
        }

        public void Visit(TypeString type)
        {
            Name = "Zeze.Raft.RocksRaft.Log1.LogString";
        }

        public void Visit(TypeList type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeSet type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeMap type)
        {
            throw new NotImplementedException();
        }

        public void Visit(Bean type)
        {
            throw new NotImplementedException();
        }

        public void Visit(BeanKey type)
        {
            Name = $"Zeze.Raft.RocksRaft.Log1.LogBeanKey<{TypeName.GetName(type)}>";
        }

        public void Visit(TypeDynamic type)
        {
            throw new NotImplementedException();
        }

        public string Name { get; set; }

    }
}
