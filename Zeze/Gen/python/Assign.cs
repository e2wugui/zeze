using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.python
{
    public class Assign : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.WriteLine($"{prefix}def assign(self, other):");
            foreach (var var in bean.Variables)
                var.VariableType.Accept(new Assign(var, sw, prefix + "    "));
            if (bean.Variables.Count <= 0)
                sw.WriteLine($"{prefix}    pass");
        }

        public Assign(Variable var, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
        }

        public void Visit(TypeBool type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = other.{var.Name}");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = other.{var.Name}");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = other.{var.Name}");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = other.{var.Name}");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = other.{var.Name}");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = other.{var.Name}");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = other.{var.Name}");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = other.{var.Name}");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = other.{var.Name}");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(type.ValueType.IsNormalBean
                ? $"{prefix}assign_copy_list(self.{var.Name}, other.{var.Name})"
                : $"{prefix}assign_list(self.{var.Name}, other.{var.Name})");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(type.ValueType.IsNormalBean
                ? $"{prefix}assign_copy_set(self.{var.Name}, other.{var.Name})"
                : $"{prefix}assign_set(self.{var.Name}, other.{var.Name})");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(type.ValueType.IsNormalBean
                ? $"{prefix}assign_copy_dict(self.{var.Name}, other.{var.Name})"
                : $"{prefix}assign_dict(self.{var.Name}, other.{var.Name})");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.assign(other.{var.Name})");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = other.{var.Name}");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = assign_dynamic_bean(self.{var.Name}, other.{var.Name})");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.assign(other.{var.Name})");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.assign(other.{var.Name})");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.assign(other.{var.Name})");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.assign(other.{var.Name})");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.assign(other.{var.Name})");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine($"{prefix}self.{var.Name}.assign(other.{var.Name})");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine($"{prefix}self.{var.Name} = other.{var.Name}");
        }
    }
}
