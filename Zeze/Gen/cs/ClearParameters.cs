using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
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
            sw.WriteLine();
        }

        public Variable variable { get; set; }
        public StreamWriter sw { get; set; }
        public string prefix { get; set; }

        public ClearParameters(Variable var , StreamWriter sw, string prefix)
        {
            this.variable = var;
            this.sw = sw;
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

        public void Visit(TypeBool type)
        {
            Initial();
        }

        public void Visit(TypeByte type)
        {
            Initial();
        }

        public void Visit(TypeShort type)
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

        public void Visit(TypeFloat type)
        {
            Initial();
        }

        public void Visit(TypeDouble type)
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
            sw.WriteLine($"{prefix}{variable.NameUpper1}.ClearParameters();");
        }

        public void Visit(BeanKey type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NameUpper1 + " = new " + typeName + "();");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine($"{prefix}{variable.NameUpper1}.ClearParameters();");
        }

        public void Visit(TypeQuaternion type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NameUpper1 + " = new " + typeName + "();");
        }

        public void Visit(TypeVector2 type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NameUpper1 + " = new " + typeName + "();");
        }

        public void Visit(TypeVector2Int type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NameUpper1 + " = new " + typeName + "();");
        }

        public void Visit(TypeVector3 type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NameUpper1 + " = new " + typeName + "();");
        }

        public void Visit(TypeVector3Int type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NameUpper1 + " = new " + typeName + "();");
        }

        public void Visit(TypeVector4 type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NameUpper1 + " = new " + typeName + "();");
        }
    }
}
