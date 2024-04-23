using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cxx
{
    public class ParamName : Visitor
    {
        public static string GetParamList(ICollection<Types.Variable> variables)
        {
            StringBuilder plist = new();
            bool first = true;
            foreach (Types.Variable var in variables)
            {
                if (first)
                    first = false;
                else
                    plist.Append(", ");
                plist.Append(GetName(var.VariableType)).Append(" ").Append(var.NameUpper1).Append('_');
            }
            return plist.ToString();
        }

        protected string name;
        internal string nameCollectionImplement; // 容器内部类型。其他情况下为 null。
        internal string nameRaw; // 容器，其他为null。
        string nameOmitted = "";

        public static string GetName(Type type)
        {
            ParamName visitor = new();
            type.Accept(visitor);
            return visitor.name;
        }

        public static string GetNameOmitted(Type type)
        {
            var visitor = new ParamName();
            type.Accept(visitor);
            return visitor.nameOmitted;
        }

        public virtual void Visit(TypeBool type)
        {
            name = "bool";
        }

        public virtual void Visit(TypeByte type)
        {
            name = "char";
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
            name = "int64_t";
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
            name = "const std::string&";
        }

        public virtual void Visit(TypeString type)
        {
            name = "const std::string&";
        }

        public virtual void Visit(TypeList type)
        {
            string valueName = GetName(type.ValueType);
            nameRaw = "std::vector";
            nameOmitted = nameRaw;
            name = "const " + nameRaw + '<' + valueName + ">&";
            nameCollectionImplement = "std::vector<" + valueName + '>';
        }

        public virtual void Visit(TypeSet type)
        {
            string valueName = GetName(type.ValueType);
            nameRaw = "std::set";
            nameOmitted = nameRaw;
            name = "const " + nameRaw + '<' + valueName + ">&";
            nameCollectionImplement = "std::set<" + valueName + '>';
        }

        public virtual void Visit(TypeMap type)
        {
            string key = GetName(type.KeyType);
            string value = GetName(type.ValueType);
            nameRaw = "std::map";
            nameOmitted = nameRaw;
            name = "const " + nameRaw + '<' + key + ", " + value + ">&";
            nameCollectionImplement = "std::map<" + key + ", " + value + '>';
        }

        public virtual void Visit(Bean type)
        {
            name = type.FullCxxName;
        }

        public virtual void Visit(BeanKey type)
        {
            name = type.FullCxxName;
        }

        public virtual void Visit(TypeDynamic type)
        {
            name = "const Zeze::DynamicBean&";
        }

        public void Visit(TypeQuaternion type)
        {
            name = "const Zeze::Quaternion&";
        }

        public void Visit(TypeVector2 type)
        {
            name = "const Zeze::Vector2&";
        }

        public void Visit(TypeVector2Int type)
        {
            name = "const Zeze::Vector2Int&";
        }

        public void Visit(TypeVector3 type)
        {
            name = "const Zeze::Vector3&";
        }

        public void Visit(TypeVector3Int type)
        {
            name = "const Zeze::Vector3Int&";
        }

        public void Visit(TypeVector4 type)
        {
            name = "const Zeze::Vector4&";
        }

        public void Visit(TypeDecimal type)
        {
            name = "const std::string&";
        }
    }
}
