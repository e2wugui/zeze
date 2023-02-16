﻿using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.javadata
{
    public class Construct : Visitor
    {
		readonly StreamWriter sw;
		readonly Variable variable;
		readonly string prefix;
        readonly string beanName;

		public static void Make(Bean bean, StreamWriter sw, string prefix)
		{
			sw.WriteLine(prefix + "@SuppressWarnings(\"deprecation\")");
			sw.WriteLine(prefix + "public " + bean.Name + "Data() {");
            var hasImmutable = false;
            foreach (var var in bean.Variables)
            {
                if (var.VariableType.IsImmutable && false == bean.Version.Equals(var.Name))
                    hasImmutable = true;
                var.VariableType.Accept(new Construct(sw, var, prefix + "    ", bean.Name));
            }
            sw.WriteLine(prefix + "}");
			sw.WriteLine();
            if (hasImmutable)
            {
                sw.WriteLine(prefix + "@SuppressWarnings(\"deprecation\")");
                sw.Write(prefix + "public " + bean.Name + "Data(");
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
                                sw.WriteLine($"{prefix}        throw new IllegalArgumentException();");
                            }
                            sw.WriteLine($"{prefix}    {var.NamePrivate} = {var.NamePrivate}_;");
                        }
                    }
                    else
                        var.VariableType.Accept(new Construct(sw, var, prefix + "    ", bean.Name));
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
                var.VariableType.Accept(new Construct(sw, var, prefix + "    ", bean.Name));
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Construct(StreamWriter sw, Variable variable, string prefix, string beanName)
		{
			this.sw = sw;
			this.variable = variable;
			this.prefix = prefix;
            this.beanName = beanName;
        }

		void Initial()
		{
            string value = variable.Initial;
			if (value.Length > 0)
			{
                string varname = variable.NamePrivate;
				sw.WriteLine(prefix + varname + " = " + value + ";");
			}
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
            sw.WriteLine(prefix + variable.NamePrivate + " = Zeze.Net.Binary.Empty;");
        }

        public void Visit(TypeString type)
        {
            string value = variable.Initial;
            string varname = variable.NamePrivate;
            sw.WriteLine(prefix + varname + " = \"" + value + "\";");
        }

        public void Visit(TypeList type)
        {
            string typeName = TypeName.GetNameOmitted(type) + "<>";
            if (type.ValueType is TypeDynamic valueType)
                sw.WriteLine(prefix + variable.NamePrivate + $" = new {typeName}();");
            else
                sw.WriteLine(prefix + variable.NamePrivate + $" = new {typeName}();");
        }

        public void Visit(TypeSet type)
        {
            string typeName = TypeName.GetNameOmitted(type) + "<>";
            if (type.ValueType is TypeDynamic valueType)
                sw.WriteLine(prefix + variable.NamePrivate + $" = new {typeName}();");
            else
                sw.WriteLine(prefix + variable.NamePrivate + $" = new {typeName}();");
        }

        public void Visit(TypeMap type)
        {
            string typeName = TypeName.GetNameOmitted(type) + "<>";
            if (type.ValueType is TypeDynamic valueType)
                sw.WriteLine(prefix + variable.NamePrivate + $" = new {typeName}();");
            else
                sw.WriteLine(prefix + variable.NamePrivate + $" = new {typeName}();");
        }

        public void Visit(Bean type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NamePrivate + $" = new {typeName}();");
            sw.WriteLine(prefix + variable.NamePrivate + $".variableId({variable.Id});");
        }

        public void Visit(BeanKey type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NamePrivate + " = new " + typeName + "();");
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
            sw.WriteLine(prefix + variable.NamePrivate + " = newDynamicBean_" + variable.NameUpper1 + "();");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine(prefix + variable.NamePrivate + " = " + TypeName.GetName(type) + ".ZERO;");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine(prefix + variable.NamePrivate + " = " + TypeName.GetName(type) + ".ZERO;");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine(prefix + variable.NamePrivate + " = " + TypeName.GetName(type) + ".ZERO;");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine(prefix + variable.NamePrivate + " = " + TypeName.GetName(type) + ".ZERO;");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine(prefix + variable.NamePrivate + " = " + TypeName.GetName(type) + ".ZERO;");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine(prefix + variable.NamePrivate + " = " + TypeName.GetName(type) + ".ZERO;");
        }
    }
}