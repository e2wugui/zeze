package Zeze.Gen.cs;

import Zeze.*;
import Zeze.Gen.*;

public class ProtocolFormatter {
	private Zeze.Gen.Protocol p;
	public ProtocolFormatter(Zeze.Gen.Protocol p) {
		this.p = p;
	}

	public final void Make(String baseDir) {
		try (OutputStreamWriter sw = p.getSpace().OpenWriter(baseDir, p.getName() + ".cs", true)) {
    
			sw.write("// auto-generated" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			//sw.WriteLine("using Zeze.Serialize;");
			//sw.WriteLine("using Zeze.Transaction.Collections;");
			sw.write("" + System.lineSeparator());
			sw.write("namespace " + p.getSpace().Path(".", null) + System.lineSeparator());
			sw.write("{" + System.lineSeparator());
    
			String argument = p.getArgumentType() == null ? "Zeze.Transaction.EmptyBean" : TypeName.GetName(p.getArgumentType());
			sw.write("    public sealed class " + p.getName() + " : Zeze.Net.Protocol<" + argument + ">" + System.lineSeparator());
			sw.write("    {" + System.lineSeparator());
			sw.write("        public const int ModuleId_ = " + p.getSpace().getId() + ";" + System.lineSeparator());
			sw.write("        public const int ProtocolId_ = " + p.getId() + ";" + System.lineSeparator());
			sw.write("        public const int TypeId_ = ModuleId_ << 16 | ProtocolId_; " + System.lineSeparator());
			sw.WriteLine();
			sw.write("        public override int ModuleId => ModuleId_;" + System.lineSeparator());
			sw.write("        public override int ProtocolId => ProtocolId_;" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			// declare enums
			for (Types.Enum e : p.getEnums()) {
				sw.write("        public const int " + e.getName() + " = " + e.getValue() + ";" + e.getComment() + System.lineSeparator());
			}
			if (!p.getEnums().isEmpty()) {
				sw.write("" + System.lineSeparator());
			}
			sw.write("        public " + p.getName() + "()" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			/* 现在的bean不是所有的变量都可以赋值，还是先不支持吧。
			if (p.ArgumentType != null)
			{
			    Types.Bean argBean = (Types.Bean)p.ArgumentType;
			    sw.WriteLine("        public " + p.Name + "(" + ParamName.GetParamList(argBean.Variables) + ")");
			    sw.WriteLine("        {");
			    foreach (Types.Variable var in argBean.Variables)
			        sw.WriteLine("            this.Argument." + var.NameUpper1 + " = _" + var.Name + "_;");
			    sw.WriteLine("        }");
			    sw.WriteLine("");
			}
			*/
			sw.write("    }" + System.lineSeparator());
			sw.write("}" + System.lineSeparator());
		}
	}
}