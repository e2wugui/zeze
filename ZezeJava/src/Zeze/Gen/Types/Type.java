package Zeze.Gen.Types;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;

public abstract class Type {
	public abstract Type Compile(ModuleSpace space, String key, String value);
	public abstract void Depends(HashSet<Type> includes);
	public abstract void Accept(Visitor visitor);

	public abstract String getName();

	public abstract boolean isImmutable();
	public boolean isBean() {
		return false;
	}
	public boolean isKeyable() {
		return isImmutable();
	}
	public boolean isNormalBean() {
		return isBean() && !isKeyable();
	}
	public boolean isCollection() {
		return false;
	}
	public abstract boolean isNeedNegativeCheck();
	@Override
	public String toString() {
		return getName();
	}

	/////////////////////////////////////////////////////////////////////////////
	private static TreeMap<String, Type> Types = new TreeMap<String, Type> ();
	public static TreeMap<String, Type> getTypes() {
		return Types;
	}
	private static void setTypes(TreeMap<String, Type> value) {
		Types = value;
	}

	public static void Add(Zeze.Gen.ModuleSpace space, Type type) {
		String fullName = space.Path(".", type.getName());

		if (getTypes().containsKey(fullName)) {
			throw new RuntimeException("duplicate type: " + fullName);
		}

		getTypes().put(fullName, type);
	}

	public static Type Compile(Zeze.Gen.ModuleSpace space, String name) {
		return Compile(space, name, null, null);
	}

	public static Type Compile(ModuleSpace space, String name, String key, String value) {
		Type type = null;

		if (getTypes().containsKey(name) && (type = getTypes().get(name)) == type) {
			return type.Compile(space, key, value);
		}

		if (false == Program.IsFullName(name)) {
			name = space.Path(".", name);
			if (getTypes().containsKey(name) && (type = getTypes().get(name)) == type) {
				return type.Compile(space, key, value);
			}
		}

		throw new RuntimeException("type NOT FOUND! '" + name + "'" + key + "." + value);
	}

	static {
		new TypeDouble(getTypes());
		new TypeBinary(getTypes());
		new TypeBool(getTypes());
		new TypeByte(getTypes());
		new TypeInt(getTypes());
		new TypeLong(getTypes());
		new TypeMap(getTypes());
		new TypeList(getTypes());
		new TypeSet(getTypes());
		new TypeString(getTypes());
		new TypeFloat(getTypes());
		new TypeShort(getTypes());
		new TypeDynamic(getTypes());
	}
}