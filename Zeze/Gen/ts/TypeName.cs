using Zeze.Gen.Types;

namespace Zeze.Gen.ts
{
    public class TypeName : Visitor
    {
        public string name;
        public string nameCollectionImplement; // 容器内部类型。其他情况下为 null。

        public static string GetName(Type type)
        {
            TypeName visitor = new();
            type.Accept(visitor);
            return visitor.name;
        }

        public void Visit(TypeBool type)
        {
            name = "boolean";
        }

        public void Visit(TypeByte type)
        {
            name = "number";
        }

        public void Visit(TypeShort type)
        {
            name = "number";
        }

        public void Visit(TypeInt type)
        {
            name = "number";
        }

        public void Visit(TypeLong type)
        {
            name = "bigint";
        }

        public void Visit(TypeFloat type)
        {
            name = "number";
        }

        public void Visit(TypeDouble type)
        {
            name = "number";
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
            string valueName = GetName(type.ValueType);
            name = "Array<" + valueName + ">";
        }

        public void Visit(TypeSet type)
        {
            string valueName = GetName(type.ValueType);
            name = "Set<" + valueName + ">";
        }

        public void Visit(TypeMap type)
        {
            string key = GetName(type.KeyType);
            string value = GetName(type.ValueType);
            name = "Map<" + key + ", " + value + ">";
        }

        public void Visit(Bean type)
        {
            name = type.Space.Path("_", type.Name);
        }

        public void Visit(BeanKey type)
        {
            name = type.Space.Path("_", type.Name);
        }

        public void Visit(TypeDynamic type)
        {
            name = "Zeze.DynamicBean";
        }
    }
}
