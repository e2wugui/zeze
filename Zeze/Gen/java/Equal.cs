using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Equal : Visitor
    {
        readonly Variable var;
        readonly string another;
        readonly bool isEquals;
        readonly string getter;
        string text;

        /// <summary>
        /// 实际上 BeanKey 很多类型都不支持，下面先尽量实现，以后可能用来实现 Bean 的 Equals.
        /// </summary>
        public static void Make(BeanKey bean, StreamWriter sw, string prefix, bool isData)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public boolean equals(Object _o_) {");
            if (bean.Variables.Count > 0)
            {
                sw.WriteLine(prefix + "    if (_o_ == this)");
                sw.WriteLine(prefix + "        return true;");
                sw.WriteLine(prefix + "    if (!(_o_ instanceof " + bean.Name + "))");
                sw.WriteLine(prefix + "        return false;");
                sw.WriteLine(prefix + "    //noinspection PatternVariableCanBeUsed");
                sw.WriteLine(prefix + $"    var _b_ = ({bean.Name})_o_;");
                foreach (Variable var in bean.Variables)
                {
                    var v = new Equal(var, "_b_", false, isData);
                    var.VariableType.Accept(v);
                    sw.WriteLine(prefix + "    if (" + v.text + ")");
                    sw.WriteLine(prefix + "        return false;");
                }

                sw.WriteLine(prefix + "    return true;");
            }
            else
                sw.WriteLine(prefix + "    return _o_ instanceof " + bean.Name + ';');

            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(Bean bean, StreamWriter sw, string prefix, bool isData)
        {
            var className = isData ? bean.Name + ".Data" : bean.Name;
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public boolean equals(Object _o_) {");
            if (bean.Variables.Count > 0)
            {
                sw.WriteLine(prefix + "    if (_o_ == this)");
                sw.WriteLine(prefix + "        return true;");
                sw.WriteLine(prefix + "    if (!(_o_ instanceof " + className + "))");
                sw.WriteLine(prefix + "        return false;");
                sw.WriteLine(prefix + "    //noinspection PatternVariableCanBeUsed");
                sw.WriteLine(prefix + $"    var _b_ = ({className})_o_;");
                foreach (Variable var in bean.Variables)
                {
                    if (bean.Version.Equals(var.Name))
                        continue;
                    var v = new Equal(var, "_b_", false, isData);
                    var.VariableType.Accept(v);
                    sw.WriteLine(prefix + "    if (" + v.text + ")");
                    sw.WriteLine(prefix + "        return false;");
                }

                sw.WriteLine(prefix + "    return true;");
            }
            else
                sw.WriteLine(prefix + "    return _o_ instanceof " + className + ';');

            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Equal(Variable var, string another, bool isEquals, bool isData)
        {
            this.var = var;
            this.another = another;
            this.isEquals = isEquals;
            getter = isData ? var.NamePrivate : var.Getter;
        }

        void CommonEquals()
        {
            var eq = isEquals ? "==" : "!=";
            text = $"{getter} {eq} {another}.{getter}";
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
            text = (isEquals ? "" : "!") + getter + ".equals(" + another + "." + getter + ")";
        }

        public void Visit(TypeString type)
        {
            text = (isEquals ? "" : "!") + getter + ".equals(" + another + "." + getter + ")";
        }

        public void Visit(TypeList type)
        {
            text = (isEquals ? "" : "!") + var.NamePrivate + ".equals(" + another + "." + var.NamePrivate + ")";
        }

        public void Visit(TypeSet type)
        {
            text = (isEquals ? "" : "!") + var.NamePrivate + ".equals(" + another + "." + var.NamePrivate + ")";
        }

        public void Visit(TypeMap type)
        {
            text = (isEquals ? "" : "!") + var.NamePrivate + ".equals(" + another + "." + var.NamePrivate + ")";
        }

        public void Visit(Bean type)
        {
            text = (isEquals ? "" : "!") + var.NamePrivate + ".equals(" + another + "." + var.NamePrivate + ")";
        }

        public void Visit(BeanKey type)
        {
            text = (isEquals ? "" : "!") + getter + ".equals(" + another + "." + getter + ")";
        }

        public void Visit(TypeDynamic type)
        {
            text = (isEquals ? "" : "!") + var.NamePrivate + ".equals(" + another + "." + var.NamePrivate + ")";
        }

        public void Visit(TypeQuaternion type)
        {
            text = (isEquals ? "" : "!") + getter + ".equals(" + another + "." + getter + ")";
        }

        public void Visit(TypeVector2 type)
        {
            text = (isEquals ? "" : "!") + getter + ".equals(" + another + "." + getter + ")";
        }

        public void Visit(TypeVector2Int type)
        {
            text = (isEquals ? "" : "!") + getter + ".equals(" + another + "." + getter + ")";
        }

        public void Visit(TypeVector3 type)
        {
            text = (isEquals ? "" : "!") + getter + ".equals(" + another + "." + getter + ")";
        }

        public void Visit(TypeVector3Int type)
        {
            text = (isEquals ? "" : "!") + getter + ".equals(" + another + "." + getter + ")";
        }

        public void Visit(TypeVector4 type)
        {
            text = (isEquals ? "" : "!") + getter + ".equals(" + another + "." + getter + ")";
        }

        public void Visit(TypeDecimal type)
        {
            text = (isEquals ? "" : "!") + getter + ".equals(" + another + "." + getter + ")";
        }
    }
}
