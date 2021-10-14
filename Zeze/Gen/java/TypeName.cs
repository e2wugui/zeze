using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class TypeName : Types.Visitor
    {
        public string name;
        public string nameCollectionImplement; // 容器内部类型。其他情况下为 null。
        public string nameRaw; // 容器，其他为null。

        public static string GetName(Types.Type type)
        {
            var visitor = new TypeName();
            type.Accept(visitor);
            return visitor.name;
        }

        public virtual void Visit(Bean type)
        {
            name = type.Space.Path(".", type.Name);
        }

        public virtual void Visit(BeanKey type)
        {
            name = type.Space.Path(".", type.Name);
        }

        public virtual void Visit(TypeByte type)
        {
            name = "byte";
        }

        public virtual void Visit(TypeDouble type)
        {
            name = "double";
        }

        public virtual void Visit(TypeInt type)
        {
            name = "int";
        }

        public virtual void Visit(TypeLong type)
        {
            name = "long";
        }

        public virtual void Visit(TypeBool type)
        {
            name = "boolean";
        }

        public virtual void Visit(TypeBinary type)
        {
            name = "Zeze.Net.Binary";
        }

        public virtual void Visit(TypeString type)
        {
            name = "String";
        }

        public virtual void Visit(TypeList type)
        {
            string valueName = BoxingName.GetName(type.ValueType);
            nameRaw = "Zeze.Transaction.Collections.PList";
            name = nameRaw + (type.ValueType.IsNormalBean ? "2<" : "1<")  + valueName + ">";
            nameCollectionImplement = "org.pcollections.PVector<" + valueName + ">";
        }

        public virtual void Visit(TypeSet type)
        {
            string valueName = BoxingName.GetName(type.ValueType);
            nameRaw = "Zeze.Transaction.Collections.PSet";
            name = nameRaw + "1<" + valueName + " >";
            nameCollectionImplement = "org.pcollections.PSet<" + valueName + ">";
        }

        public virtual void Visit(TypeMap type)
        {
            string key = BoxingName.GetName(type.KeyType);
            string value = BoxingName.GetName(type.ValueType);
            nameRaw = "Zeze.Transaction.Collections.PMap";
            name = nameRaw + (type.ValueType.IsNormalBean ? "2<" : "1<") + key + ", " + value + ">";
            nameCollectionImplement = "org.pcollections.PMap<" + key + ", " + value + ">";
        }

        public virtual void Visit(TypeFloat type)
        {
            name = "float";
        }

        public virtual void Visit(TypeShort type)
        {
            name = "short";
        }

        public virtual void Visit(TypeDynamic type)
        {
            name = "Zeze.Transaction.DynamicBean";
        }
    }
}
