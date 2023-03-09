using Zeze.Gen.Types;

namespace Zeze.Gen.javadata
{
    public class TypeName : Visitor
    {
        protected string name;
        internal string nameCollectionImplement; // 容器内部类型。其他情况下为 null。
        internal string nameRaw; // 容器，其他为null。
        string nameOmitted = "";

        public static string GetName(Type type)
        {
            TypeName visitor = new();
            type.Accept(visitor);
            return visitor.name;
        }

        public static string GetNameOmitted(Type type)
        {
            var visitor = new TypeName();
            type.Accept(visitor);
            return visitor.nameOmitted;
        }

        public virtual void Visit(TypeBool type)
        {
            name = "boolean";
        }

        public virtual void Visit(TypeByte type)
        {
            name = "byte";
        }

        public virtual void Visit(TypeShort type)
        {
            name = "short";
        }

        public virtual void Visit(TypeInt type)
        {
            name = "int";
        }

        public virtual void Visit(TypeLong type)
        {
            name = "long";
        }

        public virtual void Visit(TypeFloat type)
        {
            name = "float";
        }

        public virtual void Visit(TypeDouble type)
        {
            name = "double";
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
            string valueName = BoxingName.GetBoxingName(type.ValueType);
            nameRaw = "java.util.ArrayList";
            nameOmitted = nameRaw;
            name = nameRaw + '<' + valueName + '>';
            nameCollectionImplement = "java.util.ArrayList<" + valueName + '>';
        }

        public virtual void Visit(TypeSet type)
        {
            string valueName = BoxingName.GetBoxingName(type.ValueType);
            nameRaw = "java.util.HashSet";
            nameOmitted = nameRaw;
            name = nameRaw + '<' + valueName + '>';
            nameCollectionImplement = "java.util.HashSet<" + valueName + '>';
        }

        public virtual void Visit(TypeMap type)
        {
            string key = BoxingName.GetBoxingName(type.KeyType);
            string value = BoxingName.GetBoxingName(type.ValueType);
            nameRaw = "java.util.HashMap";
            nameOmitted = nameRaw;
            name = nameRaw + '<' + key + ", " + value + '>';
            nameCollectionImplement = "java.util.HashMap<" + key + ", " + value + '>';
        }

        public virtual void Visit(Bean type)
        {
            name = type.FullName + "DaTa";
        }

        public virtual void Visit(BeanKey type)
        {
            name = type.FullName;
        }

        public virtual void Visit(TypeDynamic type)
        {
            name = "Zeze.Transaction.DynamicBeanData";
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
    }
}
