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
                sw.WriteLine(prefix + $"    var data = new {bean.FullName}.Data();");
                sw.WriteLine(prefix + $"    data.assign(this);");
                sw.WriteLine(prefix + $"    return data;");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                sw.WriteLine(prefix + "@Override");
                sw.WriteLine(prefix + "public void assign(Zeze.Transaction.Data other) {");
                sw.WriteLine(prefix + $"    assign(({bean.FullName}.Data)other);");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                sw.WriteLine(prefix + "public void assign(" + bean.Name + ".Data other) {");
                foreach (Variable var in bean.Variables)
                    var.VariableType.Accept(new Assign(var, sw, prefix + "    ", true));
                if (withUnknown)
                    sw.WriteLine(prefix + "    _unknown_ = null;");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
            }
            sw.WriteLine(prefix + "public void assign(" + bean.Name + " other) {");
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new Assign(var, sw, prefix + "    ", false));
            if (withUnknown)
                sw.WriteLine(prefix + "    _unknown_ = other._unknown_;");
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
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
            if (type.ValueType.IsNormalBean)
            {
                if (isData)
                {
                    sw.WriteLine(prefix + "for (var e : other." + var.NamePrivate + ") {");
                    type.ValueType.Accept(new Define("data", sw, prefix + "    "));
                    sw.WriteLine(prefix + "    data.assign(e);");
                    sw.WriteLine(prefix + "    " + var.NamePrivate + ".add(data);");
                    sw.WriteLine(prefix + "}");
                }
                else
                {
                    sw.WriteLine(prefix + "for (var e : other." + var.NamePrivate + ")");
                    sw.WriteLine(prefix + "    " + var.NamePrivate + ".add(e.copy());");
                }
            }
            else if (isData && !string.IsNullOrEmpty(type.Variable.JavaType))
            {
                if (type.ValueType.Name.StartsWith("vector"))
                    sw.WriteLine($"{prefix}other.{var.NamePrivate}.addAllToVector({var.NamePrivate});");
                else
                    sw.WriteLine($"{prefix}other.{var.NamePrivate}.addAllTo({var.NamePrivate});");
            }
            else
                sw.WriteLine($"{prefix}{var.NamePrivate}.addAll(other.{var.NamePrivate});");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
            if (type.ValueType.IsNormalBean)
            {
                if (isData)
                {
                    sw.WriteLine(prefix + "for (var e : other." + var.NamePrivate + ") {");
                    type.ValueType.Accept(new Define("data", sw, prefix + "    "));
                    sw.WriteLine(prefix + "    data.assign(e);");
                    sw.WriteLine(prefix + "    " + var.NamePrivate + ".add(data);");
                    sw.WriteLine(prefix + "}");
                }
                else
                {
                    sw.WriteLine(prefix + "for (var e : other." + var.NamePrivate + ")");
                    sw.WriteLine(prefix + "    " + var.NamePrivate + ".add(e.copy());"); // set 里面现在不让放 bean，先这样写吧。
                }
            }
            else if (isData && !string.IsNullOrEmpty(type.Variable.JavaType))
                sw.WriteLine($"{prefix}other.{var.NamePrivate}.addAllTo({var.NamePrivate});");
            else
                sw.WriteLine($"{prefix}{var.NamePrivate}.addAll(other.{var.NamePrivate});");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
            if (type.ValueType.IsNormalBean)
            {
                if (isData)
                {
                    if (string.IsNullOrEmpty(type.Variable.JavaType))
                    {
                        sw.WriteLine(prefix + "for (var e : other." + var.NamePrivate + ".entrySet()) {");
                        type.ValueType.Accept(new Define("data", sw, prefix + "    "));
                        sw.WriteLine(prefix + "    data.assign(e.getValue());");
                        sw.WriteLine(prefix + "    " + var.NamePrivate + ".put(e.getKey(), data);");
                        sw.WriteLine(prefix + "}");
                    }
                    else
                    {
                        sw.WriteLine(prefix + "other." + var.NamePrivate + ".foreach((k, v) -> {");
                        type.ValueType.Accept(new Define("data", sw, prefix + "    "));
                        sw.WriteLine(prefix + "    data.assign(v);");
                        sw.WriteLine(prefix + "    " + var.NamePrivate + ".put(k, data);");
                        sw.WriteLine(prefix + "});");
                    }
                }
                else
                {
                    sw.WriteLine(prefix + "for (var e : other." + var.NamePrivate + ".entrySet())");
                    sw.WriteLine(prefix + "    " + var.NamePrivate + ".put(e.getKey(), e.getValue().copy());");
                }
            }
            else if (isData && !string.IsNullOrEmpty(type.Variable.JavaType))
                sw.WriteLine($"{prefix}other.{var.NamePrivate}.putAllTo({var.NamePrivate});");
            else
                sw.WriteLine($"{prefix}{var.NamePrivate}.putAll(other.{var.NamePrivate});");
        }

        public void Visit(Bean type)
        {
            if (isData)
            {
                var tmpVarName = "data" + var.NamePrivate;
                type.Accept(new Define(tmpVarName, sw, prefix));
                sw.WriteLine(prefix + tmpVarName + ".assign(other." + var.NamePrivate + ");");
                sw.WriteLine(prefix + var.NamePrivate + ".setValue(" + tmpVarName + ");");
            }
            else
            {
                sw.WriteLine(prefix + var.NamePrivate + ".assign(other." + var.NamePrivate + ");");
            }
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".assign(other." + var.NamePrivate + ");");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{transGetter}") + ";");
        }
    }
}
