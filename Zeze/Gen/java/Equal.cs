using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
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
        /// <param name="bean"></param>
        /// <param name="sw"></param>
        /// <param name="prefix"></param>
        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public boolean equals(Object _obj1_) {");
            sw.WriteLine(prefix + "    if (_obj1_ == this)");
            sw.WriteLine(prefix + "        return true;");
            sw.WriteLine(prefix + "    if (_obj1_ instanceof " + bean.Name + ") {");
            sw.WriteLine(prefix + $"        var _obj_ = ({bean.Name})_obj1_;");
            foreach (Variable var in bean.Variables)
            {
                var v = new Equal(var, "_obj_", false);
                var.VariableType.Accept(v);
                sw.WriteLine(prefix + "        if (" + v.text + ")");
                sw.WriteLine(prefix + "            return false;");
            }
            sw.WriteLine(prefix + "        return true;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    return false;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
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
            text = $"{var.Getter} {eq} {another}.{var.Getter}";
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
            text = (isEquals ? "" : "!") + var.Getter + ".equals(" + another + "." + var.Getter + ")";
        }

        public void Visit(TypeString type)
        {
            text = (isEquals ? "" : "!") + var.Getter + ".equals(" + another + "." + var.Getter + ")";
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

        public void Visit(Bean type)
        {
            text = (isEquals ? "" : "!") + var.Getter + ".equals(" + another + "." + var.Getter + ")";
        }

        public void Visit(BeanKey type)
        {
            text = (isEquals ? "" : "!") + var.Getter + ".equals(" + another + "." + var.Getter + ")";
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
            throw new NotImplementedException();
        }

        public void Visit(TypeVector3 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector3Int type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector4 type)
        {
            throw new NotImplementedException();
        }
    }
}
