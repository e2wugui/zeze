using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class ClearParameters : Visitor
    {
        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public override void ClearParameters()");
            sw.WriteLine(prefix + "{");
            foreach (Variable var in bean.VariablesIdOrder)
            {
                ClearParameters e = new ClearParameters(var, sw, prefix + "    ");
                var.VariableType.Accept(e);
            }
            sw.WriteLine(prefix + "}");
            // sw.WriteLine();
        }

        public Variable variable { get; set; }
        public StreamWriter sw { get; set; }
        public string prefix { get; set; }

        public ClearParameters(Variable var, StreamWriter sw, string prefix)
        {
            this.variable = var;
            this.sw = sw;
            this.prefix = prefix;
        }

        void Initial(string def)
        {
            string value = variable.Initial;
            value = value.Length > 0 ? value : def;
            sw.WriteLine(prefix + variable.NameUpper1 + " = " + value + ";");
        }

        public void Visit(TypeBool type)
        {
            Initial("false");
        }

        public void Visit(TypeByte type)
        {
            Initial("0");
        }

        public void Visit(TypeShort type)
        {
            Initial("0");
        }

        public void Visit(TypeInt type)
        {
            Initial("0");
        }

        public void Visit(TypeLong type)
        {
            Initial("0");
        }

        public void Visit(TypeFloat type)
        {
            Initial("0");
        }

        public void Visit(TypeDouble type)
        {
            Initial("0");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + variable.NameUpper1 + " = Zeze.Net.Binary.Empty;");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine(prefix + variable.NameUpper1 + " = \"" + variable.Initial + "\";");
        }

        public void Visit(TypeList type)
        {
            if (type.FixSize >= 0 || type is TypeArray)
            {
                sw.WriteLine($"{prefix}if ({variable.NameUpper1} != null)");
                sw.WriteLine($"{prefix}    System.Array.Clear({variable.NameUpper1}, 0, {variable.NameUpper1}.Length);");
            }
            else
                sw.WriteLine($"{prefix}{variable.NameUpper1}.Clear();");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine($"{prefix}{variable.NameUpper1}.Clear();");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine($"{prefix}{variable.NameUpper1}.Clear();");
        }

        public void Visit(Bean type)
        {
            var value = variable.Initial;
            if (value.Length > 0)
                sw.WriteLine($"{prefix}{variable.NameUpper1} = new {TypeName.GetName(type)}({value});");
            else
                sw.WriteLine($"{prefix}{variable.NameUpper1}.ClearParameters();");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine($"{prefix}{variable.NameUpper1} = new {TypeName.GetName(type)}({variable.Initial});");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine($"{prefix}{variable.NameUpper1}.ClearParameters();");
        }

        public void InitialVector(string def)
        {
            var value = variable.Initial;
            value = value.Length > 0 ? value : def;
            sw.WriteLine($"{prefix}{variable.NameUpper1}.Set({value});");
        }

        public void Visit(TypeVector2 type)
        {
            InitialVector("0, 0");
        }

        public void Visit(TypeVector2Int type)
        {
            InitialVector("0, 0");
        }

        public void Visit(TypeVector3 type)
        {
            InitialVector("0, 0, 0");
        }

        public void Visit(TypeVector3Int type)
        {
            InitialVector("0, 0, 0");
        }

        public void Visit(TypeVector4 type)
        {
            InitialVector("0, 0, 0, 0");
        }

        public void Visit(TypeQuaternion type)
        {
            InitialVector("0, 0, 0, 0");
        }

        public void Visit(TypeDecimal type)
        {
            Initial("0");
        }
    }
}
