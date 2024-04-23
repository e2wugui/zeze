using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.rrcs
{
    public class Construct : Visitor
    {
		readonly StreamWriter sw;
		readonly Variable variable;
		readonly string prefix;

		public static void Make(Bean bean, StreamWriter sw, string prefix)
		{
            sw.WriteLine(prefix + "public " + bean.Name + "()");
            sw.WriteLine(prefix + "{");
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

                sw.WriteLine(")");
                sw.WriteLine($"{prefix}{{");
                foreach (var var in bean.Variables)
                {
                    if (var.VariableType.IsImmutable)
                        sw.WriteLine($"{prefix}    {var.NamePrivate} = {var.NamePrivate}_;");
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
				sw.WriteLine(prefix + variable.NamePrivate + " = " + value + ";");
		}

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + variable.NamePrivate + $" = new {TypeName.GetName(type)}({variable.Initial});");
            sw.WriteLine(prefix + variable.NamePrivate + $".VariableId = {variable.Id};");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + variable.NamePrivate + $" = new {TypeName.GetName(type)}({variable.Initial});");
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
            sw.WriteLine(prefix + variable.NamePrivate + " = \"" + variable.Initial + "\";");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + variable.NamePrivate + $" = new {TypeName.GetName(type)}() {{ VariableId = {variable.Id} }};");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + variable.NamePrivate + $" = new {TypeName.GetName(type)}() {{ VariableId = {variable.Id} }};");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + variable.NamePrivate + $" = new {TypeName.GetName(type)}() {{ VariableId = {variable.Id} }};");
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
                + $"({variable.Id}, GetSpecialTypeIdFromBean_{variable.Id}, CreateBeanFromSpecialTypeId_{variable.Id});");
        }

        public void Visit(TypeQuaternion type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector2 type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector2Int type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector3 type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector3Int type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector4 type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeDecimal type)
        {
            Initial();
        }
    }
}
