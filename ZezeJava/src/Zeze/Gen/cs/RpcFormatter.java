package Zeze.Gen.cs;

import Zeze.*;
import Zeze.Gen.*;

public class RpcFormatter {
	private Zeze.Gen.Rpc rpc;
	public RpcFormatter(Zeze.Gen.Rpc p) {
		this.rpc = p;
	}

	public final void Make(String baseDir) {
		try (OutputStreamWriter sw = rpc.getSpace().OpenWriter(baseDir, rpc.getName() + ".cs", true)) {
    
			sw.write("// auto-generated" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			//sw.WriteLine("using Zeze.Serialize;");
			//sw.WriteLine("using Zeze.Transaction.Collections;");
			sw.write("" + System.lineSeparator());
			sw.write("namespace " + rpc.getSpace().Path(".", null) + System.lineSeparator());
			sw.write("{" + System.lineSeparator());
    
			String argument = rpc.getArgumentType() == null ? "Zeze.Transaction.EmptyBean" : TypeName.GetName(rpc.getArgumentType());
			String result = rpc.getResultType() == null ? "Zeze.Transaction.EmptyBean" : TypeName.GetName(rpc.getResultType());
    
			sw.write("    public sealed class " + rpc.getName() + " : Zeze.Net.Rpc<" + argument + ", " + result + ">" + System.lineSeparator());
			sw.write("    {" + System.lineSeparator());
			sw.write("        public const int ModuleId_ = " + rpc.getSpace().getId() + ";" + System.lineSeparator());
			sw.write("        public const int ProtocolId_ = " + rpc.getId() + ";" + System.lineSeparator());
			sw.write("        public const int TypeId_ = ModuleId_ << 16 | ProtocolId_; " + System.lineSeparator());
			sw.WriteLine();
			sw.write("        public override int ModuleId => ModuleId_;" + System.lineSeparator());
			sw.write("        public override int ProtocolId => ProtocolId_;" + System.lineSeparator());
			// declare enums
			for (Types.Enum e : rpc.getEnums()) {
				sw.write("        public const int " + e.getName() + " = " + e.getValue() + ";" + e.getComment() + System.lineSeparator());
			}
			if (!rpc.getEnums().isEmpty()) {
				sw.write("" + System.lineSeparator());
			}
			sw.write("    }" + System.lineSeparator());
			sw.write("}" + System.lineSeparator());
		}
	}
}