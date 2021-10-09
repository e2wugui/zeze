package Zeze.Gen.ts;

import Zeze.*;
import Zeze.Gen.*;
import java.io.*;

public class BeanFormatter {
	private Types.Bean bean;

	public BeanFormatter(Types.Bean bean) {
		this.bean = bean;
	}

	public final void Make(OutputStreamWriter sw) {
		sw.write("export class " + bean.getSpace().Path("_", bean.getName()) + " implements Zeze.Bean {" + System.lineSeparator());
		// declare enums
		for (Types.Enum e : bean.getEnums()) {
			sw.write("    public static readonly " + e.getName() + " = " + e.getValue() + ";" + e.getComment() + System.lineSeparator());
		}
		if (!bean.getEnums().isEmpty()) {
			sw.write("" + System.lineSeparator());
		}
		// declare variables
		for (Types.Variable v : bean.getVariables()) {
			sw.write(String.valueOf(String.format("    public %1$s: %2$s; %3$s", v.getName(), TypeName.GetName(v.getVariableType()), v.getComment())) + System.lineSeparator());
		}
		sw.write("" + System.lineSeparator());
		sw.WriteLine();
		Construct.Make(bean, sw, "    ");
		sw.WriteLine();
		sw.write("    public static readonly TYPEID: bigint = " + bean.getTypeId() + "n;" + System.lineSeparator());
		sw.write("    public TypeId(): bigint { return " + bean.getSpace().Path("_", bean.getName()) + ".TYPEID; }" + System.lineSeparator());
		sw.WriteLine();
		Encode.Make(bean, sw, "    ");
		Decode.Make(bean, sw, "    ");
		MakeDynamicStaticFunc(sw);
		sw.write("}" + System.lineSeparator());
		sw.WriteLine();
	}

	public final void MakeDynamicStaticFunc(OutputStreamWriter sw) {
		for (var v : bean.getVariables()) {
			boolean tempVar = v.VariableType instanceof Types.TypeDynamic;
			Types.TypeDynamic d = tempVar ? (Types.TypeDynamic)v.VariableType : null;
			if (tempVar) {
				sw.write(String.valueOf(String.format("    public static GetSpecialTypeIdFromBean_%1$s(bean: Zeze.Bean): bigint {", v.getNameUpper1())) + System.lineSeparator());
				sw.write(String.format("        switch (bean.TypeId())") + System.lineSeparator());
				sw.write(String.format("        {{") + System.lineSeparator());
				sw.write(String.format("            case Zeze.EmptyBean.TYPEID: return Zeze.EmptyBean.TYPEID;") + System.lineSeparator());
				for (var real : d.getRealBeans().entrySet()) {
					sw.write(String.format("            case %1$sn: return %2$sn; // %3$s", real.getValue().TypeId, real.getKey(), real.getValue().FullName) + System.lineSeparator());
				}
				sw.write(String.format("        }}") + System.lineSeparator());
				Zeze.Gen.Types.Type tempVar2 = v.getBean();
				sw.write(String.valueOf(String.format("        throw new Error(\"Unknown Bean! dynamic@%1$s:%2$s\");", (tempVar2 instanceof Types.Bean ? (Types.Bean)tempVar2 : null).getFullName(), v.getName())) + System.lineSeparator());
				sw.write(String.format("    }}") + System.lineSeparator());
				sw.WriteLine();
				sw.write(String.valueOf(String.format("    public static CreateBeanFromSpecialTypeId_%1$s(typeId: bigint): Zeze.Bean {", v.getNameUpper1())) + System.lineSeparator());
				sw.write(String.format("        switch (typeId)") + System.lineSeparator());
				sw.write(String.format("        {{") + System.lineSeparator());
				//sw.WriteLine($"            case Zeze.EmptyBean.TYPEID: return new Zeze.EmptyBean();");
				for (var real : d.getRealBeans().entrySet()) {
					sw.write(String.format("            case %1$sn: return new %2$s();", real.getKey(), real.getValue().Space.Path("_", real.getValue().Name)) + System.lineSeparator());
				}
				sw.write(String.format("        }}") + System.lineSeparator());
				sw.write(String.format("        return null;") + System.lineSeparator());
				sw.write(String.format("    }}") + System.lineSeparator());
				sw.WriteLine();
			}
		}
	}
}