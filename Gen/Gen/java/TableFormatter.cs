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
            sw.WriteLine("    public " + table.Name + "(String _s_) {");
            sw.WriteLine($"        super({table.Id}, \"{table.Space.Path("_", table.Name)}\", _s_);");
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
                sw.WriteLine("    public long insert(" + value + " _v_) {");
                sw.WriteLine("        //noinspection DataFlowIssue");
                sw.WriteLine("        long _k_ = getAutoKey().next();");
                sw.WriteLine("        insert(_k_, _v_);");
                sw.WriteLine("        return _k_;");
                sw.WriteLine("    }");
                sw.WriteLine();
            }
            if (table.IsAutoKeyRandom)
            {
                sw.WriteLine("    public Zeze.Net.Binary insert(" + value + " _v_) {");
                sw.WriteLine("        var _k_ = Zeze.Util.Random.nextBinary(16);");
                sw.WriteLine("        while (!tryAdd(_k_, _v_))");
                sw.WriteLine("            _k_ = Zeze.Util.Random.nextBinary(16);");
                sw.WriteLine("        return _k_;");
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
            sw.WriteLine("    public " + keyboxing + " decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {");
            if (table.KeyType.IsBean)
                sw.WriteLine("        var _p_ = new java.util.ArrayList<String>();");
            var hasParentName = new bool[1];
            table.KeyType.Accept(new Define("_v_", sw, "        "));
            table.KeyType.Accept(new DecodeResultSet("__key", "_v_", -1, "_s_", sw, "        ", hasParentName, true));
            sw.WriteLine("        return _v_;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, " + keyboxing + " _v_) {");
            if (table.KeyType.IsBean)
                sw.WriteLine("        var _p_ = new java.util.ArrayList<String>();");
            var hasParentName2 = new bool[1];
            table.KeyType.Accept(new EncodeSQLStatement("__key", null, "_v_", -1, "_s_", sw, "        ", hasParentName2, true));
            sw.WriteLine("    }");
            sw.WriteLine();

            sw.WriteLine("    @Override");
            sw.WriteLine($"    public {value} newValue() {{");
            sw.WriteLine($"        return new {value}();");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine($"    public {value}ReadOnly getReadOnly({keyboxing} _k_) {{");
            sw.WriteLine($"        return get(_k_);");
            sw.WriteLine("    }");
            //sw.WriteLine();
            //CreateChangeVariableCollector.Make(sw, "    ", (Types.Bean)table.ValueType);
            sw.WriteLine("}");
        }
    }
}
