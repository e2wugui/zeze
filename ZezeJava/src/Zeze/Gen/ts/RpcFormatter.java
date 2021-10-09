package Zeze.Gen.ts;

import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class RpcFormatter {
	private Zeze.Gen.Rpc rpc;
	public RpcFormatter(Zeze.Gen.Rpc p) {
		this.rpc = p;
	}

	public final void Make(OutputStreamWriter sw) {
		String argument = rpc.getArgumentType() == null ? "Zeze.EmptyBean" : TypeName.GetName(rpc.getArgumentType());
		String result = rpc.getResultType() == null ? "Zeze.EmptyBean" : TypeName.GetName(rpc.getResultType());

		sw.write("export class " + rpc.getSpace().Path("_", rpc.getName()) + " extends Zeze.Rpc<" + argument + ", " + result + "> {" + System.lineSeparator());
		sw.write("    public ModuleId(): number { return " + rpc.getSpace().getId() + "; }" + System.lineSeparator());
		sw.write("    public ProtocolId(): number { return " + rpc.getId() + "; }" + System.lineSeparator());
		// declare enums
		for (Types.Enum e : rpc.getEnums()) {
			sw.write("    public static readonly " + e.getName() + " = " + e.getValue() + ";" + e.getComment() + System.lineSeparator());
		}
		if (!rpc.getEnums().isEmpty()) {
			sw.write("" + System.lineSeparator());
		}
		sw.write("    public constructor() {" + System.lineSeparator());
		sw.write("        super(new " + argument + "(), new " + result + "());" + System.lineSeparator());
		sw.write("    }" + System.lineSeparator());
		sw.write("}" + System.lineSeparator());
	}
}