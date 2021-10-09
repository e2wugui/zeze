package Zeze.Gen.ts;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;

public class TypeName implements Types.Visitor {
	public String name;
	public String nameCollectionImplement; // 容器内部类型。其他情况下为 null。

	public static String GetName(Types.Type type) {
		var visitor = new TypeName();
		type.Accept(visitor);
		return visitor.name;
	}

	public final void Visit(Bean type) {
		name = type.getSpace().Path("_", type.getName());
	}

	public final void Visit(BeanKey type) {
		name = type.getSpace().Path("_", type.getName());
	}

	public final void Visit(TypeByte type) {
		name = "number";
	}

	public final void Visit(TypeDouble type) {
		name = "number";
	}

	public final void Visit(TypeInt type) {
		name = "number";
	}

	public final void Visit(TypeLong type) {
		name = "bigint";
	}

	public final void Visit(TypeBool type) {
		name = "boolean";
	}

	public final void Visit(TypeBinary type) {
		name = "Uint8Array";
	}

	public final void Visit(TypeString type) {
		name = "string";
	}

	public final void Visit(TypeList type) {
		String valueName = TypeName.GetName(type.getValueType());
		name = "Array<" + valueName + ">";
	}

	public final void Visit(TypeSet type) {
		String valueName = TypeName.GetName(type.getValueType());
		name = "Set<" + valueName + ">";
	}

	public final void Visit(TypeMap type) {
		String key = TypeName.GetName(type.getKeyType());
		String value = TypeName.GetName(type.getValueType());
		name = "Map<" + key + ", " + value + ">";
	}

	public final void Visit(TypeFloat type) {
		name = "number";
	}

	public final void Visit(TypeShort type) {
		name = "number";
	}

	public final void Visit(TypeDynamic type) {
		name = "Zeze.DynamicBean";
	}
}