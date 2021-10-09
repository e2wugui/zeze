package Zeze.Gen.lua;

import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class ProtocolFormatter {
	public static void Make(String moduleName, Protocol p, OutputStreamWriter sw) {
		sw.write(String.valueOf(String.format("%1$s.%2$s = {", moduleName, p.getName())) + System.lineSeparator());
		sw.write(String.format("    TypeId = %1$s,", p.getTypeId()) + System.lineSeparator());
		sw.write(String.format("    ModuleId = %1$s,", p.getSpace().getId()) + System.lineSeparator());
		sw.write(String.format("    ProtocolId = %1$s,", p.getId()) + System.lineSeparator());
		sw.write(String.format("    ResultCode = 0,") + System.lineSeparator());
		sw.write(String.format("    Argument = {{}},") + System.lineSeparator());
		if (p instanceof Rpc) {
			sw.write(String.format("    Result = {{}},") + System.lineSeparator());
		}

		sw.write("}" + System.lineSeparator());
	}
}