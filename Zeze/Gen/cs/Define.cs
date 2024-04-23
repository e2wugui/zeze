using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
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

        void DefineNew(Type type)
        {
            string tName = Project.MakingInstance.Platform.StartsWith("conf+cs") ? confcs.TypeName.GetName(type) : TypeName.GetName(type);
            sw.WriteLine(prefix + "var " + varname + " = new " + tName + "();");
        }

        void DefineStack(Type type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + typeName + " " + varname + ";");
        }

        public void Visit(Bean type)
        {
            DefineNew(type);
        }

        public void Visit(BeanKey type)
        {
            DefineNew(type);
        }

        public void Visit(TypeByte type)
        {
            DefineStack(type);
        }

        public void Visit(TypeDouble type)
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

        public void Visit(TypeBool type)
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
            DefineNew(type);
        }

        public void Visit(TypeSet type)
        {
            DefineNew(type);
        }

        public void Visit(TypeMap type)
        {
            DefineNew(type);
        }

        public void Visit(TypeFloat type)
        {
            DefineStack(type);
        }

        public void Visit(TypeShort type)
        {
            DefineStack(type);
        }

        public void Visit(TypeDynamic type)
        {
            if (Project.MakingInstance.Platform.StartsWith("conf+cs"))
            {
                sw.WriteLine($"{prefix}{confcs.TypeName.GetName(type)} {varname} = null;");
            }
            else if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId)) // 判断一个就够了。
            {
                sw.WriteLine($"{prefix}var {varname} = new Zeze.Transaction.DynamicBean"
                    + $"(0, GetSpecialTypeIdFromBean_{type.Variable.Id}, CreateBeanFromSpecialTypeId_{type.Variable.Id});");
            }
            else
            {
                sw.WriteLine($"{prefix}var {varname} = new Zeze.Transaction.DynamicBean"
                    + $"(0, {type.DynamicParams.GetSpecialTypeIdFromBeanCsharp}, {type.DynamicParams.CreateBeanFromSpecialTypeIdCsharp});");
            }
        }

        public void Visit(TypeQuaternion type)
        {
            DefineNew(type);
        }

        public void Visit(TypeVector2 type)
        {
            DefineNew(type);
        }

        public void Visit(TypeVector2Int type)
        {
            DefineNew(type);
        }

        public void Visit(TypeVector3 type)
        {
            DefineNew(type);
        }

        public void Visit(TypeVector3Int type)
        {
            DefineNew(type);
        }

        public void Visit(TypeVector4 type)
        {
            DefineNew(type);
        }

        public void Visit(TypeDecimal type)
        {
            DefineStack(type);
        }
    }
}
