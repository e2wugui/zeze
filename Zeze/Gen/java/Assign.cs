using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Assign : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;
        readonly bool isData;
        readonly string transGetter;

        public static void Make(Bean bean, StreamWriter sw, string prefix, Project project, bool withUnknown)
        {
            if (Program.isData(bean))
            {
                sw.WriteLine(prefix + "@Override");
                sw.WriteLine(prefix + $"public {bean.FullName}.Data toData() {{");
                sw.WriteLine(prefix + $"    var _d_ = new {bean.FullName}.Data();");
                sw.WriteLine(prefix + $"    _d_.assign(this);");
                sw.WriteLine(prefix + $"    return _d_;");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                sw.WriteLine(prefix + "@Override");
                sw.WriteLine(prefix + "public void assign(Zeze.Transaction.Data _o_) {");
                sw.WriteLine(prefix + $"    assign(({bean.FullName}.Data)_o_);");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                sw.WriteLine(prefix + "public void assign(" + bean.Name + ".Data _o_) {");
                foreach (Variable var in bean.Variables)
                    var.VariableType.Accept(new Assign(var, sw, prefix + "    ", true));
                if (withUnknown)
                    sw.WriteLine(prefix + "    _unknown_ = null;");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
            }
            sw.WriteLine(prefix + "public void assign(" + bean.Name + " _o_) {");
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new Assign(var, sw, prefix + "    ", false));
            if (withUnknown)
                sw.WriteLine(prefix + "    _unknown_ = _o_._unknown_;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Assign(Variable var, StreamWriter sw, string prefix, bool isData)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
            this.isData = isData;
            transGetter = isData ? var.NamePrivate : var.Getter;
        }

        public void Visit(TypeBool type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeList type)
        {
            if (type.ValueType.IsNormalBean)
            {
                sw.WriteLine(prefix + var.NamePrivate + ".clear();");
                if (isData)
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
            else if (isData)
            {
                sw.WriteLine(prefix + var.NamePrivate + ".clear();");
                if (!string.IsNullOrEmpty(type.Variable.JavaType))
                {
                    if (type.ValueType.Name.StartsWith("vector"))
                        sw.WriteLine($"{prefix}_o_.{var.NamePrivate}.addAllToVector({var.NamePrivate});");
                    else
                        sw.WriteLine($"{prefix}_o_.{var.NamePrivate}.addAllTo({var.NamePrivate});");
                }
                else
                    sw.WriteLine($"{prefix}{var.NamePrivate}.addAll(_o_.{var.NamePrivate});");
            }
            else
                sw.WriteLine($"{prefix}{var.NamePrivate}.assign(_o_.{var.NamePrivate});");
        }

        public void Visit(TypeSet type)
        {
            if (type.ValueType.IsNormalBean)
            {
                sw.WriteLine(prefix + var.NamePrivate + ".clear();");
                if (isData)
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
            else if (isData)
            {
                sw.WriteLine(prefix + var.NamePrivate + ".clear();");
                if (!string.IsNullOrEmpty(type.Variable.JavaType))
                    sw.WriteLine($"{prefix}_o_.{var.NamePrivate}.addAllTo({var.NamePrivate});");
                else
                    sw.WriteLine($"{prefix}{var.NamePrivate}.addAll(_o_.{var.NamePrivate});");
            }
            else
                sw.WriteLine($"{prefix}{var.NamePrivate}.assign(_o_.{var.NamePrivate});");
        }

        public void Visit(TypeMap type)
        {
            if (type.ValueType.IsNormalBean)
            {
                sw.WriteLine(prefix + var.NamePrivate + ".clear();");
                if (isData)
                {
                    if (string.IsNullOrEmpty(type.Variable.JavaType))
                    {
                        sw.WriteLine(prefix + "for (var _e_ : _o_." + var.NamePrivate + ".entrySet()) {");
                        type.ValueType.Accept(new Define("_v_", sw, prefix + "    "));
                        sw.WriteLine(prefix + "    _v_.assign(_e_.getValue());");
                        sw.WriteLine(prefix + "    " + var.NamePrivate + ".put(_e_.getKey(), _v_);");
                        sw.WriteLine(prefix + "}");
                    }
                    else
                    {
                        sw.WriteLine(prefix + "_o_." + var.NamePrivate + ".foreach((k, v) -> {");
                        type.ValueType.Accept(new Define("_v_", sw, prefix + "    "));
                        sw.WriteLine(prefix + "    _v_.assign(v);");
                        sw.WriteLine(prefix + "    " + var.NamePrivate + ".put(k, _v_);");
                        sw.WriteLine(prefix + "});");
                    }
                }
                else
                {
                    sw.WriteLine(prefix + "for (var _e_ : _o_." + var.NamePrivate + ".entrySet())");
                    sw.WriteLine(prefix + "    " + var.NamePrivate + ".put(_e_.getKey(), _e_.getValue().copy());");
                }
            }
            else if (isData)
            {
                sw.WriteLine(prefix + var.NamePrivate + ".clear();");
                if (!string.IsNullOrEmpty(type.Variable.JavaType))
                    sw.WriteLine($"{prefix}_o_.{var.NamePrivate}.putAllTo({var.NamePrivate});");
                else
                    sw.WriteLine($"{prefix}{var.NamePrivate}.putAll(_o_.{var.NamePrivate});");
            }
            else
                sw.WriteLine($"{prefix}{var.NamePrivate}.assign(_o_.{var.NamePrivate});");
        }

        public void Visit(Bean type)
        {
            if (isData)
            {
                var tmpVarName = "_d_" + var.NamePrivate;
                type.Accept(new Define(tmpVarName, sw, prefix));
                sw.WriteLine(prefix + tmpVarName + ".assign(_o_." + var.NamePrivate + ");");
                sw.WriteLine(prefix + var.NamePrivate + ".setValue(" + tmpVarName + ");");
            }
            else
            {
                sw.WriteLine(prefix + var.NamePrivate + ".assign(_o_." + var.NamePrivate + ");");
            }
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".assign(_o_." + var.NamePrivate + ");");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine(prefix + var.Setter($"_o_.{transGetter}") + ";");
        }
    }
}
