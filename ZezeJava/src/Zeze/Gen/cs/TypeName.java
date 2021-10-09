package Zeze.Gen.cs;

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
		name = type.getSpace().Path(".", type.getName());
	}

	public final void Visit(BeanKey type) {
		name = type.getSpace().Path(".", type.getName());
	}

	public final void Visit(TypeByte type) {
		name = "byte";
	}

	public final void Visit(TypeDouble type) {
		name = "double";
	}

	public final void Visit(TypeInt type) {
		name = "int";
	}

	public final void Visit(TypeLong type) {
		name = "long";
	}

	public final void Visit(TypeBool type) {
		name = "bool";
	}

	public final void Visit(TypeBinary type) {
		name = "Zeze.Net.Binary";
	}

	public final void Visit(TypeString type) {
		name = "string";
	}

	public final void Visit(TypeList type) {
		String valueName = TypeName.GetName(type.getValueType());
		name = "Zeze.Transaction.Collections.PList" + (type.getValueType().isNormalBean() ? "2<" : "1<") + valueName + ">";
		nameCollectionImplement = "System.Collections.Immutable.ImmutableList<" + valueName + ">";
	}

	public final void Visit(TypeSet type) {
		String valueName = TypeName.GetName(type.getValueType());
		name = "Zeze.Transaction.Collections.PSet1<" + valueName + ">";
		nameCollectionImplement = "System.Collections.Immutable.ImmutableHashSet<" + valueName + ">";
	}

	public final void Visit(TypeMap type) {
		String key = TypeName.GetName(type.getKeyType());
		String value = TypeName.GetName(type.getValueType());
		name = "Zeze.Transaction.Collections.PMap" + (type.getValueType().isNormalBean() ? "2<" : "1<") + key + ", " + value + ">";
		nameCollectionImplement = "System.Collections.Immutable.ImmutableDictionary<" + key + ", " + value + ">";
	}

	public final void Visit(TypeFloat type) {
		name = "float";
	}

	public final void Visit(TypeShort type) {
		name = "short";
	}

	public final void Visit(TypeDynamic type) {
		name = "Zeze.Transaction.DynamicBean";
	}
}