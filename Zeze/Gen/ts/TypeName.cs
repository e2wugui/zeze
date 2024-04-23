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

        public void Visit(TypeDecimal type)
        {
            name = "string";
        }

        public void Visit(TypeList type)
        {
            string valueName = GetName(type.ValueType);
            name = valueName + "[]";
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
            name = type.FullName.Replace(".", "_");
        }

        public void Visit(BeanKey type)
        {
            name = type.FullName.Replace(".", "_");
        }

        public void Visit(TypeDynamic type)
        {
            name = "Zeze.DynamicBean";
        }

        public void Visit(TypeVector2 type)
        {
            name = "Zeze.Vector2";
        }

        public void Visit(TypeVector2Int type)
        {
            name = "Zeze.Vector2";
        }

        public void Visit(TypeVector3 type)
        {
            name = "Zeze.Vector3";
        }

        public void Visit(TypeVector3Int type)
        {
            name = "Zeze.Vector3";
        }

        public void Visit(TypeVector4 type)
        {
            name = "Zeze.Vector4";
        }

        public void Visit(TypeQuaternion type)
        {
            name = "Zeze.Vector4";
        }
    }
}
