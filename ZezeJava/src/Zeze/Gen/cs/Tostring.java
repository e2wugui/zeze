package Zeze.Gen.cs;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class Tostring implements Types.Visitor {
	private OutputStreamWriter sw;
	private String var;
	private String prefix;
	private String sep;

	public static void Make(Types.Bean bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public override string ToString()" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    System.Text.StringBuilder sb = new System.Text.StringBuilder();" + System.lineSeparator());
		sw.write(prefix + "    BuildString(sb, 0);" + System.lineSeparator());
		sw.write(prefix + "    sb.Append(Environment.NewLine);" + System.lineSeparator());
		sw.write(prefix + "    return sb.ToString();" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.WriteLine();
		sw.write(prefix + "public override void BuildString(System.Text.StringBuilder sb, int level)" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(String.valueOf(String.format("%1$s    sb.Append(new string(' ', level * 4)).Append(\"%2$s: {\").Append(Environment.NewLine);", prefix, bean.getFullName())) + System.lineSeparator());
		sw.write(prefix + "    level++;" + System.lineSeparator());
		for (int i = 0; i < bean.getVariables().size(); ++i) {
			var var = bean.getVariables().get(i);
			var sep = i == bean.getVariables().size() - 1 ? "" : ",";
			var.getVariableType().Accept(new Tostring(sw, var.getNameUpper1(), prefix + "    ", sep));
		}
		sw.write(prefix + "    sb.Append(\"}\");" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}

	public static void Make(Types.BeanKey bean, OutputStreamWriter sw, String prefix) {
		sw.write(prefix + "public override string ToString()" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    System.Text.StringBuilder sb = new System.Text.StringBuilder();" + System.lineSeparator());
		sw.write(prefix + "    BuildString(sb, 0);" + System.lineSeparator());
		sw.write(prefix + "    sb.Append(Environment.NewLine);" + System.lineSeparator());
		sw.write(prefix + "    return sb.ToString();" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.WriteLine();
		sw.write(prefix + "public void BuildString(System.Text.StringBuilder sb, int level)" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(String.valueOf(String.format("%1$s    sb.Append(new string(' ', level * 4)).Append(\"%2$s: {\").Append(Environment.NewLine);", prefix, bean.getFullName())) + System.lineSeparator());
		sw.write(prefix + "    level++;" + System.lineSeparator());
		for (int i = 0; i < bean.getVariables().size(); ++i) {
			var var = bean.getVariables().get(i);
			var sep = i == bean.getVariables().size() - 1 ? "" : ",";
			var.getVariableType().Accept(new Tostring(sw, var.getNameUpper1(), prefix + "    ", sep));
		}
		sw.write(prefix + "    sb.Append(\"}\");" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write("" + System.lineSeparator());
	}

	public Tostring(OutputStreamWriter sw, String var, String prefix, String sep) {
		this.sw = sw;
		this.var = var;
		this.prefix = prefix;
		this.sep = sep;
	}

	public final void Visit(Bean type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=\").Append(Environment.NewLine);", var) + System.lineSeparator());
		sw.write(prefix + var + ".BuildString(sb, level + 1);" + System.lineSeparator());
		sw.write(prefix + String.format("sb.Append(\"%1$s\").Append(Environment.NewLine);", sep) + System.lineSeparator());
	}

	public final void Visit(BeanKey type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=\").Append(Environment.NewLine);", var) + System.lineSeparator());
		sw.write(prefix + var + ".BuildString(sb, level + 1);" + System.lineSeparator());
		sw.write(prefix + String.format("sb.Append(\"%1$s\").Append(Environment.NewLine);", sep) + System.lineSeparator());
	}

	public final void Visit(TypeByte type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=\").Append(%2$s).Append(\"%3$s\").Append(Environment.NewLine);", var, var, sep) + System.lineSeparator());
	}

	public final void Visit(TypeDouble type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=\").Append(%2$s).Append(\"%3$s\").Append(Environment.NewLine);", var, var, sep) + System.lineSeparator());
	}

	public final void Visit(TypeInt type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=\").Append(%2$s).Append(\"%3$s\").Append(Environment.NewLine);", var, var, sep) + System.lineSeparator());
	}

	public final void Visit(TypeLong type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=\").Append(%2$s).Append(\"%3$s\").Append(Environment.NewLine);", var, var, sep) + System.lineSeparator());
	}

	public final void Visit(TypeBool type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=\").Append(%2$s).Append(\"%3$s\").Append(Environment.NewLine);", var, var, sep) + System.lineSeparator());
	}

	public final void Visit(TypeBinary type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=\").Append(%2$s).Append(\"%3$s\").Append(Environment.NewLine);", var, var, sep) + System.lineSeparator());
	}

	public final void Visit(TypeString type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=\").Append(%2$s).Append(\"%3$s\").Append(Environment.NewLine);", var, var, sep) + System.lineSeparator());
	}

	public final void Visit(TypeList type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=[\").Append(Environment.NewLine);", var) + System.lineSeparator());
		sw.write(prefix + "level++;" + System.lineSeparator());
		sw.write(prefix + String.format("foreach (var Item in %1$s)", var) + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		type.getValueType().Accept(new Tostring(sw, "Item", prefix + "    ", ","));
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write(prefix + "level--;" + System.lineSeparator());
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"]%1$s\").Append(Environment.NewLine);", sep) + System.lineSeparator());
	}

	public final void Visit(TypeSet type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=[\").Append(Environment.NewLine);", var) + System.lineSeparator());
		sw.write(prefix + "level++;" + System.lineSeparator());
		sw.write(prefix + String.format("foreach (var Item in %1$s)", var) + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		type.getValueType().Accept(new Tostring(sw, "Item", prefix + "    ", ","));
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write(prefix + "level--;" + System.lineSeparator());
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"]%1$s\").Append(Environment.NewLine);", sep) + System.lineSeparator());
	}

	public final void Visit(TypeMap type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=[\").Append(Environment.NewLine);", var) + System.lineSeparator());
		sw.write(prefix + "level++;" + System.lineSeparator());
		sw.write(prefix + String.format("foreach (var _kv_ in %1$s)", var) + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    sb.Append(\"(\").Append(Environment.NewLine);" + System.lineSeparator());
		sw.write(prefix + "    var Key = _kv_.Key;" + System.lineSeparator());
		type.getKeyType().Accept(new Tostring(sw, "Key", prefix + "    ", ","));
		sw.write(prefix + "    var Value = _kv_.Value;" + System.lineSeparator());
		type.getValueType().Accept(new Tostring(sw, "Value", prefix + "    ", ","));
		sw.write(prefix + "    sb.Append(\")\").Append(Environment.NewLine);" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.write(prefix + "level--;" + System.lineSeparator());
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"]%1$s\").Append(Environment.NewLine);", sep) + System.lineSeparator());
	}

	public final void Visit(TypeFloat type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=\").Append(%2$s).Append(\"%3$s\").Append(Environment.NewLine);", var, var, sep) + System.lineSeparator());
	}

	public final void Visit(TypeShort type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=\").Append(%2$s).Append(\"%3$s\").Append(Environment.NewLine);", var, var, sep) + System.lineSeparator());
	}

	public final void Visit(TypeDynamic type) {
		sw.write(prefix + String.format("sb.Append(new string(' ', level * 4)).Append(\"%1$s\").Append(\"=\").Append(Environment.NewLine);", var) + System.lineSeparator());
		sw.write(prefix + var + ".Bean.BuildString(sb, level + 1);" + System.lineSeparator());
		sw.write(prefix + String.format("sb.Append(\"%1$s\").Append(Environment.NewLine);", sep) + System.lineSeparator());
	}
}