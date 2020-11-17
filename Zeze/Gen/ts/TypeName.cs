using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.ts
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
            name = type.Space.Path("_", type.Name);
        }

        public void Visit(BeanKey type)
        {
            name = type.Space.Path("_", type.Name);
        }

        public void Visit(TypeByte type)
        {
            name = "number";
        }

        public void Visit(TypeDouble type)
        {
            name = "number";
        }

        public void Visit(TypeInt type)
        {
            name = "number";
        }

        public void Visit(TypeLong type)
        {
            name = "Long";
        }

        public void Visit(TypeBool type)
        {
            name = "boolean";
        }

        public void Visit(TypeBinary type)
        {
            name = "Uint8Array";
        }

        public void Visit(TypeString type)
        {
            name = "string";
        }

        public void Visit(TypeList type)
        {
            string valueName = TypeName.GetName(type.ValueType);
            name = "Array<" + valueName + ">";
        }

        public void Visit(TypeSet type)
        {
            string valueName = TypeName.GetName(type.ValueType);
            name = "Set<" + valueName + ">";
        }

        public void Visit(TypeMap type)
        {
            string key = TypeName.GetName(type.KeyType);
            string value = TypeName.GetName(type.ValueType);
            name = "Map<" + key + ", " + value + ">";
        }

        public void Visit(TypeFloat type)
        {
            name = "number";
        }

        public void Visit(TypeShort type)
        {
            name = "number";
        }

        public void Visit(TypeDynamic type)
        {
            name = "Zeze.Bean";
        }
    }
}
