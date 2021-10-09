package Zeze.Gen.cs;

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
		setName("ByteBuffer.BEAN");
	}

	public final void Visit(BeanKey type) {
		setName("ByteBuffer.BEAN");
	}

	public final void Visit(TypeByte type) {
		setName("ByteBuffer.BYTE");
	}

	public final void Visit(TypeShort type) {
		setName("ByteBuffer.SHORT");
	}

	public final void Visit(TypeInt type) {
		setName("ByteBuffer.INT");
	}

	public final void Visit(TypeLong type) {
		setName("ByteBuffer.LONG");
	}

	public final void Visit(TypeBool type) {
		setName("ByteBuffer.BOOL");
	}

	public final void Visit(TypeBinary type) {
		setName("ByteBuffer.BYTES");
	}

	public final void Visit(TypeString type) {
		setName("ByteBuffer.STRING");
	}

	public final void Visit(TypeFloat type) {
		setName("ByteBuffer.FLOAT");
	}

	public final void Visit(TypeDouble type) {
		setName("ByteBuffer.DOUBLE");
	}

	public final void Visit(TypeList type) {
		setName("ByteBuffer.LIST");
	}

	public final void Visit(TypeSet type) {
		setName("ByteBuffer.SET");
	}

	public final void Visit(TypeMap type) {
		setName("ByteBuffer.MAP");
	}

	public final void Visit(TypeDynamic type) {
		setName("ByteBuffer.DYNAMIC");
	}
}