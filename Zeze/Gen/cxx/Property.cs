using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cxx
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
            foreach (Bean real in type.RealBeans.Values)
            {
                string pname = "Get" + var.NameUpper1 + "_" + real.Space.Path("_", real.Name) + "()";
                sw.WriteLine(prefix + real.FullCxxName + "* " + pname + " {");
                sw.WriteLine(prefix + "    return (" + real.FullCxxName + "*)" + var.NameUpper1 + ".GetBean();");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                sw.WriteLine(prefix + $"void {Program.Upper1(var.NameSetter)}({real.FullCxxName}* value) {{");
                sw.WriteLine(prefix + "    " + var.NameUpper1 + ".SetBean(value);");
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
