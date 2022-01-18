using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Assign : Visitor
    {
        private StreamWriter sw;
        private Variable var;
        private string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public void Assign(" + bean.Name + " other)");
            sw.WriteLine(prefix + "{");
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new Assign(var, sw, prefix + "    "));
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Assign(Variable var, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
        }

        void Visitor.Visit(Bean type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + ".Assign(other." + var.NameUpper1 + ");");
        }

        void Visitor.Visit(BeanKey type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        void Visitor.Visit(TypeByte type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        void Visitor.Visit(TypeDouble type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        void Visitor.Visit(TypeInt type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        void Visitor.Visit(TypeLong type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        void Visitor.Visit(TypeBool type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        void Visitor.Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        void Visitor.Visit(TypeString type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        void Visitor.Visit(TypeList type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + ".Clear();");
            string copyif = type.ValueType.IsNormalBean ? "e.Copy()" : "e";

            sw.WriteLine(prefix + "foreach (var e in other." + var.NameUpper1 +")");
            sw.WriteLine(prefix + "    " + var.NameUpper1 + ".Add(" + copyif + ");");
        }

        void Visitor.Visit(TypeSet type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + ".Clear();");
            string copyif = type.ValueType.IsNormalBean ? "e.Copy()" : "e"; // set 里面现在不让放 bean，先这样写吧。

            sw.WriteLine(prefix + "foreach (var e in other." + var.NameUpper1 + ")");
            sw.WriteLine(prefix + "    " + var.NameUpper1 + ".Add(" + copyif + ");");
        }

        void Visitor.Visit(TypeMap type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + ".Clear();");
            string copyif = type.ValueType.IsNormalBean ? "e.Value.Copy()" : "e.Value";

            sw.WriteLine(prefix + "foreach (var e in other." + var.NameUpper1 + ")");
            sw.WriteLine(prefix + "    " + var.NameUpper1 + ".Add(e.Key, " + copyif + ");");
        }

        void Visitor.Visit(TypeFloat type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        void Visitor.Visit(TypeShort type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        void Visitor.Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + ".Assign(other." + var.NameUpper1 + ");");
        }
    }
}
