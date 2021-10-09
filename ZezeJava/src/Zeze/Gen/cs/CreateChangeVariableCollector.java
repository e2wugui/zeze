package Zeze.Gen.cs;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class CreateChangeVariableCollector implements Types.Visitor {
	private String ChangeVariableCollectorName;
	public final String getChangeVariableCollectorName() {
		return ChangeVariableCollectorName;
	}
	private void setChangeVariableCollectorName(String value) {
		ChangeVariableCollectorName = value;
	}
	//private Types.Variable var;

	public static void Make(OutputStreamWriter sw, String prefix, Types.Bean bean) {
		sw.write(prefix + "public override Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId)" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    return variableId switch" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		sw.write(prefix + "        0 => new Zeze.Transaction.ChangeVariableCollectorChanged()," + System.lineSeparator());
		for (var v : bean.getVariables()) {
			CreateChangeVariableCollector vistor = new CreateChangeVariableCollector();
			v.getVariableType().Accept(vistor);
			sw.write(prefix + "        " + v.getId() + " => new " + vistor.getChangeVariableCollectorName() + "," + System.lineSeparator());
		}
		sw.write("                _ => null," + System.lineSeparator());
		sw.write("            };" + System.lineSeparator());
		sw.write("        }" + System.lineSeparator());
		sw.WriteLine();
	}

	private CreateChangeVariableCollector() {
	}

	public final void Visit(Bean type) {
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorChanged()");
	}

	public final void Visit(BeanKey type) {
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorChanged()");
	}

	public final void Visit(TypeByte type) {
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorChanged()");
	}

	public final void Visit(TypeShort type) {
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorChanged()");
	}

	public final void Visit(TypeInt type) {
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorChanged()");
	}

	public final void Visit(TypeLong type) {
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorChanged()");
	}

	public final void Visit(TypeBool type) {
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorChanged()");
	}

	public final void Visit(TypeBinary type) {
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorChanged()");
	}

	public final void Visit(TypeString type) {
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorChanged()");
	}

	public final void Visit(TypeFloat type) {
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorChanged()");
	}

	public final void Visit(TypeDouble type) {
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorChanged()");
	}

	public final void Visit(TypeList type) {
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorChanged()");
	}

	public final void Visit(TypeSet type) {
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorSet()");
	}

	public final void Visit(TypeMap type) {
		String kv = TypeName.GetName(type.getKeyType()) + ", " + TypeName.GetName(type.getValueType());
		String factory = type.getValueType().isNormalBean() ? "() => new Zeze.Transaction.ChangeNoteMap2<" + kv + ">(null)" : "() => new Zeze.Transaction.ChangeNoteMap1<" + kv + ">(null)";
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorMap(" + factory + ")");
	}

	public final void Visit(TypeDynamic type) {
		setChangeVariableCollectorName("Zeze.Transaction.ChangeVariableCollectorChanged()");
	}
}