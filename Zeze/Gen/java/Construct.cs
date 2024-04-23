using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Construct : Visitor
    {
		readonly StreamWriter sw;
		readonly Variable variable;
		readonly string prefix;
        readonly string beanName;
        readonly string varName;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
		{
			sw.WriteLine(prefix + "@SuppressWarnings(\"deprecation\")");
			sw.WriteLine(prefix + "public " + bean.Name + "() {");
            var hasImmutable = false;
            foreach (var var in bean.Variables)
            {
                if (var.VariableType.IsImmutable && false == bean.Version.Equals(var.Name))
                    hasImmutable = true;
                var.VariableType.Accept(new Construct(sw, var, prefix + "    ", bean.Name, var.NamePrivate));
            }
            sw.WriteLine(prefix + "}");
			sw.WriteLine();
            if (hasImmutable)
            {
                sw.WriteLine(prefix + "@SuppressWarnings(\"deprecation\")");
                sw.Write(prefix + "public " + bean.Name + '(');
                var first = true;
                foreach (var var in bean.Variables)
                {
                    if (var.VariableType.IsImmutable && false == bean.Version.Equals(var.Name))
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
                    if (var.VariableType.IsImmutable)
                    {
                        if (false == bean.Version.Equals(var.Name))
                        {
                            if (!var.VariableType.IsJavaPrimitive)
                            {
                                sw.WriteLine($"{prefix}    if ({var.NamePrivate}_ == null)");
                                var.VariableType.Accept(new Construct(sw, var, prefix + "        ", bean.Name, var.NamePrivate + '_'));
                            }
                            sw.WriteLine($"{prefix}    {var.NamePrivate} = {var.NamePrivate}_;");
                        }
                    }
                    else
                        var.VariableType.Accept(new Construct(sw, var, prefix + "    ", bean.Name, var.NamePrivate));
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
                var.VariableType.Accept(new Construct(sw, var, prefix + "    ", bean.Name, var.NamePrivate));
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Construct(StreamWriter sw, Variable variable, string prefix, string beanName, string varName)
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
            string typeName = TypeName.GetNameOmitted(type) + "<>";
            if (type.ValueType is TypeDynamic valueType)
                sw.WriteLine(prefix + varName + $" = new {typeName}({GetAndCreateDynamicBean(valueType)});");
            else
                sw.WriteLine(prefix + varName + $" = new {typeName}({BoxingName.GetBoxingName(type.ValueType)}.class);");
            sw.WriteLine(prefix + varName + $".variableId({variable.Id});");
        }

        public void Visit(TypeSet type)
        {
            string typeName = TypeName.GetNameOmitted(type) + "<>";
            if (type.ValueType is TypeDynamic valueType)
                sw.WriteLine(prefix + varName + $" = new {typeName}({GetAndCreateDynamicBean(valueType)});");
            else
                sw.WriteLine(prefix + varName + $" = new {typeName}({BoxingName.GetBoxingName(type.ValueType)}.class);");
            sw.WriteLine(prefix + varName + $".variableId({variable.Id});");
        }

        public void Visit(TypeMap type)
        {
            string typeName = TypeName.GetNameOmitted(type) + "<>";
            if (type.ValueType is TypeDynamic valueType)
                sw.WriteLine(prefix + varName + $" = new {typeName}({BoxingName.GetBoxingName(type.KeyType)}.class, {GetAndCreateDynamicBean(valueType)});");
            else
                sw.WriteLine(prefix + varName + $" = new {typeName}({BoxingName.GetBoxingName(type.KeyType)}.class, {BoxingName.GetBoxingName(type.ValueType)}.class);");
            sw.WriteLine(prefix + varName + $".variableId({variable.Id});");
            /*
            var key = TypeName.GetName(type.KeyType);
            var value = type.ValueType.IsNormalBean
                ? TypeName.GetName(type.ValueType) + "ReadOnly"
                : TypeName.GetName(type.ValueType);
            var readonlyTypeName = $"Zeze.Transaction.Collections.PMapReadOnly<{key},{value},{TypeName.GetName(type.ValueType)}>";
            sw.WriteLine($"{prefix}{varName}ReadOnly = new {readonlyTypeName}({varName});");
            */
        }

        public void Visit(Bean type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + varName + $" = new Zeze.Transaction.Collections.CollOne<>(new {typeName}({variable.Initial}), {typeName}.class);");
            sw.WriteLine(prefix + varName + $".variableId({variable.Id});");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + varName + $" = new {TypeName.GetName(type)}({variable.Initial});");
        }

        string GetAndCreateDynamicBean(TypeDynamic type)
        {
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId)) // 判断一个就够了。
            {
                return $"{beanName}::getSpecialTypeIdFromBean_{variable.Id}, " +
                       $"{beanName}::createBeanFromSpecialTypeId_{variable.Id}";
            }
            return $"{type.DynamicParams.GetSpecialTypeIdFromBean}, " +
                   $"{type.DynamicParams.CreateBeanFromSpecialTypeId}";
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + varName + " = newDynamicBean_" + variable.NameUpper1 + "();");
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
