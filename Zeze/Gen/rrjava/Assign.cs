using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.rrjava
{
    public class Assign : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public void assign(" + bean.Name + " other) {");
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
            sw.WriteLine(prefix + var.NamePrivate + ".assign(other." + var.NamePrivate + ");");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeBool type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
            string copyif = type.ValueType.IsNormalBean ? "e.copy()" : "e";

            sw.WriteLine(prefix + "for (var e : other." + var.NamePrivate + ")");
            sw.WriteLine(prefix + "    " + var.NamePrivate + ".add(" + copyif + ");");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
            string copyif = type.ValueType.IsNormalBean ? "e.copy()" : "e"; // set 里面现在不让放 bean，先这样写吧。

            sw.WriteLine(prefix + "for (var e : other." + var.NamePrivate + ")");
            sw.WriteLine(prefix + "    " + var.NamePrivate + ".add(" + copyif + ");");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
            string copyif = type.ValueType.IsNormalBean ? "e.getValue().copy()" : "e.getValue()";

            sw.WriteLine(prefix + "for (var e : other." + var.NamePrivate + ".entrySet())");
            sw.WriteLine(prefix + "    " + var.NamePrivate + ".put(e.getKey(), " + copyif + ");");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".assign(other." + var.Getter + ");");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }
    }
}
