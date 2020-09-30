using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class TypeName : Types.Visitor
    {
        public string name;
        public string nameCollectionImplement; // 容器内部类型。其他情况下为 null。

        public static string GetName(Types.Type type)
        {
            var visitor = new TypeName();
            type.Accept(visitor);
            return visitor.name;
        }

        public void Visit(Bean type)
        {
            name = type.Space.Path(".", type.Name);
        }

        public void Visit(BeanKey type)
        {
            name = type.Space.Path(".", type.Name);
        }

        public void Visit(TypeByte type)
        {
            name = "byte";
        }

        public void Visit(TypeDouble type)
        {
            name = "double";
        }

        public void Visit(TypeInt type)
        {
            name = "int";
        }

        public void Visit(TypeLong type)
        {
            name = "long";
        }

        public void Visit(TypeBool type)
        {
            name = "bool";
        }

        public void Visit(TypeBinary type)
        {
            name = "byte[]";
        }

        public void Visit(TypeString type)
        {
            name = "string";
        }

        public void Visit(TypeList type)
        {
            string valueName = TypeName.GetName(type.ValueType);
            name = "Zeze.Transaction.Collections.PList" + (type.ValueType.IsNormalBean ? "2<" : "1<")  + valueName + ">";
            nameCollectionImplement = "System.Collections.Immutable.ImmutableList<" + valueName + ">";
        }

        public void Visit(TypeSet type)
        {
            string valueName = TypeName.GetName(type.ValueType);
            name = "Zeze.Transaction.Collections.PSet1<" + valueName + ">";
            nameCollectionImplement = "System.Collections.Immutable.ImmutableHashSet<" + valueName + ">";
        }

        public void Visit(TypeMap type)
        {
            string key = TypeName.GetName(type.KeyType);
            string value = TypeName.GetName(type.ValueType);
            name = "Zeze.Transaction.Collections.PMap" + (type.ValueType.IsNormalBean ? "2<" : "1<") + key + ", " + value + ">";
            nameCollectionImplement = "System.Collections.Immutable.ImmutableDictionary<" + key + ", " + value + ">";
        }

        public void Visit(TypeFloat type)
        {
            name = "float";
        }

        public void Visit(TypeShort type)
        {
            name = "short";
        }

        public void Visit(TypeDynamic type)
        {
            name = "Zeze.Transaction.Bean";
        }
    }
}
