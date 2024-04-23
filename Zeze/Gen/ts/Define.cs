using Zeze.Gen.Types;

namespace Zeze.Gen.ts
{
    public class Define : Visitor
    {
        private readonly string varname;
        private readonly System.IO.StreamWriter sw;
        private readonly string prefix;

        public Define(string varname, System.IO.StreamWriter sw, string prefix)
        {
            this.varname = varname;
            this.sw = sw;
            this.prefix = prefix;
        }

        private void DefineNew(Type type)
        {
            string tName = TypeName.GetName(type);
            sw.WriteLine(prefix + "const " + varname + ": " + tName + " = new " + tName + "();");
        }

        private void DefineStack(Type type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "let " + varname + ": " + typeName + ";");
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

        public void Visit(TypeDecimal type)
        {
            DefineStack(type);
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + "const " + varname + ": " + TypeName.GetName(type) + " = [];");
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
            // string tName = TypeName.GetName(type);
            var bean = (Bean)type.Variable.Bean;
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId)) // 判断一个就够了。
            {
                sw.WriteLine($"{prefix}const {varname} = new Zeze.DynamicBean("
                + $"{bean.Space.Path("_", bean.Name)}.GetSpecialTypeIdFromBean_{type.Variable.Id}, "
                + $"{bean.Space.Path("_", bean.Name)}.CreateBeanFromSpecialTypeId_{type.Variable.Id}"
                + ");");
            }
            else
            {
                sw.WriteLine($"{prefix}const {varname} = new Zeze.DynamicBean"
                    + $"(0, {type.DynamicParams.GetSpecialTypeIdFromBeanCsharp}, {type.DynamicParams.CreateBeanFromSpecialTypeIdCsharp});");
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
    }
}
