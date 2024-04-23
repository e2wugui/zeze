using Zeze.Gen.Types;

namespace Zeze.Gen.confcs
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
            if (type.FixSize >= 0 || type is TypeArray)
                nameCollectionImplement = name = valueName + "[]";
            else
                nameCollectionImplement = name = $"System.Collections.Generic.List<{valueName}>";
        }

        public void Visit(TypeSet type)
        {
            string valueName = GetName(type.ValueType);
            name = $"System.Collections.Generic.HashSet<{valueName}>";
            nameCollectionImplement = name;
        }

        public void Visit(TypeMap type)
        {
            string key = GetName(type.KeyType);
            string value = GetName(type.ValueType);
            name = $"System.Collections.Generic.Dictionary<{key}, {value}>";
            nameCollectionImplement = name;
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
            name = string.IsNullOrEmpty(type.DynamicParams.Base) ? "Zeze.Util.ConfBean" : type.DynamicParams.Base;
        }

        public void Visit(TypeQuaternion type)
        {
            name = Project.MakingInstance.IsUnity ? "UnityEngine.Quaternion" : "Zeze.Serialize.Quaternion";
        }

        public void Visit(TypeVector2 type)
        {
            name = Project.MakingInstance.IsUnity ? "UnityEngine.Vector2" : "Zeze.Serialize.Vector2";
        }

        public void Visit(TypeVector2Int type)
        {
            name = Project.MakingInstance.IsUnity ? "UnityEngine.Vector2Int" : "Zeze.Serialize.Vector2Int";
        }

        public void Visit(TypeVector3 type)
        {
            name = Project.MakingInstance.IsUnity ? "UnityEngine.Vector3" : "Zeze.Serialize.Vector3";
        }

        public void Visit(TypeVector3Int type)
        {
            name = Project.MakingInstance.IsUnity ? "UnityEngine.Vector3Int" : "Zeze.Serialize.Vector3Int";
        }

        public void Visit(TypeVector4 type)
        {
            name = Project.MakingInstance.IsUnity ? "UnityEngine.Vector4" : "Zeze.Serialize.Vector4";
        }

        public void Visit(TypeDecimal type)
        {
            name = "decimal";
        }
    }
}
