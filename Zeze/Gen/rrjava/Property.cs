using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.rrjava
{
    public class Property : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            foreach (Variable var in bean.Variables)
            {
                if (var.Transient)
                {
                    var type = TypeName.GetName(var.VariableType);
                    sw.WriteLine($"{prefix}public {type} {var.Getter} {{");
                    sw.WriteLine($"{prefix}    return {var.NamePrivate};");
                    sw.WriteLine($"{prefix}}}");
                    sw.WriteLine();
                    sw.WriteLine($"{prefix}public void {var.Setter(type + " value")} {{");
                    sw.WriteLine($"{prefix}    {var.NamePrivate} = value;");
                    sw.WriteLine($"{prefix}}}");
                    sw.WriteLine();

                    continue;
                }
                var.VariableType.Accept(new Property(sw, var, prefix));
            }
        }

        public Property(StreamWriter sw, Variable var, string prefix)
        {
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
        }

        public void Visit(Bean type)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + "{");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static string GetLogName(Type type)
        {
            switch (type.Name)
            {
                case "bool": return "Zeze.Raft.RocksRaft.Log1.LogBool";
                case "byte": return "Zeze.Raft.RocksRaft.Log1.LogByte";
                case "short": return "Zeze.Raft.RocksRaft.Log1.LogShort";
                case "int": return "Zeze.Raft.RocksRaft.Log1.LogInt";
                case "long": return "Zeze.Raft.RocksRaft.Log1.LogLong";

                case "binary": return "Zeze.Raft.RocksRaft.Log1.LogBinary";
                case "string": return "Zeze.Raft.RocksRaft.Log1.LogString";
            }
            return null;
        }
        void WriteProperty(Type type, bool checkNull = false)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + "{");
            sw.WriteLine(prefix + "    if (!isManaged())");
            sw.WriteLine(prefix + "        return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();");
            sw.WriteLine(prefix + "    if (txn == null)");
            sw.WriteLine(prefix + "        return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    var log = txn.GetLog(getObjectId() + " + var.Id + ");");
            sw.WriteLine(prefix + "    if (null == log)");
            sw.WriteLine(prefix + "        return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + $"    return (({GetLogName(type)})log).Value;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public void " + var.Setter($"{typeName} value") + "{");
            if (checkNull)
            {
                sw.WriteLine(prefix + "    if (value == null)");
                sw.WriteLine(prefix + "        throw new IllegalArgumentException();");
            }
            sw.WriteLine(prefix + "    if (!isManaged()) {");
            sw.WriteLine(prefix + "        " + var.NamePrivate + " = value;");
            sw.WriteLine(prefix + "        return;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();");
            sw.WriteLine(prefix + "    assert txn != null;");
            sw.WriteLine(prefix + $"    txn.PutLog(new {GetLogName(type)}(this, {var.Id}, value));"); // 
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(BeanKey type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeByte type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeDouble type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeInt type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeLong type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeBool type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeBinary type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeString type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeList type)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + "{");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(TypeSet type)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + "{");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(TypeMap type)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + "{");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(TypeFloat type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeShort type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeDynamic type)
        {
            var typeName = TypeName.GetName(type);
            //var beanNameReadOnly = TypeName.GetName(var.Bean) + "ReadOnly";
            sw.WriteLine($"{prefix}public {typeName} {var.Getter} {{");
            sw.WriteLine($"{prefix}    return {var.NamePrivate};");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            foreach (Bean real in type.RealBeans.Values)
            {
                string rname = TypeName.GetName(real);
                string pname = "get" + var.NameUpper1 + "_" + real.Space.Path("_", real.Name) + "()";
                sw.WriteLine(prefix + "public " + rname + " " + pname + "{");
                sw.WriteLine(prefix + "    return (" + rname + ")" + var.Getter + ".getBean();");
                sw.WriteLine(prefix + "}");
                sw.WriteLine(prefix + $"public void {var.Setter($"{rname} value")} {{");
                sw.WriteLine(prefix + "    " + var.Getter + ".setBean(value);");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                //sw.WriteLine(prefix + rname + "ReadOnly " + beanNameReadOnly + "." + pname + " => " + pname + ";");
                //sw.WriteLine();
            }
        }
    }
}
