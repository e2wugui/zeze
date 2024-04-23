using System.IO;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.python
{
    public class Construct : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable variable;

        private static string GetTypeDesc(Variable var)
        {
            var sb = new StringBuilder(var.Type);
            if (!string.IsNullOrEmpty(var.Value))
            {
                sb.Append('<');
                if (!string.IsNullOrEmpty(var.Key))
                    sb.Append(var.Key).Append(',');
                sb.Append(var.Value).Append('>');
            }
            return sb.ToString();
        }

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.Write(prefix + "def __init__(self");
            foreach (var var in bean.Variables)
                sw.Write($", {var.Name}=None");
            sw.WriteLine("):");
            foreach (var var in bean.Variables)
            {
                sw.Write($"{prefix}    self.{var.Name} = ");
                var.VariableType.Accept(new Construct(sw, var));
                sw.WriteLine($" if {var.Name} is None else {var.Name}  # {var.Id}:{GetTypeDesc(var)}");
            }
            if (bean.Variables.Count <= 0)
                sw.WriteLine($"{prefix}    pass");
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.Write(prefix + "def __init__(self");
            foreach (var var in bean.Variables)
                sw.Write($", {var.Name}=None");
            sw.WriteLine("):");
            foreach (var var in bean.Variables)
            {
                sw.Write($"{prefix}    self.{var.Name} = ");
                var.VariableType.Accept(new Construct(sw, var));
                sw.WriteLine($" if {var.Name} is None else {var.Name}  # {var.Id}:{GetTypeDesc(var)}");
            }
            if (bean.Variables.Count <= 0)
                sw.WriteLine($"{prefix}    pass");
        }

        public Construct(StreamWriter sw, Variable variable)
        {
            this.sw = sw;
            this.variable = variable;
        }

        void Initial(string def)
        {
            string value = variable.Initial;
            sw.Write(value.Length > 0 ? value : def);
        }

        public void Visit(TypeBool type)
        {
            sw.Write(variable.Initial.ToLower().Equals("true") ? "True" : "False");
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
            Initial("0.0");
        }

        public void Visit(TypeDouble type)
        {
            Initial("0.0");
        }

        public void Visit(TypeBinary type)
        {
            sw.Write("ByteBuffer.empty_bytes");
        }

        public void Visit(TypeString type)
        {
            sw.Write($"\"{variable.Initial}\"");
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

        public void Visit(TypeDecimal type)
        {
            sw.Write($"\"{variable.Initial}\"");
        }
    }
}
