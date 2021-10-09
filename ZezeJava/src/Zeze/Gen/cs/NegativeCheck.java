package Zeze.Gen.cs;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class NegativeCheck implements Types.Visitor {
	private OutputStreamWriter sw;
	private String varname;
	private String prefix;

	public static void Make(Types.Bean bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public override bool NegativeCheck()" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		for (Types.Variable var : bean.getVariables()) {
			if (var.getAllowNegative()) {
				continue;
			}
			var.getVariableType().Accept(new NegativeCheck(sw, var.getNameUpper1(), prefix + "    "));
		}
		sw.write(prefix + "    return false;" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}

	public static void Make(Types.BeanKey bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public bool NegativeCheck()" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		for (Types.Variable var : bean.getVariables()) {
			if (var.getAllowNegative()) {
				continue;
			}
			var.getVariableType().Accept(new NegativeCheck(sw, var.getNameUpper1(), prefix + "    "));
		}
		sw.write(prefix + "    return false;" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}

	private NegativeCheck(OutputStreamWriter sw, String varname, String prefix) {
		this.sw = sw;
		this.varname = varname;
		this.prefix = prefix;
	}

	public final void Visit(Bean type) {
		if (type.isNeedNegativeCheck()) {
			sw.write(prefix + "if (" + varname + ".NegativeCheck()) return true;" + System.lineSeparator());
		}
	}

	public final void Visit(BeanKey type) {
		if (type.isNeedNegativeCheck()) {
			sw.write(prefix + "if (" + varname + ".NegativeCheck()) return true;" + System.lineSeparator());
		}
	}

	public final void Visit(TypeByte type) {
	}

	public final void Visit(TypeDouble type) {
	}

	public final void Visit(TypeInt type) {
		sw.write(prefix + "if (" + varname + " < 0) return true;" + System.lineSeparator());
	}

	public final void Visit(TypeLong type) {
		sw.write(prefix + "if (" + varname + " < 0) return true;" + System.lineSeparator());
	}

	public final void Visit(TypeBool type) {
	}

	public final void Visit(TypeBinary type) {
	}

	public final void Visit(TypeString type) {
	}

	public final void Visit(TypeList type) {
		if (type.isNeedNegativeCheck()) {
			sw.write(prefix + "foreach (var _v_ in " + varname + ")" + System.lineSeparator());
			sw.write(prefix + "{" + System.lineSeparator());
			type.getValueType().Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
			sw.write(prefix + "}" + System.lineSeparator());
		}
	}

	public final void Visit(TypeSet type) {
		if (type.isNeedNegativeCheck()) {
			sw.write(prefix + "foreach (var _v_ in " + varname + ")" + System.lineSeparator());
			sw.write(prefix + "{" + System.lineSeparator());
			type.getValueType().Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
			sw.write(prefix + "}" + System.lineSeparator());
		}
	}

	public final void Visit(TypeMap type) {
		if (type.isNeedNegativeCheck()) {
			sw.write(prefix + "foreach (var _v_ in " + varname + ".Values)" + System.lineSeparator());
			sw.write(prefix + "{" + System.lineSeparator());
			type.getValueType().Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
			sw.write(prefix + "}" + System.lineSeparator());
		}
	}

	public final void Visit(TypeFloat type) {
	}

	public final void Visit(TypeShort type) {
		sw.write(prefix + "if (" + varname + " < 0) return true;" + System.lineSeparator());
	}

	public final void Visit(TypeDynamic type) {
		if (type.isNeedNegativeCheck()) {
			sw.write(prefix + "if (" + varname + ".NegativeCheck()) return true;" + System.lineSeparator());
		}
	}
}