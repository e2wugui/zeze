using System;
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
            sw.WriteLine(prefix + "    const int _prime_ = 31;");
            sw.WriteLine(prefix + "    int _h_ = 0;");
            foreach (Variable var in bean.Variables)
			{
				HashCode e = new HashCode(sw, prefix + "    ", var.NamePrivate);
				var.VariableType.Accept(e);
			}
			sw.WriteLine(prefix + "    return _h_;");
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
            sw.WriteLine($"{prefix}_h_ = _h_ * _prime_ + {varname}.GetHashCode();");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _prime_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _prime_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeDouble type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _prime_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _prime_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeBool type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _prime_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _prime_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _prime_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeList type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeSet type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeMap type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeFloat type)
        {
            throw new NotImplementedException();
            //text = "System.BitConverter.SingleToInt32Bits(" + varname + ").GetHashCode()";
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _prime_ + {varname}.GetHashCode();");
        }

        public void Visit(TypeDynamic type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeQuaternion type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector2 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _prime_ + {varname}.x.GetHashCode();");
            sw.WriteLine($"{prefix}_h_ = _h_ * _prime_ + {varname}.y.GetHashCode();");
        }

        public void Visit(TypeVector3 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine($"{prefix}_h_ = _h_ * _prime_ + {varname}.x.GetHashCode();");
            sw.WriteLine($"{prefix}_h_ = _h_ * _prime_ + {varname}.y.GetHashCode();");
            sw.WriteLine($"{prefix}_h_ = _h_ * _prime_ + {varname}.z.GetHashCode();");
        }

        public void Visit(TypeVector4 type)
        {
            throw new NotImplementedException();
        }
    }
}
