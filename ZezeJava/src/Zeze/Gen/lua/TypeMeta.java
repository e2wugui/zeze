package Zeze.Gen.lua;

import Zeze.Gen.Types.*;
import Zeze.*;
import Zeze.Gen.*;

public class TypeMeta implements Types.Visitor {
	private Types.Variable Var;
	public final Types.Variable getVar() {
		return Var;
	}
	private void setVar(Types.Variable value) {
		Var = value;
	}
	private int Type;
	public final int getType() {
		return Type;
	}
	private void setType(int value) {
		Type = value;
	}
	private long TypeBeanTypeId;
	public final long getTypeBeanTypeId() {
		return TypeBeanTypeId;
	}
	private void setTypeBeanTypeId(long value) {
		TypeBeanTypeId = value;
	}
	private int Key;
	public final int getKey() {
		return Key;
	}
	private void setKey(int value) {
		Key = value;
	}
	private long KeyBeanTypeId;
	public final long getKeyBeanTypeId() {
		return KeyBeanTypeId;
	}
	private void setKeyBeanTypeId(long value) {
		KeyBeanTypeId = value;
	}
	private int Value;
	public final int getValue() {
		return Value;
	}
	private void setValue(int value) {
		Value = value;
	}
	private long ValueBeanTypeId;
	public final long getValueBeanTypeId() {
		return ValueBeanTypeId;
	}
	private void setValueBeanTypeId(long value) {
		ValueBeanTypeId = value;
	}

	@Override
	public String toString() {
		return String.format("{ %1$s,%2$s,%3$s,%4$s,%5$s,%6$s,\"%7$s\" }", getType(), getTypeBeanTypeId(), getKey(), getKeyBeanTypeId(), getValue(), getValueBeanTypeId(), getVar().getNamePinyin());
	}

	public TypeMeta(Variable var) {
		setVar(var);
	}

	public static TypeMeta Get(Types.Variable var, Types.Type type) {
		TypeMeta v = new TypeMeta(var);
		type.Accept(v);
		return v;
	}

	public final void Visit(Bean type) {
		setType(Zeze.Serialize.ByteBuffer.BEAN);
		setTypeBeanTypeId(type.getTypeId());
	}

	public final void Visit(BeanKey type) {
		setType(Zeze.Serialize.ByteBuffer.BEAN);
		setTypeBeanTypeId(type.getTypeId());
	}

	public final void Visit(TypeByte type) {
		setType(Zeze.Serialize.ByteBuffer.BYTE);
	}

	public final void Visit(TypeShort type) {
		setType(Zeze.Serialize.ByteBuffer.SHORT);
	}

	public final void Visit(TypeInt type) {
		setType(Zeze.Serialize.ByteBuffer.INT);
	}

	public final void Visit(TypeLong type) {
		setType(Zeze.Serialize.ByteBuffer.LONG);
	}

	public final void Visit(TypeBool type) {
		setType(Zeze.Serialize.ByteBuffer.BOOL);
	}

	public final void Visit(TypeBinary type) {
		setType(Zeze.Serialize.ByteBuffer.BYTES);
	}

	public final void Visit(TypeString type) {
		setType(Zeze.Serialize.ByteBuffer.STRING);
	}

	public final void Visit(TypeFloat type) {
		setType(Zeze.Serialize.ByteBuffer.FLOAT);
	}

	public final void Visit(TypeDouble type) {
		setType(Zeze.Serialize.ByteBuffer.DOUBLE);
	}

	public final void Visit(TypeList type) {
		setType(Zeze.Serialize.ByteBuffer.LIST);
		TypeMeta vm = TypeMeta.Get(getVar(), type.getValueType());
		setValue(vm.getType());
		setValueBeanTypeId(vm.getTypeBeanTypeId());
	}

	public final void Visit(TypeSet type) {
		setType(Zeze.Serialize.ByteBuffer.SET);
		TypeMeta vm = TypeMeta.Get(getVar(), type.getValueType());
		setValue(vm.getType());
		setValueBeanTypeId(vm.getTypeBeanTypeId());
	}

	public final void Visit(TypeMap type) {
		setType(Zeze.Serialize.ByteBuffer.MAP);

		TypeMeta km = TypeMeta.Get(getVar(), type.getKeyType());
		setKey(km.getType());
		setKeyBeanTypeId(km.getTypeBeanTypeId());

		TypeMeta vm = TypeMeta.Get(getVar(), type.getValueType());
		setValue(vm.getType());
		setValueBeanTypeId(vm.getTypeBeanTypeId());
	}

	public final void Visit(TypeDynamic type) {
		setType(Zeze.Serialize.ByteBuffer.DYNAMIC);
		// TypeBeanTypeId = 使用的时候指定。
	}
}