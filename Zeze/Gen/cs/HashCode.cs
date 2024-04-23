using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class HashCode : Visitor
    {
		public static void Make(BeanKey bean, StreamWriter sw, string prefix)
		{
			sw.WriteLine(prefix + "public override int GetHashCode()");
			sw.WriteLine(prefix + "{");
            if (bean.VariablesIdOrder.Count > 0)
            {
                sw.WriteLine(prefix + "    const int _p_ = 31;"); // prime number
                sw.WriteLine(prefix + "    int _h_ = 0;");
                foreach (Variable var in bean.VariablesIdOrder)
                {
                    HashCode e = new HashCode(sw, prefix + "    ", var.NamePrivate);
                    var.VariableType.Accept(e);
                }
                sw.WriteLine(prefix + "    return _h_;");
            }
            else
                sw.WriteLine(prefix + "    return 0;");
            sw.WriteLine(prefix + "}");
			sw.WriteLine();
		}

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public override int GetHashCode()");
            sw.WriteLine(prefix + "{");
            if (bean.VariablesIdOrder.Count > 0)
            {
                sw.WriteLine(prefix + "    const int _p_ = 31;"); // prime number
                sw.WriteLine(prefix + "    int _h_ = 0;");
                foreach (Variable var in bean.VariablesIdOrder)
                {
                    if (bean.Version.Equals(var.Name))
                        continue;
                    HashCode e = new HashCode(sw, prefix + "    ", var.NamePrivate);
                    var.VariableType.Accept(e);
                }
                sw.WriteLine(prefix + "    return _h_;");
            }
            else
                sw.WriteLine(prefix + "    return 0;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        readonly string prefix;
        readonly string varname;
        readonly StreamWriter sw;

        public HashCode(StreamWriter sw, string prefix, string varname)
        {
            this.sw = sw;
            this.prefix = prefix;
            this.varname = varname;
        }

        public void Visit(Bean type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeBool type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _p_ + {varname}.GetHashCode();");
        }
    }
}
