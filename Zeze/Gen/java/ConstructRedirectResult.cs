using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class ConstructRedirectResult : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable variable;
        readonly string prefix;
        readonly string beanName;
        readonly string varName;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public " + bean.Name + "() {");
            foreach (var var in bean.Variables)
            {
                var.VariableType.Accept(new ConstructRedirectResult(sw, var, prefix + "    ", bean.Name, var.Name));
            }
            sw.WriteLine(prefix + "}");
        }

        public ConstructRedirectResult(StreamWriter sw, Variable variable, string prefix, string beanName, string varName)
        {
            this.sw = sw;
            this.variable = variable;
            this.prefix = prefix;
            this.beanName = beanName;
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
            if (type.ValueType is TypeDynamic)
                throw new System.Exception("RedirectResult not support dynamic.");
            string typeName = $"java.util.ArrayList<{BoxingName.GetBoxingName(type.ValueType)}>";
            sw.WriteLine(prefix + varName + $" = new {typeName}();");
        }

        public void Visit(TypeSet type)
        {
            if (type.ValueType is TypeDynamic)
                throw new System.Exception("RedirectResult not support dynamic.");
            string typeName = $"java.util.HashSet<{BoxingName.GetBoxingName(type.ValueType)}>";
            sw.WriteLine(prefix + varName + $" = new {typeName}();");
        }

        public void Visit(TypeMap type)
        {
            if (type.ValueType is TypeDynamic)
                throw new System.Exception("RedirectResult not support dynamic.");
            if (type.KeyType is TypeDynamic)
                throw new System.Exception("RedirectResult not support dynamic.");

            string typeName = $"java.util.HashMap<{BoxingName.GetBoxingName(type.KeyType)}, {BoxingName.GetBoxingName(type.ValueType)}>";
            sw.WriteLine(prefix + varName + $" = new {typeName}();");
        }

        public void Visit(Bean type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + varName + $" = new {typeName}();");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + varName + $" = new {TypeName.GetName(type)}({variable.Initial});");
        }

        public void Visit(TypeDynamic type)
        {
            throw new System.Exception("RedirectResult not support dynamic.");
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
