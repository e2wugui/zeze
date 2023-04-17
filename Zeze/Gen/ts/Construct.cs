using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.ts
{
    public class Construct : Types.Visitor
    {
		private System.IO.StreamWriter sw;
		private Types.Variable variable;
		private string prefix;

		public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
		{
			sw.WriteLine(prefix + "public constructor() {");
            foreach (Types.Variable var in bean.Variables)
            {
                var.VariableType.Accept(new Construct(sw, var, prefix + "    "));
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Construct(System.IO.StreamWriter sw, Types.Variable variable, string prefix)
		{
			this.sw = sw;
			this.variable = variable;
			this.prefix = prefix;
		}

		private void Initial()
		{
            string value = variable.Initial;
			if (value.Length > 0)
			{
                string varname = variable.Name;
				sw.WriteLine(prefix + "this." + varname + " = " + value + ";");
			}
		}

        public void Visit(Bean type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "this." + variable.Name + " = new " + typeName + "();");
        }

        public void Visit(BeanKey type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "this." + variable.Name + " = new " + typeName + "();");
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
            long init = variable.Initial.Length > 0 ? long.Parse(variable.Initial) : 0;
            sw.WriteLine(prefix + "this." + variable.Name + " = " + init + "n;");
        }

        public void Visit(TypeBool type)
        {
            Initial();
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = new Uint8Array(0);");
        }

        public void Visit(TypeString type)
        {
            string value = variable.Initial;
            string varname = variable.Name;
            sw.WriteLine(prefix + "this." + varname + " = \"" + value + "\";");
        }

        public void Visit(TypeList type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "this." + variable.Name + " = new " + typeName + "();");
        }

        public void Visit(TypeSet type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "this." + variable.Name + " = new " + typeName + "();");
        }

        public void Visit(TypeMap type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "this." + variable.Name + " = new " + typeName + "();");
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
            var bean = variable.Bean as Bean;
            sw.WriteLine(prefix + "this." + variable.Name + " = new Zeze.DynamicBean("
                + $"{bean.Space.Path("_", bean.Name)}.GetSpecialTypeIdFromBean_{variable.Id}, "
                + $"{bean.Space.Path("_", bean.Name)}.CreateBeanFromSpecialTypeId_{variable.Id}"
                + ");");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = new Zeze.Vector2(0, 0);");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = new Zeze.Vector2(0, 0);");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = new Zeze.Vector3(0, 0, 0);");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = new Zeze.Vector3(0, 0, 0);");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = new Zeze.Vector4(0, 0, 0, 0);");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = new Zeze.Vector4(0, 0, 0, 0);");
        }
    }
}
