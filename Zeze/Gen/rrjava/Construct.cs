using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.rrjava
{
    public class Construct : Visitor
    {
		readonly StreamWriter sw;
		readonly Variable variable;
		readonly string prefix;

		public static void Make(Bean bean, StreamWriter sw, string prefix)
		{
            sw.WriteLine(prefix + "public " + bean.Name + "() {");
            var hasImmutable = false;
            foreach (var var in bean.Variables)
            {
                if (var.VariableType.IsImmutable)
                    hasImmutable = true;
                var.VariableType.Accept(new Construct(sw, var, prefix + "    "));
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            if (hasImmutable)
            {
                sw.Write(prefix + "public " + bean.Name + '(');
                var first = true;
                foreach (var var in bean.Variables)
                {
                    if (var.VariableType.IsImmutable)
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
                        if (!var.VariableType.IsJavaPrimitive)
                        {
                            sw.WriteLine($"{prefix}    if ({var.NamePrivate}_ == null)");
                            sw.WriteLine($"{prefix}        throw new IllegalArgumentException();");
                        }
                        sw.WriteLine($"{prefix}    {var.NamePrivate} = {var.NamePrivate}_;");
                    }
                    else
                        var.VariableType.Accept(new Construct(sw, var, prefix + "    "));
                }

                sw.WriteLine(prefix + "}");
                sw.WriteLine();
            }
        }

        public Construct(StreamWriter sw, Variable variable, string prefix)
		{
			this.sw = sw;
			this.variable = variable;
			this.prefix = prefix;
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

        public void Visit(Bean type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + variable.NamePrivate + " = new " + typeName + "();");
            sw.WriteLine(prefix + variable.NamePrivate + $".variableId({variable.Id});");
        }

        public void Visit(BeanKey type)
        {
            string typeName = TypeName.GetName(type);
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
            string value = variable.Initial;
            string varname = variable.NamePrivate;
            sw.WriteLine(prefix + varname + " = \"" + value + "\";");
        }

        public void Visit(TypeList type)
        {
            string typeName = TypeName.GetSimpleName(type);
            sw.WriteLine(prefix + variable.NamePrivate + $" = new {typeName}({BoxingName.GetBoxingName(type.ValueType)}.class);");
            sw.WriteLine(prefix + variable.NamePrivate + $".variableId({variable.Id});");
        }

        public void Visit(TypeSet type)
        {
            string typeName = TypeName.GetSimpleName(type);
            sw.WriteLine(prefix + variable.NamePrivate + $" = new {typeName}({BoxingName.GetBoxingName(type.ValueType)}.class);");
            sw.WriteLine(prefix + variable.NamePrivate + $".variableId({variable.Id});");
        }

        public void Visit(TypeMap type)
        {
            string typeName = TypeName.GetSimpleName(type);
            sw.WriteLine(prefix + variable.NamePrivate + $" = new {typeName}({BoxingName.GetBoxingName(type.KeyType)}.class, {BoxingName.GetBoxingName(type.ValueType)}.class);");
            sw.WriteLine(prefix + variable.NamePrivate + $".variableId({variable.Id});");
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
            sw.WriteLine(prefix + variable.NamePrivate + " = new Zeze.Raft.RocksRaft.DynamicBean"
                + $"({variable.Id}, getSpecialTypeIdFromBean_{variable.Id}, createBeanFromSpecialTypeId_{variable.Id});");
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
