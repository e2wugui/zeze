package Zeze.Gen.cs;

import Zeze.Serialize.*;
import Zeze.*;
import Zeze.Gen.*;

public class TableFormatter {
	private final Table table;
	private final String genDir;

	public TableFormatter(Table table, String genDir) {
		this.table = table;
		this.genDir = genDir;
	}

	public final void Make() {
		try (OutputStreamWriter sw = table.getSpace().OpenWriter(genDir, table.getName() + ".cs", true)) {
    
			sw.write("// auto-generated" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("using Zeze.Serialize;" + System.lineSeparator());
			//sw.WriteLine("using Zeze.Transaction.Collections;");
			sw.write("" + System.lineSeparator());
			sw.write("namespace " + table.getSpace().Path(".", null) + System.lineSeparator());
			sw.write("{" + System.lineSeparator());
			String key = TypeName.GetName(table.getKeyType());
			String value = TypeName.GetName(table.getValueType());
			sw.write("    public sealed class " + table.getName() + " : Zeze.Transaction.Table<" + key + ", " + value + ">" + System.lineSeparator());
			sw.write("    {" + System.lineSeparator());
			sw.write("        public " + table.getName() + "() : base(\"" + table.getSpace().Path("_", table.getName()) + "\")" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.WriteLine();
			sw.write("        public override bool IsMemory => " + (table.isMemory() ? "true;" : "false;") + System.lineSeparator());
			sw.write("        public override bool IsAutoKey => " + (table.isAutoKey() ? "true;" : "false;") + System.lineSeparator());
			sw.WriteLine();
			sw.write("        public const int VAR_All = 0;" + System.lineSeparator());
			for (var v : ((Types.Bean)table.getValueType()).getVariables()) {
				sw.write("        public const int VAR_" + v.getName() + " = " + v.getId() + ";" + System.lineSeparator());
			}
			sw.WriteLine();
			if (table.isAutoKey()) {
				sw.write("        public long Insert(" + value + " value)" + System.lineSeparator());
				sw.write("        {" + System.lineSeparator());
				sw.write("            long key = AutoKey.Next();" + System.lineSeparator());
				sw.write("            Insert(key, value);" + System.lineSeparator());
				sw.write("            return key;" + System.lineSeparator());
				sw.write("        }" + System.lineSeparator());
				sw.WriteLine();
			}
			sw.write("        public override " + TypeName.GetName(table.getKeyType()) + " DecodeKey(ByteBuffer _os_)" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			table.getKeyType().Accept(new Define("_v_", sw, "            "));
			table.getKeyType().Accept(new Decode("_v_", -1, "_os_", sw, "            "));
			sw.write("            return _v_;" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.WriteLine();
			sw.write("        public override ByteBuffer EncodeKey(" + TypeName.GetName(table.getKeyType()) + " _v_)" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("            ByteBuffer _os_ = ByteBuffer.Allocate();" + System.lineSeparator());
			table.getKeyType().Accept(new Encode("_v_", -1, "_os_", sw, "            "));
			sw.write("            return _os_;" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.WriteLine();
			CreateChangeVariableCollector.Make(sw, "        ", (Types.Bean)table.getValueType());
			sw.WriteLine();
			sw.write("    }" + System.lineSeparator());
			sw.write("}" + System.lineSeparator());
    
		}
	}
}