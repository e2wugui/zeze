using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.ts
{
    public class Construct : Visitor
    {
		private StreamWriter sw;
        private readonly Bean bean;
		private readonly Variable variable;
		private readonly string prefix;

		public static void Make(Bean bean, StreamWriter sw, string prefix)
		{
			sw.WriteLine(prefix + "public constructor() {");
            foreach (var var in bean.Variables)
            {
                var.VariableType.Accept(new Construct(sw, bean, var, prefix + "    "));
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Construct(StreamWriter sw, Bean bean, Variable variable, string prefix)
		{
			this.sw = sw;
            this.bean = bean;
			this.variable = variable;
			this.prefix = prefix;
        }

		private void Initial()
		{
            string value = variable.Initial;
            if (value.Length == 0)
                value = "0";
            else if (!int.TryParse(value, out int v))
                value = bean.Space.Path("_", bean.Name) + '.' + value;
			sw.WriteLine(prefix + "this." + variable.Name + " = " + value + ";");
		}

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = new " + TypeName.GetName(type) + "(" + variable.Initial + ");");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = new " + TypeName.GetName(type) + "(" + variable.Initial + ");");
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
            string value = variable.Initial;
            if (value.Length == 0)
                value = "0n";
            else if (long.TryParse(value, out long v))
                value = v + "n";
            else
                value = bean.Space.Path("_", bean.Name) + '.' + value;
            sw.WriteLine(prefix + "this." + variable.Name + " = " + value + ";");
        }

        public void Visit(TypeBool type)
        {
            string value = variable.Initial;
            if (value.Length == 0)
                value = "false";
            sw.WriteLine(prefix + "this." + variable.Name + " = " + value + ";");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = new Uint8Array(0);");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = \"" + variable.Initial + "\";");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = \"" + variable.Initial + "\";");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = [];");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = new " + TypeName.GetName(type) + "();");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + "this." + variable.Name + " = new " + TypeName.GetName(type) + "();");
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
            var bean = (Bean)variable.Bean;
            sw.WriteLine(prefix + "this." + variable.Name + " = new Zeze.DynamicBean("
                + $"{bean.Space.Path("_", bean.Name)}.GetSpecialTypeIdFromBean_{variable.Id}, "
                + $"{bean.Space.Path("_", bean.Name)}.CreateBeanFromSpecialTypeId_{variable.Id}"
                + ");");
        }

        public void InitialVector(Type type, string def)
        {
            string value = variable.Initial;
            value = value.Length > 0 ? value : def;
            sw.WriteLine($"{prefix}this.{variable.Name} = new {TypeName.GetName(type)}({value});");
        }

        public void Visit(TypeVector2 type)
        {
            InitialVector(type, "0, 0");
        }

        public void Visit(TypeVector2Int type)
        {
            InitialVector(type, "0, 0");
        }

        public void Visit(TypeVector3 type)
        {
            InitialVector(type, "0, 0, 0");
        }

        public void Visit(TypeVector3Int type)
        {
            InitialVector(type, "0, 0, 0");
        }

        public void Visit(TypeVector4 type)
        {
            InitialVector(type, "0, 0, 0, 0");
        }

        public void Visit(TypeQuaternion type)
        {
            InitialVector(type, "0, 0, 0, 0");
        }
    }
}
