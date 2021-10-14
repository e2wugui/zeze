using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    // 现在这个类仅用于BeanKey，如果普通Bean要支持，
    // 需要使用NameUpper1进行比较，而不是NamePrivate。
    public class Equal : Types.Visitor
    {
        private Types.Variable var;
        private string another;
        private bool isEquals;
        private string text;

        /// <summary>
        /// 实际上 BeanKey 很多类型都不支持，下面先尽量实现，以后可能用来实现 Bean 的 Equals.
        /// </summary>
        /// <param name="bean"></param>
        /// <param name="sw"></param>
        /// <param name="prefix"></param>
        public static void Make(Types.BeanKey bean, System.IO.StreamWriter sw, String prefix)
        {
            sw.WriteLine(prefix + "public override bool Equals(object _obj1_)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    if (_obj1_ == this) return true;");
            sw.WriteLine(prefix + "    if (_obj1_ is " + bean.Name + " _obj_)");
            sw.WriteLine(prefix + "    {");
            foreach (Types.Variable var in bean.Variables)
            {
                var v = new Equal(var, "_obj_", false);
                var.VariableType.Accept(v);
                sw.WriteLine(prefix + "        if (" + v.text + ") return false;");
            }
            sw.WriteLine(prefix + "        return true;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    return false;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine("");
        }

        public Equal(Variable var, string another, bool isEquals)
        {
            this.var = var;
            this.another = another;
            this.isEquals = isEquals;
        }

        void Visitor.Visit(Bean type)
        {
            text = (isEquals ? "" : "!") + var.NamePrivate + ".Equals(" + another + "." + var.NamePrivate + ")";
        }

        void Visitor.Visit(BeanKey type)
        {
            text = (isEquals ? "" : "!") + var.NamePrivate + ".Equals(" + another + "." + var.NamePrivate + ")";
        }

        void Visitor.Visit(TypeByte type)
        {
            text = var.NamePrivate + (isEquals ? " == " : " != ") + another + "." + var.NamePrivate;
        }

        void Visitor.Visit(TypeDouble type)
        {
            text = var.NamePrivate + (isEquals ? " == " : " != ") + another + "." + var.NamePrivate;
        }

        void Visitor.Visit(TypeInt type)
        {
            text = var.NamePrivate + (isEquals ? " == " : " != ") + another + "." + var.NamePrivate;
        }

        void Visitor.Visit(TypeLong type)
        {
            text = var.NamePrivate + (isEquals ? " == " : " != ") + another + "." + var.NamePrivate;
        }

        void Visitor.Visit(TypeBool type)
        {
            text = var.NamePrivate + (isEquals ? " == " : " != ") + another + "." + var.NamePrivate;
        }

        void Visitor.Visit(TypeBinary type)
        {
            throw new NotImplementedException();
        }

        void Visitor.Visit(TypeString type)
        {
            text = (isEquals ? "" : "!") + var.NamePrivate + ".Equals(" + another + "." + var.NamePrivate + ")";
        }

        void Visitor.Visit(TypeList type)
        {
            throw new NotImplementedException();
        }

        void Visitor.Visit(TypeSet type)
        {
            throw new NotImplementedException();
        }

        void Visitor.Visit(TypeMap type)
        {
            throw new NotImplementedException();
        }

        void Visitor.Visit(TypeFloat type)
        {
            text = var.NamePrivate + (isEquals ? " == " : " != ") + another + "." + var.NamePrivate;
        }

        void Visitor.Visit(TypeShort type)
        {
            text = var.NamePrivate + (isEquals ? " == " : " != ") + another + "." + var.NamePrivate;
        }

        void Visitor.Visit(TypeDynamic type)
        {
            throw new NotImplementedException();
        }
    }
}
