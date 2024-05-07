using System.IO;
using Zeze.Gen.Types;

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
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + table.Space.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("import Zeze.Serialize.ByteBuffer;");
            sw.WriteLine("import Zeze.Transaction.TableX;");
            sw.WriteLine("import Zeze.Transaction.TableReadOnly;");
            sw.WriteLine();
            string value = TypeName.GetName(table.ValueType);
            string keyboxing = BoxingName.GetBoxingName(table.KeyType);
            if (table.Comment.Length > 0)
                sw.WriteLine(table.Comment);
            sw.WriteLine("@SuppressWarnings({\"DuplicateBranchesInSwitch\", \"NullableProblems\", \"RedundantSuppression\"})");
            sw.WriteLine("public final class " + table.Name + $" extends TableX<{keyboxing}, {value}>");
            sw.WriteLine($"        implements TableReadOnly<{keyboxing}, {value}, {value}ReadOnly> {{");
            sw.WriteLine("    public " + table.Name + "() {");
            sw.WriteLine($"        super({table.Id}, \"{table.Space.Path("_", table.Name)}\");");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public " + table.Name + "(String suffix) {");
            sw.WriteLine($"        super({table.Id}, \"{table.Space.Path("_", table.Name)}\", suffix);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine($"    public Class<{BoxingName.GetBoxingName(table.KeyType)}> getKeyClass() {{");
            sw.WriteLine($"        return {BoxingName.GetBoxingName(table.KeyType)}.class;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine($"    public Class<{TypeName.GetName(table.ValueType)}> getValueClass() {{");
            sw.WriteLine($"        return {TypeName.GetName(table.ValueType)}.class;");
            sw.WriteLine("    }");
            if (table.IsMemory) // 需要保证基类返回false
            {
                sw.WriteLine();
                sw.WriteLine("    @Override");
                sw.WriteLine("    public boolean isMemory() {");
                sw.WriteLine("        return true;");
                sw.WriteLine("    }");
            }
            if (table.IsAutoKey) // 需要保证基类返回false
            {
                sw.WriteLine();
                sw.WriteLine("    @Override");
                sw.WriteLine("    public boolean isAutoKey() {");
                sw.WriteLine("        return true;");
                sw.WriteLine("    }");
            }
            if (table.IsRelationalMapping) // 需要保证基类返回false
            {
                sw.WriteLine();
                sw.WriteLine("    @Override");
                sw.WriteLine("    public boolean isRelationalMapping() {");
                sw.WriteLine("        return true;");
                sw.WriteLine("    }");
            }
            sw.WriteLine();
            foreach (var v in ((Types.Bean)table.ValueType).Variables)
                sw.WriteLine("    public static final int VAR_" + v.Name + " = " + v.Id + ";");
            sw.WriteLine();
            if (table.IsAutoKey)
            {
                sw.WriteLine("    public long insert(" + value + " value) {");
                sw.WriteLine("        //noinspection DataFlowIssue");
                sw.WriteLine("        long key = getAutoKey().next();");
                sw.WriteLine("        insert(key, value);");
                sw.WriteLine("        return key;");
                sw.WriteLine("    }");
                sw.WriteLine();
            }
            if (table.IsAutoKeyRandom)
            {
                sw.WriteLine("    public Zeze.Net.Binary insert(" + value + " value) {");
                sw.WriteLine("        var key = Zeze.Util.Random.nextBinary(16);");
                sw.WriteLine("        while (!tryAdd(key, value))");
                sw.WriteLine("            key = Zeze.Util.Random.nextBinary(16);");
                sw.WriteLine("        return key;");
                sw.WriteLine("    }");
                sw.WriteLine();
            }
            sw.WriteLine("    @Override");
            sw.WriteLine("    public " + keyboxing + " decodeKey(ByteBuffer _os_) {");
            table.KeyType.Accept(new Define("_v_", sw, "        "));
            table.KeyType.Accept(new Decode("_v_", -1, "_os_", sw, "        ", false));
            sw.WriteLine("        return _v_;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public ByteBuffer encodeKey(" + keyboxing + " _v_) {");
            sw.WriteLine(table.KeyType is TypeLong or TypeInt
                ? "        ByteBuffer _os_ = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(_v_));"
                : "        ByteBuffer _os_ = ByteBuffer.Allocate(16);");
            table.KeyType.Accept(new Encode(null, "_v_", -1, "_os_", sw, "        ", true));
            sw.WriteLine("        return _os_;");
            sw.WriteLine("    }");
            sw.WriteLine();

            sw.WriteLine("    @Override");
            sw.WriteLine("    public " + keyboxing + " decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {");
            if (table.KeyType.IsBean)
                sw.WriteLine("        var parents = new java.util.ArrayList<String>();");
            var hasParentName = new bool[1];
            table.KeyType.Accept(new Define("_v_", sw, "        "));
            table.KeyType.Accept(new DecodeResultSet("__key", "_v_", -1, "rs", sw, "        ", hasParentName, true));
            sw.WriteLine("        return _v_;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, " + keyboxing + " _v_) {");
            if (table.KeyType.IsBean)
                sw.WriteLine("        var parents = new java.util.ArrayList<String>();");
            var hasParentName2 = new bool[1];
            table.KeyType.Accept(new EncodeSQLStatement("__key", null, "_v_", -1, "st", sw, "        ", hasParentName2, true));
            sw.WriteLine("    }");
            sw.WriteLine();

            sw.WriteLine("    @Override");
            sw.WriteLine($"    public {value} newValue() {{");
            sw.WriteLine($"        return new {value}();");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine($"    public {value}ReadOnly getReadOnly({keyboxing} key) {{");
            sw.WriteLine($"        return get(key);");
            sw.WriteLine("    }");
            //sw.WriteLine();
            //CreateChangeVariableCollector.Make(sw, "    ", (Types.Bean)table.ValueType);
            sw.WriteLine("}");
        }
    }
}
