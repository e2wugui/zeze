using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
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
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public boolean equals(Object _obj1_) {");
            sw.WriteLine(prefix + "    if (_obj1_ == this) return true;");
            sw.WriteLine(prefix + "    if (_obj1_ instanceof " + bean.Name + ") {");
            sw.WriteLine(prefix + $"        var _obj_ = ({bean.Name})_obj1_;");
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
            text = (isEquals ? "" : "!") + var.Getter + ".equals(" + another + "." + var.Getter + ")";
        }

        void Visitor.Visit(BeanKey type)
        {
            text = (isEquals ? "" : "!") + var.Getter + ".equals(" + another + "." + var.Getter + ")";
        }

        void EqualsOnCompare(string typeWrapperName)
        {
            var eq = (isEquals ? " == " : " != ");
            text = $"{typeWrapperName}.compare({var.Getter}, {another}.{var.Getter}){eq}0";
        }

        void Visitor.Visit(TypeByte type)
        {
            EqualsOnCompare("Byte");
        }

        void Visitor.Visit(TypeDouble type)
        {
            EqualsOnCompare("Double");
        }

        void Visitor.Visit(TypeInt type)
        {
            EqualsOnCompare("Integer");
        }

        void Visitor.Visit(TypeLong type)
        {
            EqualsOnCompare("Long");
        }

        void Visitor.Visit(TypeBool type)
        {
            EqualsOnCompare("Boolean");
        }

        void Visitor.Visit(TypeBinary type)
        {
            throw new NotImplementedException();
        }

        void Visitor.Visit(TypeString type)
        {
            text = (isEquals ? "" : "!") + var.Getter + ".equals(" + another + "." + var.Getter + ")";
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
            EqualsOnCompare("Float");
        }

        void Visitor.Visit(TypeShort type)
        {
            EqualsOnCompare("Short");
        }

        void Visitor.Visit(TypeDynamic type)
        {
            throw new NotImplementedException();
        }
    }
}
