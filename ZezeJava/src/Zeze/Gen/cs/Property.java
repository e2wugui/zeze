package Zeze.Gen.cs;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class Property implements Types.Visitor {
	private OutputStreamWriter sw;
	private Types.Variable var;
	private String prefix;

	public static void Make(Types.Bean bean, OutputStreamWriter sw, String prefix) {
		for (Types.Variable var : bean.getVariables()) {
			var.getVariableType().Accept(new Property(sw, var, prefix));
		}
	}

	public Property(OutputStreamWriter sw, Types.Variable var, String prefix) {
		this.sw = sw;
		this.var = var;
		this.prefix = prefix;
	}

	public final void Visit(Bean type) {
		var typeName = TypeName.GetName(type);
		var typeNameReadOnly = typeName + "ReadOnly";
		var beanNameReadOnly = TypeName.GetName(var.getBean()) + "ReadOnly";
		sw.write(prefix + "public " + typeName + " " + var.getNameUpper1() + " => " + var.getNamePrivate() + ";" + System.lineSeparator());
		sw.write(prefix + typeNameReadOnly + " " + beanNameReadOnly + "." + var.getNameUpper1() + " => " + var.getNamePrivate() + ";" + System.lineSeparator());
	}


	private void WriteProperty(Types.Type type) {
		WriteProperty(type, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: private void WriteProperty(Types.Type type, bool checkNull = false)
	private void WriteProperty(Types.Type type, boolean checkNull) {
		sw.write(prefix + "public " + TypeName.GetName(type) + " " + var.getNameUpper1() + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    get" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		sw.write(prefix + "        if (false == this.IsManaged)" + System.lineSeparator());
		sw.write(prefix + "            return " + var.getNamePrivate() + ";" + System.lineSeparator());
		sw.write(prefix + "        var txn = Zeze.Transaction.Transaction.Current;" + System.lineSeparator());
		sw.write(prefix + "        if (txn == null) return " + var.getNamePrivate() + ";" + System.lineSeparator());
		sw.write(prefix + "        txn.VerifyRecordAccessed(this, true);" + System.lineSeparator());
		sw.write(prefix + "        var log = (Log_" + var.getNamePrivate() + ")txn.GetLog(this.ObjectId + " + var.getId() + ");" + System.lineSeparator());
		sw.write(prefix + "        return log != null ? log.Value : " + var.getNamePrivate() + ";" + System.lineSeparator());
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "    set" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		if (checkNull) {
			sw.write(prefix + "        if (null == value) throw new System.ArgumentNullException();" + System.lineSeparator());
		}
		sw.write(prefix + "        if (false == this.IsManaged)" + System.lineSeparator());
		sw.write(prefix + "        {" + System.lineSeparator());
		sw.write(prefix + "            " + var.getNamePrivate() + " = value;" + System.lineSeparator());
		sw.write(prefix + "            return;" + System.lineSeparator());
		sw.write(prefix + "        }" + System.lineSeparator());
		sw.write(prefix + "        var txn = Zeze.Transaction.Transaction.Current;" + System.lineSeparator());
		sw.write(prefix + "        txn.VerifyRecordAccessed(this);" + System.lineSeparator());
		sw.write(prefix + "        txn.PutLog(new Log_" + var.getNamePrivate() + "(this, value));" + System.lineSeparator());
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.WriteLine();
	}

	public final void Visit(BeanKey type) {
		sw.write(prefix + "public " + TypeName.GetName(type) + " " + var.getNameUpper1() + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    get" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		sw.write(prefix + "        if (false == this.IsManaged)" + System.lineSeparator());
		sw.write(prefix + "            return " + var.getNamePrivate() + ";" + System.lineSeparator());
		sw.write(prefix + "        var txn = Zeze.Transaction.Transaction.Current;" + System.lineSeparator());
		sw.write(prefix + "        if (txn == null) return " + var.getNamePrivate() + ";" + System.lineSeparator());
		sw.write(prefix + "        txn.VerifyRecordAccessed(this, true);" + System.lineSeparator());
		sw.write(prefix + "        var log = (Log_" + var.getNamePrivate() + ")txn.GetLog(this.ObjectId + " + var.getId() + ");" + System.lineSeparator());
		sw.write(prefix + "        return log != null ? log.Value : " + var.getNamePrivate() + ";" + System.lineSeparator());
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "    set" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		sw.write(prefix + "        if (null == value)" + System.lineSeparator());
		sw.write(prefix + "            throw new System.ArgumentNullException();" + System.lineSeparator());
		sw.write(prefix + "        if (false == this.IsManaged)" + System.lineSeparator());
		sw.write(prefix + "        {" + System.lineSeparator());
		sw.write(prefix + "            " + var.getNamePrivate() + " = value;" + System.lineSeparator());
		sw.write(prefix + "            return;" + System.lineSeparator());
		sw.write(prefix + "        }" + System.lineSeparator());
		sw.write(prefix + "        var txn = Zeze.Transaction.Transaction.Current;" + System.lineSeparator());
		sw.write(prefix + "        txn.VerifyRecordAccessed(this);" + System.lineSeparator());
		sw.write(prefix + "        txn.PutLog(new Log_" + var.getNamePrivate() + "(this, value));" + System.lineSeparator());
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.WriteLine();
	}

	public final void Visit(TypeByte type) {
		WriteProperty(type);
	}

	public final void Visit(TypeDouble type) {
		WriteProperty(type);
	}

	public final void Visit(TypeInt type) {
		WriteProperty(type);
	}

	public final void Visit(TypeLong type) {
		WriteProperty(type);
	}

	public final void Visit(TypeBool type) {
		WriteProperty(type);
	}

	public final void Visit(TypeBinary type) {
		sw.write(prefix + "public " + TypeName.GetName(type) + " " + var.getNameUpper1() + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    get" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		sw.write(prefix + "        if (false == this.IsManaged)" + System.lineSeparator());
		sw.write(prefix + "            return " + var.getNamePrivate() + ";" + System.lineSeparator());
		sw.write(prefix + "        var txn = Zeze.Transaction.Transaction.Current;" + System.lineSeparator());
		sw.write(prefix + "        if (txn == null) return " + var.getNamePrivate() + ";" + System.lineSeparator());
		sw.write(prefix + "        txn.VerifyRecordAccessed(this, true);" + System.lineSeparator());
		sw.write(prefix + "        var log = (Log_" + var.getNamePrivate() + ")txn.GetLog(this.ObjectId + " + var.getId() + ");" + System.lineSeparator());
		sw.write(prefix + "        return log != null ? log.Value : " + var.getNamePrivate() + ";" + System.lineSeparator());
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "    set" + System.lineSeparator());
		sw.write(prefix + "    {" + System.lineSeparator());
		sw.write(prefix + "        if (null == value) throw new System.ArgumentNullException();" + System.lineSeparator());
		sw.write(prefix + "        if (false == this.IsManaged)" + System.lineSeparator());
		sw.write(prefix + "        {" + System.lineSeparator());
		sw.write(prefix + "            " + var.getNamePrivate() + " = value;" + System.lineSeparator());
		sw.write(prefix + "            return;" + System.lineSeparator());
		sw.write(prefix + "        }" + System.lineSeparator());
		sw.write(prefix + "        var txn = Zeze.Transaction.Transaction.Current;" + System.lineSeparator());
		sw.write(prefix + "        txn.VerifyRecordAccessed(this);" + System.lineSeparator());
		sw.write(prefix + "        txn.PutLog(new Log_" + var.getNamePrivate() + "(this, value));" + System.lineSeparator());
		sw.write(prefix + "    }" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
		sw.WriteLine();
	}

	public final void Visit(TypeString type) {
		WriteProperty(type, true);
	}

	public final void Visit(TypeList type) {
		sw.write(prefix + "public " + TypeName.GetName(type) + " " + var.getNameUpper1() + " => " + var.getNamePrivate() + ";" + System.lineSeparator());
		var valueName = type.getValueType().isNormalBean() ? TypeName.GetName(type.getValueType()) + "ReadOnly" : TypeName.GetName(type.getValueType());
		var beanNameReadOnly = TypeName.GetName(var.getBean()) + "ReadOnly";
		sw.write(String.valueOf(String.format("%1$sSystem.Collections.Generic.IReadOnlyList<%2$s> %3$s.%4$s => %5$s;", prefix, valueName, beanNameReadOnly, var.getNameUpper1(), var.getNamePrivate())) + System.lineSeparator());
		sw.WriteLine();
	}

	public final void Visit(TypeSet type) {
		sw.write(prefix + "public " + TypeName.GetName(type) + " " + var.getNameUpper1() + " => " + var.getNamePrivate() + ";" + System.lineSeparator());
		var v = TypeName.GetName(type.getValueType());
		var t = String.format("System.Collections.Generic.IReadOnlySet<%1$s>", v);
		var beanNameReadOnly = TypeName.GetName(var.getBean()) + "ReadOnly";
		sw.write(String.valueOf(String.format("%1$s%2$s %3$s.%4$s => %5$s;", prefix, t, beanNameReadOnly, var.getNameUpper1(), var.getNamePrivate())) + System.lineSeparator());
		sw.WriteLine();
	}

	public final void Visit(TypeMap type) {
		sw.write(prefix + "public " + TypeName.GetName(type) + " " + var.getNameUpper1() + " => " + var.getNamePrivate() + ";" + System.lineSeparator());
		var valueName = type.getValueType().isNormalBean() ? TypeName.GetName(type.getValueType()) + "ReadOnly" : TypeName.GetName(type.getValueType());
		var keyName = TypeName.GetName(type.getKeyType());
		var beanNameReadOnly = TypeName.GetName(var.getBean()) + "ReadOnly";
		sw.write(String.valueOf(String.format("%1$sSystem.Collections.Generic.IReadOnlyDictionary<%2$s,%3$s> %4$s.%5$s => %6$sReadOnly;", prefix, keyName, valueName, beanNameReadOnly, var.getNameUpper1(), var.getNamePrivate())) + System.lineSeparator());
		sw.WriteLine();
	}

	public final void Visit(TypeFloat type) {
		WriteProperty(type);
	}

	public final void Visit(TypeShort type) {
		WriteProperty(type);
	}

	public final void Visit(TypeDynamic type) {
		var typeName = TypeName.GetName(type);
		var beanNameReadOnly = TypeName.GetName(var.getBean()) + "ReadOnly";
		sw.write(String.format("%1$spublic %2$s %3$s => %4$s;", prefix, typeName, var.getNameUpper1(), var.getNamePrivate()) + System.lineSeparator());
		sw.write(String.valueOf(String.format("%1$s%2$sReadOnly %3$s.%4$s => %5$s;", prefix, typeName, beanNameReadOnly, var.getNameUpper1(), var.getNameUpper1())) + System.lineSeparator());
		/*
		sw.WriteLine(prefix + "{");
		sw.WriteLine(prefix + "    get");
		sw.WriteLine(prefix + "    {");
		sw.WriteLine(prefix + "        if (false == this.IsManaged)");
		sw.WriteLine(prefix + "            return " + var.NamePrivate + ";");
		sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.Current;");
		sw.WriteLine(prefix + "        if (txn == null) return " + var.NamePrivate + ";");
		sw.WriteLine(prefix + "        txn.VerifyRecordAccessed(this, true);");
		sw.WriteLine(prefix + "        var log = (Log_" + var.NamePrivate + ")txn.GetLog(this.ObjectId + " + var.Id + ");");
		sw.WriteLine(prefix + "        return log != null ? log.Value : " + var.NamePrivate + ";");
		sw.WriteLine(prefix + "    }");
		sw.WriteLine(prefix + "    private set");
		sw.WriteLine(prefix + "    {");
		sw.WriteLine(prefix + "        if (null == value)");
		sw.WriteLine(prefix + "            throw new System.ArgumentNullException();");
		sw.WriteLine(prefix + "        if (false == this.IsManaged)");
		sw.WriteLine(prefix + "        {");
		sw.WriteLine(prefix + "            " + var.NamePrivate + " = value;");
		sw.WriteLine(prefix + "            return;");
		sw.WriteLine(prefix + "        }");
		sw.WriteLine(prefix + "        value.InitRootInfo(RootInfo, this);");
		sw.WriteLine(prefix + "        value.VariableId = " + var.Id + ";");
		sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.Current;");
		sw.WriteLine(prefix + "        txn.VerifyRecordAccessed(this);");
		sw.WriteLine(prefix + "        txn.PutLog(new Log_" + var.NamePrivate + "(this, value));"); // 
		sw.WriteLine(prefix + "    }");
		sw.WriteLine(prefix + "}");
		*/
		sw.WriteLine();
		for (Bean real : type.getRealBeans().values()) {
			String rname = TypeName.GetName(real);
			String pname = var.getNameUpper1() + "_" + real.getSpace().Path("_", real.getName());
			sw.write(prefix + "public " + rname + " " + pname + System.lineSeparator());
			sw.write(prefix + "{" + System.lineSeparator());
			sw.write(prefix + "    get { return (" + rname + ")" + var.getNameUpper1() + ".Bean; }" + System.lineSeparator());
			sw.write(prefix + "    set { " + var.getNameUpper1() + ".Bean = value; }" + System.lineSeparator());
			sw.write(prefix + "}" + System.lineSeparator());
			sw.WriteLine();
			sw.write(prefix + rname + "ReadOnly " + beanNameReadOnly + "." + pname + " => " + pname + ";" + System.lineSeparator());
			sw.WriteLine();
		}
	}
}