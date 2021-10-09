package Zeze.Gen.ts;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;

public class TypeTagName implements Types.Visitor {
	private String Name;
	public final String getName() {
		return Name;
	}
	private void setName(String value) {
		Name = value;
	}
	public static String GetName(Types.Type type) {
		TypeTagName v = new TypeTagName();
		type.Accept(v);
		return v.getName();
	}

	public final void Visit(Bean type) {
		setName("Zeze.ByteBuffer.BEAN");
	}

	public final void Visit(BeanKey type) {
		setName("Zeze.ByteBuffer.BEAN");
	}

	public final void Visit(TypeByte type) {
		setName("Zeze.ByteBuffer.BYTE");
	}

	public final void Visit(TypeShort type) {
		setName("Zeze.ByteBuffer.SHORT");
	}

	public final void Visit(TypeInt type) {
		setName("Zeze.ByteBuffer.INT");
	}

	public final void Visit(TypeLong type) {
		setName("Zeze.ByteBuffer.LONG");
	}

	public final void Visit(TypeBool type) {
		setName("Zeze.ByteBuffer.BOOL");
	}

	public final void Visit(TypeBinary type) {
		setName("Zeze.ByteBuffer.BYTES");
	}

	public final void Visit(TypeString type) {
		setName("Zeze.ByteBuffer.STRING");
	}

	public final void Visit(TypeFloat type) {
		setName("Zeze.ByteBuffer.FLOAT");
	}

	public final void Visit(TypeDouble type) {
		setName("Zeze.ByteBuffer.DOUBLE");
	}

	public final void Visit(TypeList type) {
		setName("Zeze.ByteBuffer.LIST");
	}

	public final void Visit(TypeSet type) {
		setName("Zeze.ByteBuffer.SET");
	}

	public final void Visit(TypeMap type) {
		setName("Zeze.ByteBuffer.MAP");
	}

	public final void Visit(TypeDynamic type) {
		setName("Zeze.ByteBuffer.DYNAMIC");
	}
}