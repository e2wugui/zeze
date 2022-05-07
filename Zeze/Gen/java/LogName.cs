using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
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
            Name = "Zeze.Transaction.Logs.LogBool";
        }

        public void Visit(TypeByte type)
        {
            Name = "Zeze.Transaction.Logs.LogByte";
        }

        public void Visit(TypeShort type)
        {
            Name = "Zeze.Transaction.Logs.LogShort";
        }

        public void Visit(TypeInt type)
        {
            Name = "Zeze.Transaction.Logs.LogInt";
        }

        public void Visit(TypeLong type)
        {
            Name = "Zeze.Transaction.Logs.LogLong";
        }

        public void Visit(TypeFloat type)
        {
            Name = "Zeze.Transaction.Logs.LogFloat";
        }

        public void Visit(TypeDouble type)
        {
            Name = "Zeze.Transaction.Logs.LogDouble";
        }

        public void Visit(TypeBinary type)
        {
            Name = "Zeze.Transaction.Logs.LogBinary";
        }

        public void Visit(TypeString type)
        {
            Name = "Zeze.Transaction.Logs.LogString";
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
            Name = $"Zeze.Transaction.Logs.Log1.LogBeanKey<{TypeName.GetName(type)}>";
        }

        public void Visit(TypeDynamic type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeQuaternion type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector2 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector2Int type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector3 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector3Int type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector4 type)
        {
            throw new NotImplementedException();
        }

        public string Name { get; set; }

    }
}
