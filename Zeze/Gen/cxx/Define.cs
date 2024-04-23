using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cxx
{
    public class Define : Visitor
    {
        readonly string varname;
        readonly StreamWriter sw;
        readonly string prefix;

        public Define(string varname, StreamWriter sw, string prefix)
        {
            this.varname = varname;
            this.sw = sw;
            this.prefix = prefix;
        }

        void DefineStack(Type type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + typeName + " " + varname + ";");
        }

        public void Visit(TypeBool type)
        {
            DefineStack(type);
        }

        public void Visit(TypeByte type)
        {
            DefineStack(type);
        }

        public void Visit(TypeShort type)
        {
            DefineStack(type);
        }

        public void Visit(TypeInt type)
        {
            DefineStack(type);
        }

        public void Visit(TypeLong type)
        {
            DefineStack(type);
        }

        public void Visit(TypeFloat type)
        {
            DefineStack(type);
        }

        public void Visit(TypeDouble type)
        {
            DefineStack(type);
        }

        public void Visit(TypeBinary type)
        {
            DefineStack(type);
        }

        public void Visit(TypeString type)
        {
            DefineStack(type);
        }

        public void Visit(TypeList type)
        {
            DefineStack(type);
        }

        public void Visit(TypeSet type)
        {
            DefineStack(type);
        }

        public void Visit(TypeMap type)
        {
            DefineStack(type);
        }

        public void Visit(Bean type)
        {
            DefineStack(type);
        }

        public void Visit(BeanKey type)
        {
            DefineStack(type);
        }

        public void Visit(TypeDynamic type)
        {
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId)) // 判断一个就够了。
            {
                sw.WriteLine($"{prefix}Zeze::DynamicBean {varname}"
                    + $"({type.Variable.Bean.Name}::GetSpecialTypeIdFromBean_{type.Variable.Id}, "
                    + $"{type.Variable.Bean.Name}::CreateBeanFromSpecialTypeId_{type.Variable.Id});");
            }
            else
            {
                sw.WriteLine($"{prefix}Zeze::DynamicBean {varname}"
                    + $"({type.DynamicParams.GetSpecialTypeIdFromBean}, {type.DynamicParams.CreateBeanFromSpecialTypeId});");
            }
        }

        public void Visit(TypeQuaternion type)
        {
            DefineStack(type);
        }

        public void Visit(TypeVector2 type)
        {
            DefineStack(type);
        }

        public void Visit(TypeVector2Int type)
        {
            DefineStack(type);
        }

        public void Visit(TypeVector3 type)
        {
            DefineStack(type);
        }

        public void Visit(TypeVector3Int type)
        {
            DefineStack(type);
        }

        public void Visit(TypeVector4 type)
        {
            DefineStack(type);
        }

        public void Visit(TypeDecimal type)
        {
            DefineStack(type);
        }
    }
}
