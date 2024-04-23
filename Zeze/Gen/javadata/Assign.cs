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
                sw.WriteLine(prefix + $"    var bean = new {bean.FullName}();");
                sw.WriteLine(prefix + $"    bean.assign(this);");
                sw.WriteLine(prefix + $"    return bean;");
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();

            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void assign(Zeze.Transaction.Bean other) {");
            if (bean.OnlyData)
                sw.WriteLine(prefix + "    throw new UnsupportedOperationException();");
            else
                sw.WriteLine(prefix + $"    assign(({bean.Name})other);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();

            sw.WriteLine(prefix + "public void assign(" + bean.Name + " other) {");
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new Assign(var, sw, prefix + "    ", !bean.OnlyData));
            sw.WriteLine(prefix + "}");
            sw.WriteLine();

            if (!bean.OnlyData)
            {
                sw.WriteLine(prefix + "public void assign(" + bean.Name + ".Data other) {");
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
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
            if (type.ValueType.IsNormalBean)
            {
                if (transBean)
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
            else if (transBean && type.ValueType.Name.StartsWith("vector") && !string.IsNullOrEmpty(type.Variable.JavaType))
                sw.WriteLine(prefix + var.NamePrivate + ".addAllVector(other." + var.NamePrivate + ");");
            else
                sw.WriteLine(prefix + var.NamePrivate + ".addAll(other." + var.NamePrivate + ");");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
            if (type.ValueType.IsNormalBean)
            {
                if (transBean)
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
            else
                sw.WriteLine(prefix + var.NamePrivate + ".addAll(other." + var.NamePrivate + ");");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
            if (type.ValueType.IsNormalBean)
            {
                if (transBean)
                {
                    sw.WriteLine(prefix + "for (var e : other." + var.NamePrivate + ".entrySet()) {");
                    type.ValueType.Accept(new Define("data", sw, prefix + "    "));
                    sw.WriteLine(prefix + "    data.assign(e.getValue());");
                    sw.WriteLine(prefix + "    " + var.NamePrivate + ".put(e.getKey(), data);");
                    sw.WriteLine(prefix + "}");
                }
                else if (string.IsNullOrEmpty(type.Variable.JavaType))
                {
                    sw.WriteLine(prefix + "for (var e : other." + var.NamePrivate + ".entrySet())");
                    sw.WriteLine(prefix + "    " + var.NamePrivate + ".put(e.getKey(), e.getValue().copy());");
                }
                else
                {
                    sw.WriteLine(prefix + "other." + var.NamePrivate + ".foreach((k, v) -> "
                                 + var.NamePrivate + ".put(k, v.copy()));");
                }
            }
            else
                sw.WriteLine(prefix + var.NamePrivate + ".putAll(other." + var.NamePrivate + ");");
        }

        public void Visit(Bean type)
        {
            if (transBean && !type.OnlyData)
                sw.WriteLine(prefix + var.NamePrivate + ".assign(other." + var.NamePrivate + ".getValue());");
            else
                sw.WriteLine(prefix + var.NamePrivate + ".assign(other." + var.NamePrivate + ");");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".assign(other." + var.NamePrivate + ");");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine($"{prefix}{var.NamePrivate} = other.{transGetter};");
        }
    }
}
