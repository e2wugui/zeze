using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cxx
{
    public class Assign : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;
        readonly bool transBean;

        public static void MakeHpp(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "virtual void Assign(const Zeze::Bean& other) override;");
            sw.WriteLine(prefix + "void Assign(const " + bean.Name + "& other);");
            sw.WriteLine(prefix + $"{bean.Name}& operator=(const {bean.Name}& other);");
        }

        public static void MakeCpp(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + $"void {bean.Name}::Assign(const Zeze::Bean& other) {{");
            sw.WriteLine(prefix + $"    Assign(dynamic_cast<const {bean.Name}&>(other));");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();

            sw.WriteLine(prefix + $"void {bean.Name}::Assign(const {bean.Name}& other) {{");
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new Assign(var, sw, prefix + "    ", true));
            sw.WriteLine(prefix + "}");
            sw.WriteLine();

            sw.WriteLine(prefix + $"{bean.Name}& {bean.Name}::operator=(const {bean.Name}& other) {{");
            sw.WriteLine(prefix + "    Assign(other);");
            sw.WriteLine(prefix + "    return *this;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "void Assign(const " + bean.Name + "& other) {");
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new Assign(var, sw, prefix + "    ", true));
            sw.WriteLine(prefix + "}");
            sw.WriteLine();

            sw.WriteLine(prefix + $"{bean.Name}& operator=(const {bean.Name}& other) {{");
            sw.WriteLine(prefix + "    Assign(other);");
            sw.WriteLine(prefix + "    return *this;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Assign(Variable var, StreamWriter sw, string prefix, bool transBean)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
            this.transBean = transBean;
        }

        public void Visit(TypeBool type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + ".Assign(other." + var.NameUpper1 + ");");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + ".Assign(other." + var.NameUpper1 + ");");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + var.NameUpper1 + ".Assign(other." + var.NameUpper1 + ");");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine(prefix + $"{var.NameUpper1} = other.{var.NameUpper1};");
        }
    }
}
