using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.confcs
{
    public class Construct : Visitor
    {
		readonly StreamWriter sw;
		readonly Variable variable;
		readonly string prefix;

		public static void Make(Bean bean, StreamWriter sw, string prefix)
		{
			// sw.WriteLine(prefix + "public " + bean.Name + "() : this(0)");
			// sw.WriteLine(prefix + "{");
			// sw.WriteLine(prefix + "}");
			// sw.WriteLine();
            sw.WriteLine(prefix + "public " + bean.Name + "()");
            sw.WriteLine(prefix + "{");
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new Construct(sw, var, prefix + "    "));
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "// for decode only");
            sw.WriteLine(prefix + "public " + bean.Name + "()");
            sw.WriteLine(prefix + "{");
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new Construct(sw, var, prefix + "    "));
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Construct(StreamWriter sw, Variable variable, string prefix)
		{
			this.sw = sw;
			this.variable = variable;
			this.prefix = prefix;
		}

		void Initial()
		{
            string value = variable.Initial;
			if (value.Length > 0)
			{
                string varname = variable.NameUpper1;
				sw.WriteLine(prefix + varname + " = " + value + ";");
			}
		}

        void InitialNew(Type type)
        {
            string value = variable.Initial;
            if (value.Length > 0)
            {
                string varname = variable.NameUpper1;
                sw.WriteLine($"{prefix}{varname} = new {TypeName.GetName(type)}({value});");
            }
        }

        public void Visit(Bean type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NameUpper1 + " = new " + typeName + "();");
        }

        public void Visit(BeanKey type)
        {
            string value = variable.Initial;
            if (value.Length > 0)
            {
                string varname = variable.NameUpper1;
                sw.WriteLine($"{prefix}{varname} = new {TypeName.GetName(type)}({value});");
            }
            else
            {
                sw.WriteLine($"{prefix}{variable.NameUpper1} = new {TypeName.GetName(type)}();");
            }
        }

        public void Visit(TypeByte type)
        {
            Initial();
        }

        public void Visit(TypeDouble type)
        {
            Initial();
        }

        public void Visit(TypeInt type)
        {
            Initial();
        }

        public void Visit(TypeLong type)
        {
            Initial();
        }

        public void Visit(TypeBool type)
        {
            Initial();
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + variable.NameUpper1 + " = Zeze.Net.Binary.Empty;");
        }

        public void Visit(TypeString type)
        {
            string value = variable.Initial;
            string varname = variable.NameUpper1;
            sw.WriteLine(prefix + varname + " = \"" + value + "\";");
        }

        public void Visit(TypeList type)
        {
            if (type.Variable.Type == "array")
                return;
            if (type.FixSize >= 0)
            {
                string typeName = TypeName.GetName(type);
                sw.WriteLine($"{prefix}{variable.NameUpper1} = new {typeName.Replace("[]", $"[{type.FixSize}]")};");
            }
            else
            {
                string typeName = TypeName.GetName(type);
                sw.WriteLine($"{prefix}{variable.NameUpper1} = new {typeName}();");
            }
        }

        public void Visit(TypeSet type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine($"{prefix}{variable.NameUpper1} = new {typeName}();");
        }

        public void Visit(TypeMap type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine($"{prefix}{variable.NameUpper1} = new {typeName}();");
        }

        public void Visit(TypeFloat type)
        {
            Initial();
        }

        public void Visit(TypeShort type)
        {
            Initial();
        }

        public void Visit(TypeDynamic type)
        {
            // if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId)) // 判断一个就够了。
            // {
            //     sw.WriteLine(prefix + variable.Name + " = new Zeze.Util.ConfDynamicBean"
            //         + $"({variable.Id}, GetSpecialTypeIdFromBean_{variable.Id}, CreateBeanFromSpecialTypeId_{variable.Id});");
            // }
            // else
            // {
            //     sw.WriteLine(prefix + variable.Name + " = new Zeze.Transaction.DynamicBean"
            //         + $"({variable.Id}, {type.DynamicParams.GetSpecialTypeIdFromBean}, {type.DynamicParams.CreateBeanFromSpecialTypeId});");
            // }
        }

        public void Visit(TypeQuaternion type)
        {
            InitialNew(type);
        }

        public void Visit(TypeVector2 type)
        {
            InitialNew(type);
        }

        public void Visit(TypeVector2Int type)
        {
            InitialNew(type);
        }

        public void Visit(TypeVector3 type)
        {
            InitialNew(type);
        }

        public void Visit(TypeVector3Int type)
        {
            InitialNew(type);
        }

        public void Visit(TypeVector4 type)
        {
            InitialNew(type);
        }
    }
}
