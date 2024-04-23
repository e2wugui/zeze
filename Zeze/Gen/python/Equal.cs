using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.python
{
    public class Equal : Visitor
    {
        readonly Variable var;
        readonly string another;
        readonly bool isEquals;
        string text;

        /// <summary>
        /// 实际上 BeanKey 很多类型都不支持，下面先尽量实现，以后可能用来实现 Bean 的 Equals.
        /// </summary>
        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.WriteLine($"{prefix}def __eq__(self, _o_):");
            if (bean.Variables.Count > 0)
            {
                sw.WriteLine($"{prefix}    if _o_ is self:");
                sw.WriteLine($"{prefix}        return True");
                sw.WriteLine($"{prefix}    if _o_.__class__ != self.__class__:");
                sw.WriteLine($"{prefix}        return False");
                foreach (var var in bean.Variables)
                {
                    var v = new Equal(var, "_o_", false);
                    var.VariableType.Accept(v);
                    sw.WriteLine($"{prefix}    if {v.text}:");
                    sw.WriteLine($"{prefix}        return False");
                }

                sw.WriteLine($"{prefix}    return True");
            }
            else
                sw.WriteLine($"{prefix}    return _o_.__class__ == self.__class__");
        }

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.WriteLine($"{prefix}def __eq__(self, _o_):");
            if (bean.Variables.Count > 0)
            {
                sw.WriteLine($"{prefix}    if _o_ is self:");
                sw.WriteLine($"{prefix}        return True");
                sw.WriteLine($"{prefix}    if _o_.__class__ != self.__class__:");
                sw.WriteLine($"{prefix}        return False");
                foreach (var var in bean.Variables)
                {
                    var v = new Equal(var, "_o_", false);
                    var.VariableType.Accept(v);
                    sw.WriteLine($"{prefix}    if {v.text}:");
                    sw.WriteLine($"{prefix}        return False");
                }

                sw.WriteLine($"{prefix}    return True");
            }
            else
                sw.WriteLine($"{prefix}    return _o_.__class__ == self.__class__");
        }

        public Equal(Variable var, string another, bool isEquals)
        {
            this.var = var;
            this.another = another;
            this.isEquals = isEquals;
        }

        void CommonEquals()
        {
            var eq = isEquals ? "==" : "!=";
            text = $"self.{var.Name} {eq} {another}.{var.Name}";
        }

        public void Visit(TypeBool type)
        {
            CommonEquals();
        }

        public void Visit(TypeByte type)
        {
            CommonEquals();
        }

        public void Visit(TypeShort type)
        {
            CommonEquals();
        }

        public void Visit(TypeInt type)
        {
            CommonEquals();
        }

        public void Visit(TypeLong type)
        {
            CommonEquals();
        }

        public void Visit(TypeFloat type)
        {
            CommonEquals();
        }

        public void Visit(TypeDouble type)
        {
            CommonEquals();
        }

        public void Visit(TypeBinary type)
        {
            CommonEquals();
        }

        public void Visit(TypeString type)
        {
            CommonEquals();
        }

        public void Visit(TypeDecimal type)
        {
            CommonEquals();
        }

        public void Visit(TypeList type)
        {
            CommonEquals();
        }

        public void Visit(TypeSet type)
        {
            CommonEquals();
        }

        public void Visit(TypeMap type)
        {
            CommonEquals();
        }

        public void Visit(Bean type)
        {
            CommonEquals();
        }

        public void Visit(BeanKey type)
        {
            CommonEquals();
        }

        public void Visit(TypeDynamic type)
        {
            CommonEquals();
        }

        public void Visit(TypeVector2 type)
        {
            CommonEquals();
        }

        public void Visit(TypeVector2Int type)
        {
            CommonEquals();
        }

        public void Visit(TypeVector3 type)
        {
            CommonEquals();
        }

        public void Visit(TypeVector3Int type)
        {
            CommonEquals();
        }

        public void Visit(TypeVector4 type)
        {
            CommonEquals();
        }

        public void Visit(TypeQuaternion type)
        {
            CommonEquals();
        }
    }
}
