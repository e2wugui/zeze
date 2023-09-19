using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.python
{
    public class Construct : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable variable;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.Write(prefix + "def __init__(self");
            foreach (var var in bean.Variables)
                sw.Write($", {var.Name} = None");
            sw.WriteLine("):");
            foreach (var var in bean.Variables)
            {
                sw.Write($"{prefix}    self.{var.Name} = ");
                var.VariableType.Accept(new Construct(sw, var));
                sw.WriteLine($" if {var.Name} is None else {var.Name}");
            }
            sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "# for decode only");
            sw.WriteLine(prefix + "def __init__(self):");
            foreach (var var in bean.Variables)
            {
                sw.Write($"{prefix}    self.{var.Name} = ");
                var.VariableType.Accept(new Construct(sw, var));
            }
            sw.WriteLine();
        }

        public Construct(StreamWriter sw, Variable variable)
        {
            this.sw = sw;
            this.variable = variable;
        }

        public void Visit(TypeBool type)
        {
            sw.Write("False");
        }

        public void Visit(TypeByte type)
        {
            sw.Write("0");
        }

        public void Visit(TypeShort type)
        {
            sw.Write("0");
        }

        public void Visit(TypeInt type)
        {
            sw.Write("0");
        }

        public void Visit(TypeLong type)
        {
            sw.Write("0");
        }

        public void Visit(TypeFloat type)
        {
            sw.Write("0.0");
        }

        public void Visit(TypeDouble type)
        {
            sw.Write("0.0");
        }

        public void Visit(TypeBinary type)
        {
            sw.Write("Buffer.empty_bytes");
        }

        public void Visit(TypeString type)
        {
            sw.Write("\"\"");
        }

        public void Visit(TypeList type)
        {
            sw.Write("[]");
        }

        public void Visit(TypeSet type)
        {
            sw.Write("set()");
        }

        public void Visit(TypeMap type)
        {
            sw.Write("{}");
        }

        public void Visit(Bean type)
        {
            sw.Write($"{TypeName.GetName(type)}({variable.Initial})");
        }

        public void Visit(BeanKey type)
        {
            sw.Write($"{TypeName.GetName(type)}({variable.Initial})");
        }

        public void Visit(TypeDynamic type)
        {
            sw.Write("EmptyBean()");
        }

        public void Visit(TypeVector2 type)
        {
            sw.Write($"Vector2({variable.Initial})");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.Write($"Vector2Int({variable.Initial})");
        }

        public void Visit(TypeVector3 type)
        {
            sw.Write($"Vector3({variable.Initial})");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.Write($"Vector3Int({variable.Initial})");
        }

        public void Visit(TypeVector4 type)
        {
            sw.Write($"Vector4({variable.Initial})");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.Write($"Quaternion({variable.Initial})");
        }
    }
}
