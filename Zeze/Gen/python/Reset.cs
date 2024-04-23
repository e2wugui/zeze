using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.python
{
    public class Reset : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            if (bean.Variables.Count > 0)
            {
                sw.WriteLine();
                sw.WriteLine(prefix + "def reset(self):");
                foreach (var var in bean.Variables)
                    var.VariableType.Accept(new Reset(var, sw, prefix + "    "));
                if (bean.Variables.Count <= 0)
                    sw.WriteLine($"{prefix}    pass");
            }
        }

        public Reset(Variable var, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
        }

        public void Visit(TypeBool type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = False");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = 0");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = 0");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = 0");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = 0");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = 0.0");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = 0.0");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = ByteBuffer.empty_bytes");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = \"\"");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = \"\"");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.clear()");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.clear()");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.clear()");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.reset()");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.reset()");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = EmptyBean()");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.reset()");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.reset()");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.reset()");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.reset()");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.reset()");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.reset()");
        }
    }
}
