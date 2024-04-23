using System;
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
            Name = $"Zeze.Transaction.Logs.LogBeanKey<{TypeName.GetName(type)}>";
        }

        public void Visit(TypeDynamic type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeQuaternion type)
        {
            Name = "Zeze.Transaction.Logs.LogQuaternion";
        }

        public void Visit(TypeVector2 type)
        {
            Name = "Zeze.Transaction.Logs.LogVector2";
        }

        public void Visit(TypeVector2Int type)
        {
            Name = "Zeze.Transaction.Logs.LogVector2Int";
        }

        public void Visit(TypeVector3 type)
        {
            Name = "Zeze.Transaction.Logs.LogVector3";
        }

        public void Visit(TypeVector3Int type)
        {
            Name = "Zeze.Transaction.Logs.LogVector3Int";
        }

        public void Visit(TypeVector4 type)
        {
            Name = "Zeze.Transaction.Logs.LogVector4";
        }

        public void Visit(TypeDecimal type)
        {
            Name = "Zeze.Transaction.Logs.LogDecimal";
        }

        public string Name { get; set; }

    }
}
