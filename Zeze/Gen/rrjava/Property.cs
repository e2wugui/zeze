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
            /*
            foreach (var k in bean.MapKeyTypes)
            {
                // 【注意】{k.Name} see Zeze.Util.Reflect.GetStableName
                var p = $"_zeze_map_key_{k.Name}_";
                var n = TypeName.GetName(k);
                sw.WriteLine($"{prefix}private {n} _{p};");
                sw.WriteLine($"{prefix}public {n} _get{p}() {{ return _{p}; }}");
                sw.WriteLine($"{prefix}public void _set{p}({n} value) {{ _{p} = value; }}");
            }
            if (bean.MapKeyTypes.Count > 0)
                sw.WriteLine();
            */
            if (bean.MapKeyTypes.Count > 0)
            {
                sw.WriteLine($"{prefix}private transient Object __zeze_map_key__;");
                sw.WriteLine();
                sw.WriteLine($"{prefix}@Override");
                sw.WriteLine($"{prefix}public Object mapKey() {{");
                sw.WriteLine($"{prefix}    return __zeze_map_key__;");
                sw.WriteLine($"{prefix}}}");
                sw.WriteLine();
                sw.WriteLine($"{prefix}@Override");
                sw.WriteLine($"{prefix}public void mapKey(Object value) {{");
                sw.WriteLine($"{prefix}    __zeze_map_key__ = value;");
                sw.WriteLine($"{prefix}}}");
                sw.WriteLine();
            }

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
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static string GetLogName(Type type)
        {
            return LogName.GetName(type);
        }

        static string GetSimpleLogName(Type type)
        {
            return type is BeanKey ? "Zeze.Raft.RocksRaft.Log1.LogBeanKey<>" : GetLogName(type);
        }

        void WriteProperty(Type type, bool checkNull = false)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    if (!isManaged())");
            sw.WriteLine(prefix + "        return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();");
            sw.WriteLine(prefix + "    if (txn == null)");
            sw.WriteLine(prefix + "        return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    var log = txn.getLog(objectId() + " + var.Id + ");");
            sw.WriteLine(prefix + "    if (log == null)");
            sw.WriteLine(prefix + "        return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + $"    return (({GetLogName(type)})log).value;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public void " + var.Setter($"{typeName} value") + " {");
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
            sw.WriteLine(prefix + $"    txn.putLog(new {GetLogName(type)}(this, {var.Id}, value));"); //
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(BeanKey type)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "@SuppressWarnings(\"unchecked\")");
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    if (!isManaged())");
            sw.WriteLine(prefix + "        return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();");
            sw.WriteLine(prefix + "    if (txn == null)");
            sw.WriteLine(prefix + "        return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    var log = txn.getLog(objectId() + " + var.Id + ");");
            sw.WriteLine(prefix + "    if (null == log)");
            sw.WriteLine(prefix + "        return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + $"    return (({GetLogName(type)})log).value;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public void " + var.Setter($"{typeName} value") + " {");
            sw.WriteLine(prefix + "    if (value == null)");
            sw.WriteLine(prefix + "        throw new IllegalArgumentException();");
            sw.WriteLine(prefix + "    if (!isManaged()) {");
            sw.WriteLine(prefix + "        " + var.NamePrivate + " = value;");
            sw.WriteLine(prefix + "        return;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();");
            sw.WriteLine(prefix + $"    txn.putLog(new {GetSimpleLogName(type)}({typeName}.class, this, {var.Id}, value));"); //
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
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
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(TypeSet type)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(TypeMap type)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + " {");
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
                sw.WriteLine(prefix + "public " + rname + " " + pname + " {");
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

        public void Visit(TypeQuaternion type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeVector2 type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeVector2Int type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeVector3 type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeVector3Int type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeVector4 type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeDecimal type)
        {
            WriteProperty(type);
        }
    }
}
