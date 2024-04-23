using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.python
{
    public class HashCode : Visitor
    {
        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.WriteLine(prefix + "def __hash__(self):");
            if (bean.VariablesIdOrder.Count > 0)
            {
                sw.WriteLine(prefix + "    _h_ = 0");
                foreach (var var in bean.VariablesIdOrder)
                {
                    var e = new HashCode(var);
                    var.VariableType.Accept(e);
                    sw.WriteLine(prefix + "    _h_ = _h_ * 31 + " + e.text);
                }
                sw.WriteLine(prefix + "    return _h_");
            }
            else
                sw.WriteLine(prefix + "    return 0");
        }

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.WriteLine(prefix + "def __hash__(self):");
            if (bean.VariablesIdOrder.Count > 0)
            {
                sw.WriteLine(prefix + "    _h_ = 0");
                foreach (var var in bean.VariablesIdOrder)
                {
                    var e = new HashCode(var);
                    var.VariableType.Accept(e);
                    sw.WriteLine(prefix + "    _h_ = _h_ * 31 + " + e.text);
                }
                sw.WriteLine(prefix + "    return _h_");
            }
            else
                sw.WriteLine(prefix + "    return 0");
        }

        readonly Variable var;
        string text;

        public HashCode(Variable var)
        {
            this.var = var;
        }

        public void Visit(TypeBool type)
        {
            text = $"int(self.{var.Name})";
        }

        public void Visit(TypeByte type)
        {
            text = $"self.{var.Name}";
        }

        public void Visit(TypeShort type)
        {
            text = $"self.{var.Name}";
        }

        public void Visit(TypeInt type)
        {
            text = $"self.{var.Name}";
        }

        public void Visit(TypeLong type)
        {
            text = $"self.{var.Name}";
        }

        public void Visit(TypeFloat type)
        {
            text = $"self.{var.Name}.__hash__()";
        }

        public void Visit(TypeDouble type)
        {
            text = $"self.{var.Name}.__hash__()";
        }

        public void Visit(TypeBinary type)
        {
            text = $"self.{var.Name}.__hash__()";
        }

        public void Visit(TypeString type)
        {
            text = $"self.{var.Name}.__hash__()";
        }

        public void Visit(TypeDecimal type)
        {
            text = $"self.{var.Name}.__hash__()";
        }

        public void Visit(TypeList type)
        {
            text = $"hash_list(self.{var.Name})";
        }

        public void Visit(TypeSet type)
        {
            text = $"hash_list(self.{var.Name})";
        }

        public void Visit(TypeMap type)
        {
            text = $"hash_dict(self.{var.Name})";
        }

        public void Visit(Bean type)
        {
            text = $"self.{var.Name}.__hash__()";
        }

        public void Visit(BeanKey type)
        {
            text = $"self.{var.Name}.__hash__()";
        }

        public void Visit(TypeDynamic type)
        {
            text = $"self.{var.Name}.__hash__()";
        }

        public void Visit(TypeQuaternion type)
        {
            text = $"self.{var.Name}.__hash__()";
        }

        public void Visit(TypeVector2 type)
        {
            text = $"self.{var.Name}.__hash__()";
        }

        public void Visit(TypeVector2Int type)
        {
            text = $"self.{var.Name}.__hash__()";
        }

        public void Visit(TypeVector3 type)
        {
            text = $"self.{var.Name}.__hash__()";
        }

        public void Visit(TypeVector3Int type)
        {
            text = $"self.{var.Name}.__hash__()";
        }

        public void Visit(TypeVector4 type)
        {
            text = $"self.{var.Name}.__hash__()";
        }
    }
}
