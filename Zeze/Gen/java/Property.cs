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
            {
                if (bean.Version.Equals(var.Name))
                {
                    // 版本变量生成特殊 private getter、setter，仅给Encode，Decode用。
                    sw.WriteLine($"{prefix}long get{var.NameUpper1}() {{");
                    sw.WriteLine($"{prefix}    return {var.NamePrivate};");
                    sw.WriteLine($"{prefix}}}");
                    sw.WriteLine();
                    sw.WriteLine($"{prefix}private void set{var.NameUpper1}(long newValue) {{");
                    sw.WriteLine($"{prefix}    {var.NamePrivate} = newValue;");
                    sw.WriteLine($"{prefix}}}");
                    sw.WriteLine();
                    // 重载实现 Bean.Version 接口
                    sw.WriteLine($"{prefix}@Override");
                    sw.WriteLine($"{prefix}public long version() {{");
                    sw.WriteLine($"{prefix}    return {var.NamePrivate};");
                    sw.WriteLine($"{prefix}}}");
                    sw.WriteLine();
                    sw.WriteLine($"{prefix}@Override");
                    sw.WriteLine($"{prefix}protected void version(long newValue) {{");
                    sw.WriteLine($"{prefix}    {var.NamePrivate} = newValue;");
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

        public static string GetLogName(Type type)
        {
            return LogName.GetName(type);
        }

        void WriteProperty(Type type, bool checkNull = false)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine($"{prefix}@Override");
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    if (!isManaged())");
            sw.WriteLine(prefix + "        return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);");
            sw.WriteLine(prefix + "    if (txn == null)");
            sw.WriteLine(prefix + "        return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    var log = (Log_" + var.NamePrivate + ")txn.getLog(objectId() + " + var.Id + ");");
            sw.WriteLine(prefix + "    return log != null ? log.value : " + var.NamePrivate + ";");
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
            sw.WriteLine(prefix + "    txn.putLog(new Log_" + var.NamePrivate + $"(this, {var.Id}, value));"); //
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

            var v = BoxingName.GetBoxingName(type.ValueType);
            var t = type.ValueType.IsNormalBean ? "PList2ReadOnly" : "PList1ReadOnly";
            var tx = type.ValueType.IsNormalBean ? $"{t}<{v}, {v}ReadOnly>" : $"{t}<{v}>";
            sw.WriteLine($"{prefix}@Override");
            sw.WriteLine($"{prefix}public Zeze.Transaction.Collections.{tx} {var.ReadOnlyGetter} {{");
            sw.WriteLine($"{prefix}    return new Zeze.Transaction.Collections.{t}<>({var.NamePrivate});");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();

            var v = BoxingName.GetBoxingName(type.ValueType);
            var t = $"Zeze.Transaction.Collections.PSet1ReadOnly<{v}>";
            sw.WriteLine($"{prefix}@Override");
            sw.WriteLine($"{prefix}public {t} {var.ReadOnlyGetter} {{");
            sw.WriteLine($"{prefix}    return new Zeze.Transaction.Collections.PSet1ReadOnly<>({var.NamePrivate});");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();

            var k = BoxingName.GetBoxingName(type.KeyType);
            var v = BoxingName.GetBoxingName(type.ValueType);
            var t = type.ValueType.IsNormalBean ? "PMap2ReadOnly" : "PMap1ReadOnly";
            var tx = type.ValueType.IsNormalBean ? $"{t}<{k}, {v}, {v}ReadOnly>" : $"{t}<{k}, {v}>";
            sw.WriteLine($"{prefix}@Override");
            sw.WriteLine($"{prefix}public Zeze.Transaction.Collections.{tx} {var.ReadOnlyGetter} {{");
            sw.WriteLine($"{prefix}    return new Zeze.Transaction.Collections.{t}<>({var.NamePrivate});");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
        }

        public void Visit(Bean type)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + "public " + typeName + " " + var.Getter + " {");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ".getValue();");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public void " + var.Setter($"{typeName} value") + " {");
            sw.WriteLine(prefix + "    " + var.NamePrivate + ".setValue(value);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}@Override");
            sw.WriteLine($"{prefix}public {TypeName.GetName(type)}ReadOnly get{var.NameUpper1}ReadOnly() {{");
            sw.WriteLine($"{prefix}    return {var.NamePrivate}.getValue();");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
        }

        public void Visit(BeanKey type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeDynamic type)
        {
            var typeName = TypeName.GetName(type);
            sw.WriteLine($"{prefix}public {typeName} {var.Getter} {{");
            sw.WriteLine($"{prefix}    return {var.NamePrivate};");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            foreach (Bean real in type.RealBeans.Values)
            {
                string rname = TypeName.GetName(real);
                string pname = "get" + var.NameUpper1 + "_" + real.Space.Path("_", real.Name) + "()";
                sw.WriteLine(prefix + "public " + rname + " " + pname + " {");
                sw.WriteLine(prefix + "    return (" + rname + ")" + var.NamePrivate + ".getBean();");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                sw.WriteLine(prefix + $"public void {var.Setter($"{rname} value")} {{");
                sw.WriteLine(prefix + "    " + var.NamePrivate + ".setBean(value);");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                //sw.WriteLine(prefix + rname + "ReadOnly " + beanNameReadOnly + "." + pname + " => " + pname + ";");
                //sw.WriteLine();
            }
            
            sw.WriteLine($"{prefix}@Override");
            sw.WriteLine($"{prefix}public {TypeName.GetName(type)}ReadOnly get{var.NameUpper1}ReadOnly() {{");
            sw.WriteLine($"{prefix}    return {var.NamePrivate};");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            foreach (Bean real in type.RealBeans.Values)
            {
                string rname = TypeName.GetName(real);
                sw.WriteLine($"{prefix}@Override");
                sw.WriteLine($"{prefix}public {rname}ReadOnly get{var.NameUpper1}_{real.Space.Path("_", real.Name)}ReadOnly() {{");
                sw.WriteLine($"{prefix}    return ({rname}){var.NamePrivate}.getBean();");
                sw.WriteLine($"{prefix}}}");
                sw.WriteLine();
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

        public void Visit(TypeDecimal type)
        {
            WriteProperty(type, true);
        }
    }
}
