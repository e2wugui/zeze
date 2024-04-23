using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.rrcs
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
                if (var.Transient)
                {
                    sw.WriteLine($"{prefix}public {TypeName.GetName(var.VariableType)} {var.NameUpper1} {{ get {{ return {var.NamePrivate}; }} set {{ {var.NamePrivate} = value; }} }}");
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
            sw.WriteLine(prefix + "public " + typeName + " " + var.NameUpper1 + " => " + var.NamePrivate + ";");
        }

        void WriteProperty(Type type, bool checkNull = false)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1);
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    get");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (!IsManaged)");
            sw.WriteLine(prefix + "            return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var txn = Zeze.Raft.RocksRaft.Transaction.Current;");
            sw.WriteLine(prefix + "        if (txn == null) return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var log = txn.GetLog(ObjectId + " + var.Id + ");");
            sw.WriteLine(prefix + $"        return log != null ? ((Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>)log).Value : " + var.NamePrivate + ";");
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
            sw.WriteLine(prefix + "        var txn = Zeze.Raft.RocksRaft.Transaction.Current;");
            sw.WriteLine(prefix + $"        txn.PutLog(new Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>() {{ Belong = this, VariableId = {var.Id}, Value = value, }});"); // 
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1);
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    get");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (!IsManaged)");
            sw.WriteLine(prefix + "            return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var txn = Zeze.Raft.RocksRaft.Transaction.Current;");
            sw.WriteLine(prefix + "        if (txn == null) return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var log = txn.GetLog(ObjectId + " + var.Id + ");");
            sw.WriteLine(prefix + $"        return log != null ? ((Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>)log).Value : " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    set");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (value == null)");
            sw.WriteLine(prefix + "            throw new System.ArgumentNullException(nameof(value));");
            sw.WriteLine(prefix + "        if (!IsManaged)");
            sw.WriteLine(prefix + "        {");
            sw.WriteLine(prefix + "            " + var.NamePrivate + " = value;");
            sw.WriteLine(prefix + "            return;");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        var txn = Zeze.Raft.RocksRaft.Transaction.Current;");
            sw.WriteLine(prefix + $"        txn.PutLog(new Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>() {{ Belong = this, VariableId = {var.Id}, Value = value, }});"); // 
            sw.WriteLine(prefix + "    }");
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
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1);
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    get");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (!IsManaged)");
            sw.WriteLine(prefix + "            return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var txn = Zeze.Raft.RocksRaft.Transaction.Current;");
            sw.WriteLine(prefix + "        if (txn == null) return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var log = txn.GetLog(ObjectId + " + var.Id + ");");
            sw.WriteLine(prefix + $"        return log != null ? ((Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>)log).Value : " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    set");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (value == null) throw new System.ArgumentNullException(nameof(value));");
            sw.WriteLine(prefix + "        if (!IsManaged)");
            sw.WriteLine(prefix + "        {");
            sw.WriteLine(prefix + "            " + var.NamePrivate + " = value;");
            sw.WriteLine(prefix + "            return;");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        var txn = Zeze.Raft.RocksRaft.Transaction.Current;");
            sw.WriteLine(prefix + $"        txn.PutLog(new Zeze.Raft.RocksRaft.Log<{TypeName.GetName(type)}>() {{ Belong = this, VariableId = {var.Id}, Value = value, }});"); // 
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
            sw.WriteLine();
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1 + " => " + var.NamePrivate + ";");
            sw.WriteLine();
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1 + " => " + var.NamePrivate + ";");
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
            sw.WriteLine($"{prefix}public {typeName} {var.NameUpper1} => {var.NamePrivate};");
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
            }
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
            WriteProperty(type);
        }
    }
}
