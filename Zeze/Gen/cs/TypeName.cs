using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class TypeName : Visitor
    {
        internal string name;
        internal string nameCollectionImplement; // 容器内部类型。其他情况下为 null。

        public static string GetName(Type type)
        {
            TypeName visitor = new();
            type.Accept(visitor);
            return visitor.name;
        }

        public void Visit(TypeBool type)
        {
            name = "bool";
        }

        public void Visit(TypeByte type)
        {
            name = "byte";
        }

        public void Visit(TypeShort type)
        {
            name = "short";
        }

        public void Visit(TypeInt type)
        {
            name = "int";
        }

        public void Visit(TypeLong type)
        {
            name = "long";
        }

        public void Visit(TypeFloat type)
        {
            name = "float";
        }

        public void Visit(TypeDouble type)
        {
            name = "double";
        }

        public void Visit(TypeBinary type)
        {
            name = "Zeze.Net.Binary";
        }

        public void Visit(TypeString type)
        {
            name = "string";
        }

        public void Visit(TypeList type)
        {
            string valueName = GetName(type.ValueType);
            name = "Zeze.Transaction.Collections.CollList" + (type.ValueType.IsNormalBean ? "2<" : "1<")  + valueName + ">";
            nameCollectionImplement = "System.Collections.Immutable.ImmutableList<" + valueName + ">";
        }

        public void Visit(TypeSet type)
        {
            string valueName = GetName(type.ValueType);
            name = "Zeze.Transaction.Collections.CollSet1<" + valueName + ">";
            nameCollectionImplement = "System.Collections.Immutable.ImmutableHashSet<" + valueName + ">";
        }

        public void Visit(TypeMap type)
        {
            string key = GetName(type.KeyType);
            string value = GetName(type.ValueType);
            name = "Zeze.Transaction.Collections.CollMap" + (type.ValueType.IsNormalBean ? "2<" : "1<") + key + ", " + value + ">";
            nameCollectionImplement = "System.Collections.Immutable.ImmutableDictionary<" + key + ", " + value + ">";
        }

        public void Visit(Bean type)
        {
            name = type.FullName;
        }

        public void Visit(BeanKey type)
        {
            name = type.FullName;
        }

        public void Visit(TypeDynamic type)
        {
            name = "Zeze.Transaction.DynamicBean";
        }

        public void Visit(TypeQuaternion type)
        {
            name = "Zeze.Serialize.Quaternion";
        }

        public void Visit(TypeVector2 type)
        {
            name = "Zeze.Serialize.Vector2";
        }

        public void Visit(TypeVector2Int type)
        {
            name = "Zeze.Serialize.Vector2Int";
        }

        public void Visit(TypeVector3 type)
        {
            name = "Zeze.Serialize.Vector3";
        }

        public void Visit(TypeVector3Int type)
        {
            name = "Zeze.Serialize.Vector3Int";
        }

        public void Visit(TypeVector4 type)
        {
            name = "Zeze.Serialize.Vector4";
        }

        public void Visit(TypeDecimal type)
        {
            name = "decimal";
        }
    }
}
