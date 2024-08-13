using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.javadata
{
    public class Assign : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;
        readonly bool transBean;
        readonly string transGetter;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            if (bean.OnlyData)
            {
                sw.WriteLine(prefix + $"public Zeze.Transaction.Bean toBean() {{");
                sw.WriteLine(prefix + "    throw new UnsupportedOperationException();");
            }
            else
            {
                sw.WriteLine(prefix + $"public {bean.FullName} toBean() {{");
                sw.WriteLine(prefix + $"    var _b_ = new {bean.FullName}();");
                sw.WriteLine(prefix + $"    _b_.assign(this);");
                sw.WriteLine(prefix + $"    return _b_;");
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();

            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void assign(Zeze.Transaction.Bean _o_) {");
            if (bean.OnlyData)
                sw.WriteLine(prefix + "    throw new UnsupportedOperationException();");
            else
                sw.WriteLine(prefix + $"    assign(({bean.Name})_o_);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();

            if (bean.Variables.Count > 0)
                sw.WriteLine(prefix + "public void assign(" + bean.Name + " _o_) {");
            else
            {
                sw.WriteLine(prefix + "@SuppressWarnings(\"EmptyMethod\")");
                sw.WriteLine(prefix + "public void assign(@SuppressWarnings(\"unused\") " + bean.Name + " _o_) {");
            }
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new Assign(var, sw, prefix + "    ", !bean.OnlyData));
            sw.WriteLine(prefix + "}");
            sw.WriteLine();

            if (!bean.OnlyData)
            {
                if (bean.Variables.Count > 0)
                    sw.WriteLine(prefix + "public void assign(" + bean.Name + ".Data _o_) {");
                else
                {
                    sw.WriteLine(prefix + "@SuppressWarnings(\"EmptyMethod\")");
                    sw.WriteLine(prefix + "public void assign(@SuppressWarnings(\"unused\") " + bean.Name + ".Data _o_) {");
                }
                foreach (Variable var in bean.Variables)
                    var.VariableType.Accept(new Assign(var, sw, prefix + "    ", false));
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
            }
        }

        public Assign(Variable var, StreamWriter sw, string prefix, bool transBean)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
            this.transBean = transBean;
            transGetter = transBean ? var.Getter : var.NamePrivate;
        }

        public void Visit(TypeBool type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
            if (type.ValueType.IsNormalBean)
            {
                if (transBean)
                {
                    sw.WriteLine(prefix + "for (var _e_ : _o_." + var.NamePrivate + ") {");
                    type.ValueType.Accept(new Define("_v_", sw, prefix + "    "));
                    sw.WriteLine(prefix + "    _v_.assign(_e_);");
                    sw.WriteLine(prefix + "    " + var.NamePrivate + ".add(_v_);");
                    sw.WriteLine(prefix + "}");
                }
                else
                {
                    sw.WriteLine(prefix + "for (var _e_ : _o_." + var.NamePrivate + ")");
                    sw.WriteLine(prefix + "    " + var.NamePrivate + ".add(_e_.copy());");
                }
            }
            else if (transBean && type.ValueType.Name.StartsWith("vector") && !string.IsNullOrEmpty(type.Variable.JavaType))
                sw.WriteLine(prefix + var.NamePrivate + ".addAllVector(_o_." + var.NamePrivate + ");");
            else
                sw.WriteLine(prefix + var.NamePrivate + ".addAll(_o_." + var.NamePrivate + ");");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
            if (type.ValueType.IsNormalBean)
            {
                if (transBean)
                {
                    sw.WriteLine(prefix + "for (var _e_ : _o_." + var.NamePrivate + ") {");
                    type.ValueType.Accept(new Define("_v_", sw, prefix + "    "));
                    sw.WriteLine(prefix + "    _v_.assign(_e_);");
                    sw.WriteLine(prefix + "    " + var.NamePrivate + ".add(_v_);");
                    sw.WriteLine(prefix + "}");
                }
                else
                {
                    sw.WriteLine(prefix + "for (var _e_ : _o_." + var.NamePrivate + ")");
                    sw.WriteLine(prefix + "    " + var.NamePrivate + ".add(_e_.copy());"); // set 里面现在不让放 bean，先这样写吧。
                }
            }
            else
                sw.WriteLine(prefix + var.NamePrivate + ".addAll(_o_." + var.NamePrivate + ");");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
            if (type.ValueType.IsNormalBean)
            {
                if (transBean)
                {
                    sw.WriteLine(prefix + "for (var _e_ : _o_." + var.NamePrivate + ".entrySet()) {");
                    type.ValueType.Accept(new Define("_v_", sw, prefix + "    "));
                    sw.WriteLine(prefix + "    _v_.assign(_e_.getValue());");
                    sw.WriteLine(prefix + "    " + var.NamePrivate + ".put(_e_.getKey(), _v_);");
                    sw.WriteLine(prefix + "}");
                }
                else if (string.IsNullOrEmpty(type.Variable.JavaType))
                {
                    sw.WriteLine(prefix + "for (var _e_ : _o_." + var.NamePrivate + ".entrySet())");
                    sw.WriteLine(prefix + "    " + var.NamePrivate + ".put(_e_.getKey(), _e_.getValue().copy());");
                }
                else
                {
                    sw.WriteLine(prefix + "_o_." + var.NamePrivate + ".foreach((k, v) -> "
                                 + var.NamePrivate + ".put(k, v.copy()));");
                }
            }
            else
                sw.WriteLine(prefix + var.NamePrivate + ".putAll(_o_." + var.NamePrivate + ");");
        }

        public void Visit(Bean type)
        {
            if (transBean && !type.OnlyData)
                sw.WriteLine(prefix + var.NamePrivate + ".assign(_o_." + var.NamePrivate + ".getValue());");
            else
                sw.WriteLine(prefix + var.NamePrivate + ".assign(_o_." + var.NamePrivate + ");");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".assign(_o_." + var.NamePrivate + ");");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = _o_.{transGetter};");
        }
    }
}
