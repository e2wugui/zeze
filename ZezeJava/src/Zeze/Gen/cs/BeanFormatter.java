package Zeze.Gen.cs;

import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class BeanFormatter {
	private Types.Bean bean;

	public BeanFormatter(Types.Bean bean) {
		this.bean = bean;
	}

	public final void Make(String baseDir) {
		try (OutputStreamWriter sw = bean.getSpace().OpenWriter(baseDir, bean.getName() + ".cs", true)) {
    
			sw.write("// auto-generated" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("using Zeze.Serialize;" + System.lineSeparator());
			sw.write("using System;" + System.lineSeparator());
			//sw.WriteLine("using Zeze.Transaction.Collections;");
			sw.write("" + System.lineSeparator());
			sw.write("namespace " + bean.getSpace().Path(".", null) + System.lineSeparator());
			sw.write("{" + System.lineSeparator());
			sw.write(String.valueOf(String.format("    public interface %1$sReadOnly", bean.getName())) + System.lineSeparator());
			sw.write("    {" + System.lineSeparator());
			PropertyReadOnly.Make(bean, sw, "        ");
			sw.write("    }" + System.lineSeparator());
			sw.WriteLine();
			sw.write(String.valueOf(String.format("    public sealed class %1$s : Zeze.Transaction.Bean, %2$sReadOnly", bean.getName(), bean.getName())) + System.lineSeparator());
			sw.write("    {" + System.lineSeparator());
			WriteDefine(sw);
			sw.write("    }" + System.lineSeparator());
			sw.write("}" + System.lineSeparator());
		}
	}

	public final void WriteDefine(OutputStreamWriter sw) {
		// declare enums
		for (Types.Enum e : bean.getEnums()) {
			sw.write("        public const int " + e.getName() + " = " + e.getValue() + ";" + e.getComment() + System.lineSeparator());
		}
		if (!bean.getEnums().isEmpty()) {
			sw.write("" + System.lineSeparator());
		}

		// declare variables
		for (Types.Variable v : bean.getVariables()) {
			sw.write("        private " + TypeName.GetName(v.getVariableType()) + " " + v.getNamePrivate() + ";" + v.getComment() + System.lineSeparator());
			boolean tempVar = v.VariableType instanceof Types.TypeMap;
			Types.TypeMap pmap = tempVar ? (Types.TypeMap)v.VariableType : null;
			if (tempVar) {
				var key = TypeName.GetName(pmap.getKeyType());
				var value = pmap.getValueType().isNormalBean() ? TypeName.GetName(pmap.getValueType()) + "ReadOnly" : TypeName.GetName(pmap.getValueType());
				var readonlyTypeName = String.format("Zeze.Transaction.Collections.PMapReadOnly<%1$s,%2$s,%3$s>", key, value, TypeName.GetName(pmap.getValueType()));
				sw.write(String.valueOf(String.format("        private %1$s %2$sReadOnly;", readonlyTypeName, v.getNamePrivate())) + System.lineSeparator());
			}
		}
		sw.write("" + System.lineSeparator());

		Property.Make(bean, sw, "        ");
		sw.WriteLine();
		Construct.Make(bean, sw, "        ");
		Assign.Make(bean, sw, "        ");
		// Copy
		sw.write("        public " + bean.getName() + " CopyIfManaged()" + System.lineSeparator());
		sw.write("        {" + System.lineSeparator());
		sw.write("            return IsManaged ? Copy() : this;" + System.lineSeparator());
		sw.write("        }" + System.lineSeparator());
		sw.WriteLine();
		sw.write("        public " + bean.getName() + " Copy()" + System.lineSeparator());
		sw.write("        {" + System.lineSeparator());
		sw.write("            var copy = new " + bean.getName() + "();" + System.lineSeparator());
		sw.write("            copy.Assign(this);" + System.lineSeparator());
		sw.write("            return copy;" + System.lineSeparator());
		sw.write("        }" + System.lineSeparator());
		sw.WriteLine();
		sw.write(String.valueOf(String.format("        public static void Swap(%1$s a, %2$s b)", bean.getName(), bean.getName())) + System.lineSeparator());
		sw.write("        {" + System.lineSeparator());
		sw.write(String.valueOf(String.format("            %1$s save = a.Copy();", bean.getName())) + System.lineSeparator());
		sw.write("            a.Assign(b);" + System.lineSeparator());
		sw.write("            b.Assign(save);" + System.lineSeparator());
		sw.write("        }" + System.lineSeparator());
		sw.WriteLine();
		sw.write("        public override Zeze.Transaction.Bean CopyBean()" + System.lineSeparator());
		sw.write("        {" + System.lineSeparator());
		sw.write("            return Copy();" + System.lineSeparator());
		sw.write("        }" + System.lineSeparator());
		sw.WriteLine();
		sw.write("        public const long TYPEID = " + bean.getTypeId() + ";" + System.lineSeparator());
		sw.write("        public override long TypeId => TYPEID;" + System.lineSeparator());
		sw.WriteLine();
		Log.Make(bean, sw, "        ");
		Tostring.Make(bean, sw, "        ");
		Encode.Make(bean, sw, "        ");
		Decode.Make(bean, sw, "        ");
		InitChildrenTableKey.Make(bean, sw, "        ");
		NegativeCheck.Make(bean, sw, "        ");
	}
}