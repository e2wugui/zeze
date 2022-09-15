using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Property : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
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
                var.VariableType.Accept(new Property(sw, var, prefix));
        }

        public Property(StreamWriter sw, Variable var, string prefix)
        {
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
        }

        public static string GetLogName(Type type)
        {
            return LogName.GetName(type);
        }

        void WriteProperty(Type type, bool checkNull = false)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    if (!isManaged())");
            sw.WriteLine(prefix + "        return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);");
            sw.WriteLine(prefix + "    if (txn == null)");
            sw.WriteLine(prefix + "        return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    var log = (Log_" + var.NamePrivate + ")txn.GetLog(objectId() + " + var.Id + ");");
            sw.WriteLine(prefix + "    return log != null ? log.Value : " + var.NamePrivate + ";");
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
            sw.WriteLine(prefix + "    var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);");
            sw.WriteLine(prefix + "    txn.PutLog(new Log_" + var.NamePrivate + $"(this, {var.Id}, value));"); //
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(TypeBool type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeByte type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeShort type)
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

        public void Visit(TypeFloat type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeDouble type)
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
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            /*
            var valueName = type.ValueType.IsNormalBean
                ? TypeName.GetName(type.ValueType) + "ReadOnly"
                : TypeName.GetName(type.ValueType);
            var beanNameReadOnly = TypeName.GetName(var.Bean) + "ReadOnly";
            sw.WriteLine($"{prefix}System.Collections.Generic.IReadOnlyList<{valueName}> {beanNameReadOnly}.{var.NameUpper1} => {var.NamePrivate};");
            sw.WriteLine();
            */
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            /*
            var v = TypeName.GetName(type.ValueType);
            var t = $"System.Collections.Generic.IReadOnlySet<{v}>";
            var beanNameReadOnly = TypeName.GetName(var.Bean) + "ReadOnly";
            sw.WriteLine($"{prefix}{t} {beanNameReadOnly}.{var.NameUpper1} => {var.NamePrivate};");
            sw.WriteLine();
            */
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            /*
            var valueName = type.ValueType.IsNormalBean
                ? TypeName.GetName(type.ValueType) + "ReadOnly"
                : TypeName.GetName(type.ValueType);
            var keyName = TypeName.GetName(type.KeyType);
            var beanNameReadOnly = TypeName.GetName(var.Bean) + "ReadOnly";
            sw.WriteLine($"{prefix}System.Collections.Generic.IReadOnlyDictionary<{keyName},{valueName}> {beanNameReadOnly}.{var.NameUpper1} => {var.NamePrivate}ReadOnly;");
            sw.WriteLine();
            */
        }

        public void Visit(Bean type)
        {
            var typeName = TypeName.GetName(type);
            //var typeNameReadOnly = typeName + "ReadOnly";
            //var beanNameReadOnly = TypeName.GetName(var.Bean) + "ReadOnly";
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + "{");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            //sw.WriteLine(prefix + typeNameReadOnly + " " + beanNameReadOnly + "." + var.NameUpper1 + " => " + var.NamePrivate + ";");
        }

        public void Visit(BeanKey type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeDynamic type)
        {
            var typeName = TypeName.GetName(type);
            //var beanNameReadOnly = TypeName.GetName(var.Bean) + "ReadOnly";
            sw.WriteLine($"{prefix}public {typeName} {var.Getter} {{");
            sw.WriteLine($"{prefix}    return {var.NamePrivate};");
            sw.WriteLine($"{prefix}}}");
            //sw.WriteLine($"{prefix}{typeName}ReadOnly {beanNameReadOnly}.{var.NameUpper1} => {var.NameUpper1};");
            /*
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    get");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (!IsManaged)");
            sw.WriteLine(prefix + "            return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);");
            sw.WriteLine(prefix + "        if (txn == null) return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var log = (Log_" + var.NamePrivate + ")txn.GetLog(objectId() + " + var.Id + ");");
            sw.WriteLine(prefix + "        return log != null ? log.Value : " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    private set");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (value == null)");
            sw.WriteLine(prefix + "            throw new System.ArgumentNullException();");
            sw.WriteLine(prefix + "        if (!IsManaged)");
            sw.WriteLine(prefix + "        {");
            sw.WriteLine(prefix + "            " + var.NamePrivate + " = value;");
            sw.WriteLine(prefix + "            return;");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        value.initRootInfo(RootInfo, this);");
            sw.WriteLine(prefix + "        value.variableId(" + var.Id + ");");
            sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);");
            sw.WriteLine(prefix + "        txn.PutLog(new Log_" + var.NamePrivate + "(this, value));"); //
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            */
            sw.WriteLine();
            foreach (Bean real in type.RealBeans.Values)
            {
                string rname = TypeName.GetName(real);
                string pname = "get" + var.NameUpper1 + "_" + real.Space.Path("_", real.Name) + "()";
                sw.WriteLine(prefix + "public " + rname + " " + pname + "{");
                sw.WriteLine(prefix + "    return (" + rname + ")" + var.Getter + ".getBean();");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                sw.WriteLine(prefix + $"public void {var.Setter($"{rname} value")} {{");
                sw.WriteLine(prefix + "    " + var.Getter + ".setBean(value);");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                //sw.WriteLine(prefix + rname + "ReadOnly " + beanNameReadOnly + "." + pname + " => " + pname + ";");
                //sw.WriteLine();
            }
        }

        private void WriteSimpleProperty(Type type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public void " + var.Setter($"{TypeName.GetName(type)} value") + " {");
            sw.WriteLine(prefix + "    " + var.NamePrivate + " = value;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
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
    }
}
