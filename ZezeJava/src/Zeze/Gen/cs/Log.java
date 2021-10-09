package Zeze.Gen.cs;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class Log implements Types.Visitor {
	private OutputStreamWriter sw;
	private Types.Bean bean;
	private Types.Variable var;
	private String prefix;

	public static void Make(Types.Bean bean, OutputStreamWriter sw, String prefix) {
		for (Types.Variable var : bean.getVariables()) {
			var.getVariableType().Accept(new Log(bean, sw, var, prefix));
			sw.write("" + System.lineSeparator());
		}
	}

	public Log(Types.Bean bean, OutputStreamWriter sw, Types.Variable var, String prefix) {
		this.bean = bean;
		this.sw = sw;
		this.var = var;
		this.prefix = prefix;
	}

	public final void Visit(Bean type) {
	}

	private void WriteLogValue(Types.Type type) {
		String valueName = TypeName.GetName(type);
		sw.write(prefix + "private sealed class Log_" + var.getNamePrivate() + " : Zeze.Transaction.Log<" + bean.getName() + ", " + valueName + ">" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    public Log_" + var.getNamePrivate() + "(" + bean.getName() + " self, " + valueName + " value) : base(self, value) { }" + System.lineSeparator());
		sw.write(prefix + "    public override long LogKey => this.Bean.ObjectId + " + var.getId() + ";" + System.lineSeparator());
		sw.write(prefix + "    public override void Commit() { this.BeanTyped." + var.getNamePrivate() + " = this.Value; }" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
	}

	public final void Visit(BeanKey type) {
		WriteLogValue(type);
	}

	public final void Visit(TypeByte type) {
		WriteLogValue(type);
	}

	public final void Visit(TypeDouble type) {
		WriteLogValue(type);
	}

	public final void Visit(TypeInt type) {
		WriteLogValue(type);
	}

	public final void Visit(TypeLong type) {
		WriteLogValue(type);
	}

	public final void Visit(TypeBool type) {
		WriteLogValue(type);
	}

	public final void Visit(TypeBinary type) {
		WriteLogValue(type);
	}

	public final void Visit(TypeString type) {
		WriteLogValue(type);
	}

	private void WriteCollectionLog(Types.Type type) {
		var tn = new TypeName();
		type.Accept(tn);

		sw.write(prefix + "private sealed class Log_" + var.getNamePrivate() + " : " + tn.name + ".LogV" + System.lineSeparator());
		sw.write(prefix + "{" + System.lineSeparator());
		sw.write(prefix + "    public Log_" + var.getNamePrivate() + "(" + bean.getName() + " host, " + tn.nameCollectionImplement + " value) : base(host, value) { }" + System.lineSeparator());
		sw.write(prefix + "    public override long LogKey => Bean.ObjectId + " + var.getId() + ";" + System.lineSeparator());
		sw.write(prefix + "    public " + bean.getName() + " BeanTyped => (" + bean.getName() + ")Bean;" + System.lineSeparator());
		sw.write(prefix + "    public override void Commit() { Commit(BeanTyped." + var.getNamePrivate() + "); }" + System.lineSeparator());
		sw.write(prefix + "}" + System.lineSeparator());
	}

	public final void Visit(TypeList type) {
		WriteCollectionLog(type);
	}

	public final void Visit(TypeSet type) {
		WriteCollectionLog(type);
	}

	public final void Visit(TypeMap type) {
		WriteCollectionLog(type);
	}

	public final void Visit(TypeFloat type) {
		WriteLogValue(type);
	}

	public final void Visit(TypeShort type) {
		WriteLogValue(type);
	}

	public final void Visit(TypeDynamic type) {
		// TypeDynamic 使用写好的类 Zeze.Transaction.DynamicBean，
		// 不再需要生成Log。在这里生成 DynamicBean 需要的两个方法。
		sw.write(String.valueOf(String.format("%1$spublic static long GetSpecialTypeIdFromBean_%2$s(Zeze.Transaction.Bean bean)", prefix, var.getNameUpper1())) + System.lineSeparator());
		sw.write(String.valueOf(String.format("%1$s{", prefix)) + System.lineSeparator());
		sw.write(String.valueOf(String.format("%1$s    switch (bean.TypeId)", prefix)) + System.lineSeparator());
		sw.write(String.valueOf(String.format("%1$s    {", prefix)) + System.lineSeparator());
		sw.write(String.valueOf(String.format("%1$s        case Zeze.Transaction.EmptyBean.TYPEID: return Zeze.Transaction.EmptyBean.TYPEID;", prefix)) + System.lineSeparator());
		for (var real : type.getRealBeans().entrySet()) {
			sw.write(String.format("%1$s        case %2$s: return %3$s; // %4$s", prefix, real.getValue().TypeId, real.getKey(), real.getValue().FullName) + System.lineSeparator());
		}
		sw.write(String.valueOf(String.format("%1$s    }", prefix)) + System.lineSeparator());
		Zeze.Gen.Types.Type tempVar = var.getBean();
		sw.write(String.valueOf(String.format("%1$s    throw new System.Exception(\"Unknown Bean! dynamic@%2$s:%3$s\");", prefix, (tempVar instanceof Bean ? (Bean)tempVar : null).getFullName(), var.getName())) + System.lineSeparator());
		sw.write(String.format("%1$s", prefix}}) + System.lineSeparator());
		sw.WriteLine();
		sw.write(String.valueOf(String.format("%1$spublic static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_%2$s(long typeId)", prefix, var.getNameUpper1())) + System.lineSeparator());
		sw.write(String.valueOf(String.format("%1$s{", prefix)) + System.lineSeparator());
		sw.write(String.valueOf(String.format("%1$s    switch (typeId)", prefix)) + System.lineSeparator());
		sw.write(String.valueOf(String.format("%1$s    {", prefix)) + System.lineSeparator());
		//sw.WriteLine($"{prefix}        case Zeze.Transaction.EmptyBean.TYPEID: return new Zeze.Transaction.EmptyBean();");
		for (var real : type.getRealBeans().entrySet()) {
			sw.write(String.valueOf(String.format("%1$s        case %2$s: return new %3$s();", prefix, real.getKey(), real.getValue().FullName)) + System.lineSeparator());
		}
		sw.write(String.valueOf(String.format("%1$s    }", prefix)) + System.lineSeparator());
		sw.write(String.valueOf(String.format("%1$s    return null;", prefix)) + System.lineSeparator());
		sw.write(String.format("%1$s", prefix}}) + System.lineSeparator());
		sw.WriteLine();
	}
}