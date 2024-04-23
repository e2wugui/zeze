using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.javadata
{
    public class Construct : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable variable;
        readonly string prefix;
        readonly string varName;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            var className = bean.OnlyData ? bean.Name : "Data";
            sw.WriteLine(prefix + "@SuppressWarnings(\"deprecation\")");
            sw.WriteLine(prefix + "public " + className + "() {");
            var hasCtorParam = false;
            foreach (var var in bean.Variables)
            {
                if (!bean.Version.Equals(var.Name))
                    hasCtorParam = true;
                var.VariableType.Accept(new Construct(sw, var, prefix + "    ", var.NamePrivate));
            }

            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            if (hasCtorParam)
            {
                sw.WriteLine(prefix + "@SuppressWarnings(\"deprecation\")");
                sw.Write(prefix + "public " + className + "(");
                var first = true;
                foreach (var var in bean.Variables)
                {
                    if (!bean.Version.Equals(var.Name))
                    {
                        if (first)
                            first = false;
                        else
                            sw.Write(", ");
                        sw.Write($"{TypeName.GetName(var.VariableType)} {var.NamePrivate}_");
                    }
                }

                sw.WriteLine(") {");
                foreach (var var in bean.Variables)
                {
                    if (!bean.Version.Equals(var.Name))
                    {
                        if (!var.VariableType.IsJavaPrimitive)
                        {
                            sw.WriteLine($"{prefix}    if ({var.NamePrivate}_ == null)");
                            var.VariableType.Accept(new Construct(sw, var, prefix + "        ", var.NamePrivate + '_'));
                        }
                        sw.WriteLine($"{prefix}    {var.NamePrivate} = {var.NamePrivate}_;");
                    }
                }

                sw.WriteLine(prefix + "}");
                sw.WriteLine();
            }
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "// for decode only");
            sw.WriteLine(prefix + "public " + bean.Name + "() {");
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new Construct(sw, var, prefix + "    ", var.NamePrivate));
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Construct(StreamWriter sw, Variable variable, string prefix, string varName)
        {
            this.sw = sw;
            this.variable = variable;
            this.prefix = prefix;
            this.varName = varName;
        }

        void Initial()
        {
            string value = variable.Initial;
            if (value.Length > 0)
                sw.WriteLine(prefix + varName + " = " + value + ";");
        }

        public void Visit(TypeBool type)
        {
            Initial();
        }

        public void Visit(TypeByte type)
        {
            Initial();
        }

        public void Visit(TypeShort type)
        {
            Initial();
        }

        public void Visit(TypeInt type)
        {
            Initial();
        }

        public void Visit(TypeLong type)
        {
            Initial();
        }

        public void Visit(TypeFloat type)
        {
            Initial();
        }

        public void Visit(TypeDouble type)
        {
            Initial();
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + varName + " = Zeze.Net.Binary.Empty;");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine(prefix + varName + " = \"" + variable.Initial + "\";");
        }

        public void Visit(TypeList type)
        {
            if (string.IsNullOrEmpty(type.Variable.JavaType))
                sw.WriteLine(prefix + varName + $" = new {TypeName.GetNameOmitted(type)}<>();");
            else
                sw.WriteLine(prefix + varName + $" = new {TypeName.GetNameOmitted(type)}();");
        }

        public void Visit(TypeSet type)
        {
            if (string.IsNullOrEmpty(type.Variable.JavaType))
                sw.WriteLine(prefix + varName + $" = new {TypeName.GetNameOmitted(type)}<>();");
            else
                sw.WriteLine(prefix + varName + $" = new {TypeName.GetNameOmitted(type)}();");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + varName + $" = new {TypeName.GetNameOmitted(type)}<>();");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + varName + $" = new {TypeName.GetName(type)}({variable.Initial});");
            // sw.WriteLine(prefix + varName + $".variableId({variable.Id});");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + varName + $" = new {TypeName.GetName(type)}({variable.Initial});");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + varName + " = new DynamicData_" + variable.Name + "();");
        }

        void InitialVector(Type type)
        {
            if (variable.Initial.Length > 0)
                sw.WriteLine(prefix + varName + " = new " + TypeName.GetName(type) + "(" + variable.Initial + ");");
            else
                sw.WriteLine(prefix + varName + " = " + TypeName.GetName(type) + ".ZERO;");
        }

        public void Visit(TypeVector2 type)
        {
            InitialVector(type);
        }

        public void Visit(TypeVector2Int type)
        {
            InitialVector(type);
        }

        public void Visit(TypeVector3 type)
        {
            InitialVector(type);
        }

        public void Visit(TypeVector3Int type)
        {
            InitialVector(type);
        }

        public void Visit(TypeVector4 type)
        {
            InitialVector(type);
        }

        public void Visit(TypeQuaternion type)
        {
            InitialVector(type);
        }

        public void Visit(TypeDecimal type)
        {
            string value = variable.Initial;
            if (value.Length > 0)
                sw.WriteLine(prefix + varName + " = new java.math.BigDecimal(\"" + value + "\", java.math.MathContext.DECIMAL128);");
            else
                sw.WriteLine(prefix + varName + " = java.math.BigDecimal.ZERO;");
        }
    }
}
