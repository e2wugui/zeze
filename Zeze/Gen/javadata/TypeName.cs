using System;
using Zeze.Gen.Types;
using Type = Zeze.Gen.Types.Type;

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
            if (!string.IsNullOrEmpty(type.Variable.JavaType))
            {
                if (valueName == "Integer" && type.Variable.JavaType == "IntList")
                    nameRaw = nameOmitted = name = nameCollectionImplement = "Zeze.Util.IntList";
                else if (valueName == "Long" && type.Variable.JavaType == "LongList")
                    nameRaw = nameOmitted = name = nameCollectionImplement = "Zeze.Util.LongList";
                else if (valueName == "Float" && type.Variable.JavaType == "FloatList")
                    nameRaw = nameOmitted = name = nameCollectionImplement = "Zeze.Util.FloatList";
                else if (valueName == "Zeze.Serialize.Vector2" && type.Variable.JavaType == "Vector2List")
                    nameRaw = nameOmitted = name = nameCollectionImplement = "Zeze.Util.Vector2List";
                else if (valueName == "Zeze.Serialize.Vector3" && type.Variable.JavaType == "Vector3List")
                    nameRaw = nameOmitted = name = nameCollectionImplement = "Zeze.Util.Vector3List";
                else if (valueName == "Zeze.Serialize.Vector4" && type.Variable.JavaType == "Vector4List")
                    nameRaw = nameOmitted = name = nameCollectionImplement = "Zeze.Util.Vector4List";
                else if (valueName == "Zeze.Serialize.Vector2Int" && type.Variable.JavaType == "Vector2IntList")
                    nameRaw = nameOmitted = name = nameCollectionImplement = "Zeze.Util.Vector2IntList";
                else if (valueName == "Zeze.Serialize.Vector3Int" && type.Variable.JavaType == "Vector3IntList")
                    nameRaw = nameOmitted = name = nameCollectionImplement = "Zeze.Util.Vector3IntList";
                else
                {
                    throw new NotImplementedException($"invalid javaType: {type.Variable.JavaType}" +
                                                      $" for var: {type.Variable.Name}");
                }
            }
            else
            {
                nameRaw = "java.util.ArrayList";
                nameOmitted = nameRaw;
                name = nameRaw + '<' + valueName + '>';
                nameCollectionImplement = name;
            }
        }

        public virtual void Visit(TypeSet type)
        {
            string valueName = BoxingName.GetBoxingName(type.ValueType);
            if (!string.IsNullOrEmpty(type.Variable.JavaType))
            {
                if (valueName == "Integer" && type.Variable.JavaType == "IntHashSet")
                    nameRaw = nameOmitted = name = nameCollectionImplement = "Zeze.Util.IntHashSet";
                else if (valueName == "Long" && type.Variable.JavaType == "LongHashSet")
                    nameRaw = nameOmitted = name = nameCollectionImplement = "Zeze.Util.LongHashSet";
                else
                {
                    throw new NotImplementedException($"invalid javaType: {type.Variable.JavaType}" +
                                                      $" for var: {type.Variable.Name}, {type.Variable.Type}");
                }
            }
            else
            {
                nameRaw = "java.util.HashSet";
                nameOmitted = nameRaw;
                name = nameRaw + '<' + valueName + '>';
                nameCollectionImplement = name;
            }
        }

        public virtual void Visit(TypeMap type)
        {
            string key = BoxingName.GetBoxingName(type.KeyType);
            string value = BoxingName.GetBoxingName(type.ValueType);
            if (!string.IsNullOrEmpty(type.Variable.JavaType))
            {
                if (key == "Integer" && type.Variable.JavaType == "IntHashMap")
                {
                    nameRaw = nameOmitted = "Zeze.Util.IntHashMap";
                    name = nameCollectionImplement = nameRaw + '<' + value + '>';
                }
                else if (key == "Long" && type.Variable.JavaType == "LongHashMap")
                {
                    nameRaw = nameOmitted = "Zeze.Util.LongHashMap";
                    name = nameCollectionImplement = nameRaw + '<' + value + '>';
                }
                else
                {
                    throw new NotImplementedException($"invalid javaType: {type.Variable.JavaType}" +
                                                      $" for var: {type.Variable.Name}, {type.Variable.Type}");
                }
            }
            else
            {
                nameRaw = "java.util.HashMap";
                nameOmitted = nameRaw;
                name = nameRaw + '<' + key + ", " + value + '>';
                nameCollectionImplement = name;
            }
        }

        public virtual void Visit(Bean type)
        {
            name = type.OnlyData ? type.FullName : type.FullName + ".Data";
        }

        public virtual void Visit(BeanKey type)
        {
            name = type.FullName;
        }

        public virtual void Visit(TypeDynamic type)
        {
            name = "DynamicData_" + type.Variable.Name;
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
            name = "java.math.BigDecimal";
        }
    }
}
