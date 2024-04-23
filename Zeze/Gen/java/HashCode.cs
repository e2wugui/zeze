using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class HashCode : Visitor
    {
		public static void Make(BeanKey bean, StreamWriter sw, string prefix, bool isData)
		{
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public int hashCode() {");
            if (bean.VariablesIdOrder.Count > 0)
            {
                sw.WriteLine(prefix + "    final int _p_ = 31;"); // prime number
                sw.WriteLine(prefix + "    int _h_ = 0;");
                foreach (Variable var in bean.VariablesIdOrder)
                {
                    HashCode e = new HashCode(var, isData);
                    var.VariableType.Accept(e);
                    sw.WriteLine(prefix + "    _h_ = _h_ * _p_ + " + e.text + ";");
                }
                sw.WriteLine(prefix + "    return _h_;");
            }
            else
                sw.WriteLine(prefix + "    return 0;");
            sw.WriteLine(prefix + "}");
			sw.WriteLine();
		}

        public static void Make(Bean bean, StreamWriter sw, string prefix, bool isData)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public int hashCode() {");
            if (bean.VariablesIdOrder.Count > 0)
            {
                sw.WriteLine(prefix + "    final int _p_ = 31;"); // prime number
                sw.WriteLine(prefix + "    int _h_ = 0;");
                foreach (Variable var in bean.VariablesIdOrder)
                {
                    if (bean.Version.Equals(var.Name))
                        continue;
                    HashCode e = new HashCode(var, isData);
                    var.VariableType.Accept(e);
                    sw.WriteLine(prefix + "    _h_ = _h_ * _p_ + " + e.text + ";");
                }
                sw.WriteLine(prefix + "    return _h_;");
            }
            else
                sw.WriteLine(prefix + "    return 0;");
            sw.WriteLine(prefix + "}");
            // sw.WriteLine();
        }

        readonly Variable var;
        string text;
        string getter;

        public HashCode(Variable var, bool isData)
        {
            this.var = var;
            getter = isData ? var.NamePrivate : var.Getter;
        }

        public void Visit(TypeBool type)
        {
            text = $"Boolean.hashCode({getter})";
        }

        public void Visit(TypeByte type)
        {
            text = $"Byte.hashCode({getter})";
        }

        public void Visit(TypeShort type)
        {
            text = $"Short.hashCode({getter})";
        }

        public void Visit(TypeInt type)
        {
            text = $"Integer.hashCode({getter})";
        }

        public void Visit(TypeLong type)
        {
            text = $"Long.hashCode({getter})";
        }

        public void Visit(TypeFloat type)
        {
            text = $"Float.hashCode({getter})";
        }

        public void Visit(TypeDouble type)
        {
            text = $"Double.hashCode({getter})";
        }

        public void Visit(TypeBinary type)
        {
            text = getter + ".hashCode()";
        }

        public void Visit(TypeString type)
        {
            text = getter + ".hashCode()";
        }

        public void Visit(TypeList type)
        {
            text = var.NamePrivate + ".hashCode()";
        }

        public void Visit(TypeSet type)
        {
            text = var.NamePrivate + ".hashCode()";
        }

        public void Visit(TypeMap type)
        {
            text = var.NamePrivate + ".hashCode()";
        }

        public void Visit(Bean type)
        {
            text = var.NamePrivate + ".hashCode()";
        }

        public void Visit(BeanKey type)
        {
            text = getter + ".hashCode()";
        }

        public void Visit(TypeDynamic type)
        {
            text = var.NamePrivate + ".hashCode()";
        }

        public void Visit(TypeQuaternion type)
        {
            text = getter + ".hashCode()";
        }

        public void Visit(TypeVector2 type)
        {
            text = getter + ".hashCode()";
        }

        public void Visit(TypeVector2Int type)
        {
            text = getter + ".hashCode()";
        }

        public void Visit(TypeVector3 type)
        {
            text = getter + ".hashCode()";
        }

        public void Visit(TypeVector3Int type)
        {
            text = getter + ".hashCode()";
        }

        public void Visit(TypeVector4 type)
        {
            text = getter + ".hashCode()";
        }

        public void Visit(TypeDecimal type)
        {
            text = getter + ".hashCode()";
        }
    }
}
