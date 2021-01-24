using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Construct : Types.Visitor
    {
		private System.IO.StreamWriter sw;
		private Types.Variable variable;
		private String prefix;

		public static void Make(Types.Bean bean, System.IO.StreamWriter sw, String prefix)
		{
			sw.WriteLine(prefix + "public " + bean.Name + "() : this(0)");
			sw.WriteLine(prefix + "{");
			sw.WriteLine(prefix + "}");
			sw.WriteLine("");
            sw.WriteLine(prefix + "public " + bean.Name + "(int _varId_) : base(_varId_)");
            sw.WriteLine(prefix + "{");
            foreach (Types.Variable var in bean.Variables)
            {
                var.VariableType.Accept(new Construct(sw, var, prefix + "    "));
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine("");
        }

        public Construct(System.IO.StreamWriter sw, Types.Variable variable, String prefix)
		{
			this.sw = sw;
			this.variable = variable;
			this.prefix = prefix;
		}

		private void Initial()
		{
			String value = variable.Initial;
			if (value.Length > 0)
			{
				String varname = variable.NamePrivate;
				sw.WriteLine(prefix + varname + " = " + value + ";");
			}
		}

        public void Visit(Bean type)
        {
            String typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NamePrivate + " = new " + typeName + "(" + variable.Id + ");");
        }

        public void Visit(BeanKey type)
        {
            String typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NamePrivate + " = new " + typeName + "();");
        }

        public void Visit(TypeByte type)
        {
            Initial();
        }

        public void Visit(TypeDouble type)
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

        public void Visit(TypeBool type)
        {
            Initial();
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + variable.NamePrivate + " = Zeze.Net.Binary.Empty;");
        }

        public void Visit(TypeString type)
        {
            String value = variable.Initial;
            String varname = variable.NamePrivate;
            sw.WriteLine(prefix + varname + " = \"" + value + "\";");
        }

        public void Visit(TypeList type)
        {
            String typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NamePrivate + " = new " + typeName + "(ObjectId + " + variable.Id + ", _v => new Log_" + variable.NamePrivate + "(this, _v));");
        }

        public void Visit(TypeSet type)
        {
            String typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NamePrivate + " = new " + typeName + "(ObjectId + " + variable.Id + ", _v => new Log_" + variable.NamePrivate + "(this, _v));");
        }

        public void Visit(TypeMap type)
        {
            String typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NamePrivate + " = new " + typeName + "(ObjectId + " + variable.Id + ", _v => new Log_" + variable.NamePrivate + "(this, _v));");
        }

        public void Visit(TypeFloat type)
        {
            Initial();
        }

        public void Visit(TypeShort type)
        {
            Initial();
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + variable.NamePrivate + " = new Zeze.Transaction.EmptyBean();");
        }
    }
}
