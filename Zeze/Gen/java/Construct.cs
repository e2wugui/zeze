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

		public static void Make(Bean bean, StreamWriter sw, string prefix)
		{
			sw.WriteLine(prefix + "public " + bean.Name + "() {");
			sw.WriteLine(prefix + "     this(0);");
			sw.WriteLine(prefix + "}");
			sw.WriteLine();
            sw.WriteLine(prefix + "public " + bean.Name + "(int _varId_) {");
            sw.WriteLine(prefix + "    super(_varId_);");
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new Construct(sw, var, prefix + "    ", bean.Name));
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
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
            sw.WriteLine(prefix + variable.NamePrivate + " = new " + typeName + "(getObjectId() + " + variable.Id + ", (_v) -> new Log_" + variable.NamePrivate + "(this, _v));");
        }

        public void Visit(TypeSet type)
        {
            string typeName = TypeName.GetNameOmitted(type) + "<>";
            sw.WriteLine(prefix + variable.NamePrivate + " = new " + typeName + "(getObjectId() + " + variable.Id + ", (_v) -> new Log_" + variable.NamePrivate + "(this, _v));");
        }

        public void Visit(TypeMap type)
        {
            string typeName = TypeName.GetNameOmitted(type) + "<>";
            sw.WriteLine(prefix + variable.NamePrivate + " = new " + typeName + "(getObjectId() + " + variable.Id + ", (_v) -> new Log_" + variable.NamePrivate + "(this, _v));");
            /*
            var key = TypeName.GetName(type.KeyType);
            var value = type.ValueType.IsNormalBean
                ? TypeName.GetName(type.ValueType) + "ReadOnly"
                : TypeName.GetName(type.ValueType);
            var readonlyTypeName = $"Zeze.Transaction.Collections.PMapReadOnly<{key},{value},{TypeName.GetName(type.ValueType)}>";
            sw.WriteLine($"{prefix}{variable.NamePrivate}ReadOnly = new {readonlyTypeName}({variable.NamePrivate});");
            */
        }

        public void Visit(Bean type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NamePrivate + " = new " + typeName + "(" + variable.Id + ");");
        }

        public void Visit(BeanKey type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NamePrivate + " = new " + typeName + "();");
        }

        public void Visit(TypeDynamic type)
        {
            if (string.IsNullOrEmpty(type.CreateBeanFromSpecialTypeId)) // 判断一个就够了。
            {
                sw.WriteLine(prefix + variable.NamePrivate + " = new Zeze.Transaction.DynamicBean"
                    + $"({variable.Id}, {beanName}::GetSpecialTypeIdFromBean_{variable.NameUpper1}, "
                    + $"{beanName}::CreateBeanFromSpecialTypeId_{variable.NameUpper1});");
            }
            else
            {
                sw.WriteLine(prefix + variable.NamePrivate + " = new Zeze.Transaction.DynamicBean"
                    + $"({variable.Id}, {type.GetSpecialTypeIdFromBean}, {type.CreateBeanFromSpecialTypeId});");
            }
        }
    }
}
