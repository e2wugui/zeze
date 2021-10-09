package Zeze.Gen.ts;

import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class ProtocolFormatter {
	private Zeze.Gen.Protocol p;
	public ProtocolFormatter(Zeze.Gen.Protocol p) {
		this.p = p;
	}

	public final void Make(OutputStreamWriter sw) {
		String argument = p.getArgumentType() == null ? "Zeze.EmptyBean" : TypeName.GetName(p.getArgumentType());
		sw.write("export class " + p.getSpace().Path("_", p.getName()) + " extends Zeze.ProtocolWithArgument<" + argument + "> {" + System.lineSeparator());
		sw.write("    public ModuleId(): number { return " + p.getSpace().getId() + "; }" + System.lineSeparator());
		sw.write("    public ProtocolId(): number { return " + p.getId() + "; }" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
		// declare enums
		for (Types.Enum e : p.getEnums()) {
			sw.write("    public static readonly " + e.getName() + " = " + e.getValue() + ";" + e.getComment() + System.lineSeparator());
		}
		if (!p.getEnums().isEmpty()) {
			sw.write("" + System.lineSeparator());
		}
		sw.write("    public constructor() {" + System.lineSeparator());
		sw.write("        super(new " + argument + "());" + System.lineSeparator());
		sw.write("    }" + System.lineSeparator());
		sw.write("}" + System.lineSeparator());
	}
}