using System.IO;

namespace Zeze.Gen.java
{
    public class TableFormatter
    {
        readonly Table table;
        readonly string genDir;

        public TableFormatter(Table table, string genDir)
        {
            this.table = table;
            this.genDir = genDir;
        }

        public void Make()
        {
            using StreamWriter sw = table.Space.OpenWriter(genDir, table.Name + ".java");

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + table.Space.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("import Zeze.Serialize.ByteBuffer;");
            sw.WriteLine();
            string key = TypeName.GetName(table.KeyType);
            string value = TypeName.GetName(table.ValueType);
            string keyboxing = BoxingName.GetBoxingName(table.KeyType);
            sw.WriteLine("public final class " + table.Name + " extends Zeze.Transaction.TableX<" + keyboxing + ", " + value + "> {");
            sw.WriteLine("    public " + table.Name + "() {");
            sw.WriteLine("        super(\"" + table.Space.Path("_", table.Name) + "\");");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public boolean isMemory() {");
            sw.WriteLine("        return " + (table.IsMemory ? "true;" : "false;"));
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public boolean isAutoKey() {");
            sw.WriteLine("        return " + (table.IsAutoKey ? "true;" : "false;"));
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public static final int VAR_All = 0;");
            foreach (var v in ((Types.Bean)table.ValueType).Variables)
                sw.WriteLine("    public static final int VAR_" + v.Name + " = " + v.Id + ";");
            sw.WriteLine();
            if (table.IsAutoKey)
            {
                sw.WriteLine("    public long insert(" + value + " value) {");
                sw.WriteLine("        long key = getAutoKey().Next();");
                sw.WriteLine("        insert(key, value);");
                sw.WriteLine("        return key;");
                sw.WriteLine("    }");
                sw.WriteLine();
            }
            sw.WriteLine("    @Override");
            sw.WriteLine("    public " + keyboxing + " DecodeKey(ByteBuffer _os_) {");
            table.KeyType.Accept(new Define("_v_", sw, "        "));
            table.KeyType.Accept(new Decode("_v_", -1, "_os_", sw, "        "));
            sw.WriteLine("        return _v_;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public ByteBuffer EncodeKey(" + keyboxing + " _v_) {");
            sw.WriteLine("        ByteBuffer _os_ = ByteBuffer.Allocate(16);");
            table.KeyType.Accept(new Encode("_v_", -1, "_os_", sw, "        "));
            sw.WriteLine("        return _os_;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine($"    public {value} NewValue() {{");
            sw.WriteLine($"        return new {value}();");
            sw.WriteLine("    }");
            sw.WriteLine();
            CreateChangeVariableCollector.Make(sw, "    ", (Types.Bean)table.ValueType);
            sw.WriteLine("}");
        }
    }
}
