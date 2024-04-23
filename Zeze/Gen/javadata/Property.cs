using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.javadata
{
    public class Property : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            foreach (Variable var in bean.Variables)
            {
                var.VariableType.Accept(new Property(sw, var, prefix));
            }
        }

        public Property(StreamWriter sw, Variable var, string prefix)
        {
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
        }

        void WriteProperty(Type type, bool checkNull = false)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public void " + var.Setter($"{typeName} value") + " {");
            if (checkNull)
            {
                sw.WriteLine(prefix + "    if (value == null)");
                sw.WriteLine(prefix + "        throw new IllegalArgumentException();");
            }
            sw.WriteLine(prefix + "    " + var.NamePrivate + " = value;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(TypeBool type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeByte type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeShort type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeInt type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeLong type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeFloat type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeDouble type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeBinary type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeString type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeList type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeSet type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeMap type)
        {
            WriteProperty(type, true);
        }

        public void Visit(Bean type)
        {
            WriteProperty(type, true);
        }

        public void Visit(BeanKey type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeDynamic type)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine($"{prefix}public {typeName} {var.Getter} {{");
            sw.WriteLine($"{prefix}    return {var.NamePrivate};");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            foreach (Bean real in type.RealBeans.Values)
            {
                string rname = TypeName.GetName(real);
                string pname = "get" + var.NameUpper1 + "_" + real.Space.Path("_", real.Name) + "()";
                sw.WriteLine(prefix + "public " + rname + " " + pname + " {");
                sw.WriteLine(prefix + "    return (" + rname + ")" + var.NamePrivate + ".getData();");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                sw.WriteLine(prefix + $"public void {var.Setter($"{rname} value")} {{");
                sw.WriteLine(prefix + "    " + var.NamePrivate + ".setData(value);");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                //sw.WriteLine(prefix + rname + "ReadOnly " + beanNameReadOnly + "." + pname + " => " + pname + ";");
                //sw.WriteLine();
            }
        }

        public void Visit(TypeQuaternion type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeVector2 type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeVector2Int type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeVector3 type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeVector3Int type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeVector4 type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeDecimal type)
        {
            WriteProperty(type, true);
        }
    }
}
