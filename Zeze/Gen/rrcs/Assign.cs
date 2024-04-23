using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.rrcs
{
    public class Assign : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;

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

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + ".Assign(other." + var.NameUpper1 + ");");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        public void Visit(TypeBool type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + ".Clear();");
            string copyif = type.ValueType.IsNormalBean ? "e.Copy()" : "e";

            sw.WriteLine(prefix + "foreach (var e in other." + var.NameUpper1 +")");
            sw.WriteLine(prefix + "    " + var.NameUpper1 + ".Add(" + copyif + ");");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + ".Clear();");
            string copyif = type.ValueType.IsNormalBean ? "e.Copy()" : "e"; // set 里面现在不让放 bean，先这样写吧。

            sw.WriteLine(prefix + "foreach (var e in other." + var.NameUpper1 + ")");
            sw.WriteLine(prefix + "    " + var.NameUpper1 + ".Add(" + copyif + ");");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + ".Clear();");
            string copyif = type.ValueType.IsNormalBean ? "e.Value.Copy()" : "e.Value";

            sw.WriteLine(prefix + "foreach (var e in other." + var.NameUpper1 + ")");
            sw.WriteLine(prefix + "    " + var.NameUpper1 + ".Add(e.Key, " + copyif + ");");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + " = other." + var.NameUpper1 + ";");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + ".Assign(other." + var.NameUpper1 + ");");
        }

        public void Visit(TypeQuaternion type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector2 type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector2Int type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector3 type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector3Int type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector4 type)
        {
            throw new System.NotImplementedException();
        }
    }
}
