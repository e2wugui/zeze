using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Property : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            foreach (var k in bean.MapKeyTypes)
            {
                // 【注意】{k.Name} see Zeze.Util.Reflect.GetStableName
                var property = $"_zeze_map_key_{k.Name}_";
                sw.WriteLine($"{prefix}public {TypeName.GetName(k)} {property} {{ get; set; }}");
            }
            if (bean.MapKeyTypes.Count > 0)
                sw.WriteLine();
            
            foreach (Variable var in bean.Variables)
            {
                if (bean.Version.Equals(var.Name))
                {
                    // 版本变量生成特殊 private getter、setter，仅给Encode，Decode用。
                    sw.WriteLine($"{prefix}private long {var.NameUpper1}");
                    sw.WriteLine($"{prefix}{{");
                    sw.WriteLine($"{prefix}    get");
                    sw.WriteLine($"{prefix}    {{");
                    sw.WriteLine($"{prefix}        return {var.NamePrivate};");
                    sw.WriteLine($"{prefix}    }}");
                    sw.WriteLine($"{prefix}    set");
                    sw.WriteLine($"{prefix}    {{");
                    sw.WriteLine($"{prefix}        {var.NamePrivate} = value;");
                    sw.WriteLine($"{prefix}    }}");
                    sw.WriteLine($"{prefix}}}");
                    sw.WriteLine();
                    // 重载实现 Bean.Version 接口
                    sw.WriteLine($"{prefix}public override long GetVersion()");
                    sw.WriteLine($"{prefix}{{");
                    sw.WriteLine($"{prefix}    return {var.NamePrivate};");
                    sw.WriteLine($"{prefix}}}");
                    sw.WriteLine();
                    sw.WriteLine($"{prefix}protected override void SetVersion(long newValue)");
                    sw.WriteLine($"{prefix}{{");
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

        public void Visit(Bean type)
        {
            var typeName = TypeName.GetName(type);
            var typeNameReadOnly = typeName + "ReadOnly";
            var beanNameReadOnly = TypeName.GetName(var.Bean) + "ReadOnly";
            sw.WriteLine($"{prefix}public {typeName} {var.NameUpper1}");
            sw.WriteLine($"{prefix}{{");
            sw.WriteLine($"{prefix}    get");
            sw.WriteLine($"{prefix}    {{");
            sw.WriteLine($"{prefix}        return {var.NamePrivate}.Value;");
            sw.WriteLine($"{prefix}    }}");
            sw.WriteLine($"{prefix}    set");
            sw.WriteLine($"{prefix}    {{");
            sw.WriteLine($"{prefix}        {var.NamePrivate}.Value = value;");
            sw.WriteLine($"{prefix}    }}");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine(prefix + typeNameReadOnly + " " + beanNameReadOnly + "." + var.NameUpper1 + " => " + var.NamePrivate + ".Value;");
        }

        void WriteProperty(Type type, bool checkNull = false)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1);
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    get");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (!IsManaged)");
            sw.WriteLine(prefix + "            return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.Current;");
            sw.WriteLine(prefix + "        if (txn == null) return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        txn.VerifyRecordAccessed(this, true);");
            sw.WriteLine(prefix + "        var log = (Log_" + var.NamePrivate + ")txn.GetLog(ObjectId + " + var.Id + ");");
            sw.WriteLine(prefix + "        return log != null ? log.Value : " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    set");
            sw.WriteLine(prefix + "    {");
            if (checkNull)
            {
                sw.WriteLine(prefix + "        if (value == null) throw new System.ArgumentNullException(nameof(value));");
            }
            sw.WriteLine(prefix + "        if (!IsManaged)");
            sw.WriteLine(prefix + "        {");
            sw.WriteLine(prefix + "            " + var.NamePrivate + " = value;");
            sw.WriteLine(prefix + "            return;");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.Current;");
            sw.WriteLine(prefix + "        txn.VerifyRecordAccessed(this);");
            sw.WriteLine(prefix + $"        txn.PutLog(new Log_{var.NamePrivate}() {{ Belong = this, VariableId = {var.Id}, Value = value }});"); // 
            sw.WriteLine(prefix + "    }");
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
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1);
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    get");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (!IsManaged)");
            sw.WriteLine(prefix + "            return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.Current;");
            sw.WriteLine(prefix + "        if (txn == null) return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        txn.VerifyRecordAccessed(this, true);");
            sw.WriteLine(prefix + "        var log = (Log_" + var.NamePrivate + ")txn.GetLog(ObjectId + " + var.Id + ");");
            sw.WriteLine(prefix + "        return log != null ? log.Value : " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    set");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (value == null) throw new System.ArgumentNullException(nameof(value));");
            sw.WriteLine(prefix + "        if (!IsManaged)");
            sw.WriteLine(prefix + "        {");
            sw.WriteLine(prefix + "            " + var.NamePrivate + " = value;");
            sw.WriteLine(prefix + "            return;");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.Current;");
            sw.WriteLine(prefix + "        txn.VerifyRecordAccessed(this);");
            sw.WriteLine(prefix + $"        txn.PutLog(new Log_{var.NamePrivate}() {{ Belong = this, VariableId = {var.Id}, Value = value }});"); // 
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(TypeString type)
        {
            WriteProperty(type, true);
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1 + " => " + var.NamePrivate + ";");
            var valueName = type.ValueType.IsNormalBean
                ? TypeName.GetName(type.ValueType) + "ReadOnly"
                : TypeName.GetName(type.ValueType);
            var beanNameReadOnly = TypeName.GetName(var.Bean) + "ReadOnly";
            sw.WriteLine($"{prefix}System.Collections.Generic.IReadOnlyList<{valueName}> {beanNameReadOnly}.{var.NameUpper1} => {var.NamePrivate};");
            sw.WriteLine();
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1 + " => " + var.NamePrivate + ";");
            var v = TypeName.GetName(type.ValueType);
            var t = $"System.Collections.Generic.IReadOnlySet<{v}>";
            var beanNameReadOnly = TypeName.GetName(var.Bean) + "ReadOnly";
            sw.WriteLine($"{prefix}{t} {beanNameReadOnly}.{var.NameUpper1} => {var.NamePrivate};");
            sw.WriteLine();
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1 + " => " + var.NamePrivate + ";");
            var valueName = type.ValueType.IsNormalBean
                ? TypeName.GetName(type.ValueType) + "ReadOnly"
                : TypeName.GetName(type.ValueType);
            var keyName = TypeName.GetName(type.KeyType);
            var beanNameReadOnly = TypeName.GetName(var.Bean) + "ReadOnly";
            sw.WriteLine($"{prefix}System.Collections.Generic.IReadOnlyDictionary<{keyName},{valueName}> {beanNameReadOnly}.{var.NameUpper1} => {var.NamePrivate}ReadOnly;");
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
            var beanNameReadOnly = TypeName.GetName(var.Bean) + "ReadOnly";
            sw.WriteLine($"{prefix}public {typeName} {var.NameUpper1} => {var.NamePrivate};");
            sw.WriteLine($"{prefix}{typeName}ReadOnly {beanNameReadOnly}.{var.NameUpper1} => {var.NameUpper1};");
            /*
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    get");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (!IsManaged)");
            sw.WriteLine(prefix + "            return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.Current;");
            sw.WriteLine(prefix + "        if (txn == null) return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        txn.VerifyRecordAccessed(this, true);");
            sw.WriteLine(prefix + "        var log = (Log_" + var.NamePrivate + ")txn.GetLog(ObjectId + " + var.Id + ");");
            sw.WriteLine(prefix + "        return log != null ? log.Value : " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    private set");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (value == null)");
            sw.WriteLine(prefix + "            throw new System.ArgumentNullException(nameof(value));");
            sw.WriteLine(prefix + "        if (!IsManaged)");
            sw.WriteLine(prefix + "        {");
            sw.WriteLine(prefix + "            " + var.NamePrivate + " = value;");
            sw.WriteLine(prefix + "            return;");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        value.InitRootInfo(RootInfo, this);");
            sw.WriteLine(prefix + "        value.VariableId = " + var.Id + ";");
            sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.Current;");
            sw.WriteLine(prefix + "        txn.VerifyRecordAccessed(this);");
            sw.WriteLine(prefix + "        txn.PutLog(new Log_" + var.NamePrivate + "(this, value));"); // 
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            */
            sw.WriteLine();
            foreach (Bean real in type.RealBeans.Values)
            {
                string rname = TypeName.GetName(real);
                string pname = var.NameUpper1 + "_" + real.Space.Path("_", real.Name);
                sw.WriteLine(prefix + "public " + rname + " " + pname);
                sw.WriteLine(prefix + "{");
                sw.WriteLine(prefix + "    get { return (" + rname + ")" + var.NameUpper1 + ".Bean; }");
                sw.WriteLine(prefix + "    set { " + var.NameUpper1 + ".Bean = value; }");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
                sw.WriteLine(prefix + rname + "ReadOnly " + beanNameReadOnly + "." + pname + " => " + pname + ";");
                sw.WriteLine();
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
