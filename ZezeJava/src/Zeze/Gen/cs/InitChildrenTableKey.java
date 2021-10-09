package Zeze.Gen.cs;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class InitChildrenTableKey {
	public static void Make(Types.Bean bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "protected override void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root)" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		for (Types.Variable v : bean.getVariables()) {
			if (v.getVariableType().isNormalBean() || v.getVariableType().isCollection()) {
				sw.write(prefix + "    " + v.getNamePrivate() + ".InitRootInfo(root, this);" + System.lineSeparator());
			}
			else if (v.getVariableType() instanceof TypeDynamic) {
				sw.write(prefix + "    " + v.getNamePrivate() + ".InitRootInfo(root, this);" + System.lineSeparator());
			}
		}
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}
}