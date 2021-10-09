package Zeze.Gen;

import Zeze.*;
import java.util.*;

public class Table {
	private ModuleSpace Space;
	public final ModuleSpace getSpace() {
		return Space;
	}
	private void setSpace(ModuleSpace value) {
		Space = value;
	}
	private String Name;
	public final String getName() {
		return Name;
	}
	private void setName(String value) {
		Name = value;
	}
	private String Key;
	public final String getKey() {
		return Key;
	}
	private String Value;
	public final String getValue() {
		return Value;
	}
	private String Gen;
	public final String getGen() {
		return Gen;
	}
	private boolean IsMemory;
	public final boolean isMemory() {
		return IsMemory;
	}
	private boolean IsAutoKey;
	public final boolean isAutoKey() {
		return IsAutoKey;
	}

	// setup in compile
	private Zeze.Gen.Types.Type KeyType;
	public final Zeze.Gen.Types.Type getKeyType() {
		return KeyType;
	}
	private void setKeyType(Zeze.Gen.Types.Type value) {
		KeyType = value;
	}
	private Zeze.Gen.Types.Type ValueType;
	public final Zeze.Gen.Types.Type getValueType() {
		return ValueType;
	}
	private void setValueType(Zeze.Gen.Types.Type value) {
		ValueType = value;
	}
	public final String getFullName() {
		return getSpace().Path(".", getName());
	}

	public Table(ModuleSpace space, XmlElement self) {
		setSpace(space);
		setName(self.GetAttribute("name").strip());
		Program.CheckReserveName(getName());
		space.Add(this);

		Key = self.GetAttribute("key");
		Value = self.GetAttribute("value");
		Gen = self.GetAttribute("gen");
		String attr = self.GetAttribute("memory");
		IsMemory = attr.length() > 0 ? Boolean.parseBoolean(attr) : false;
		attr = self.GetAttribute("autokey");
		IsAutoKey = attr.length() > 0 ? Boolean.parseBoolean(attr) : false;
	}

	public final void Compile() {
		setKeyType(Types.Type.Compile(getSpace(), getKey()));
		if (false == getKeyType().isKeyable()) {
			throw new RuntimeException("table.key need a isKeyable type: " + getSpace().Path(".", getName()));
		}
		if (this.isAutoKey() && !(getKeyType() instanceof Zeze.Gen.Types.TypeLong)) {
			throw new RuntimeException("autokey only support key type of long");
		}

		setValueType(Types.Type.Compile(getSpace(), getValue()));
		if (!getValueType().isNormalBean()) { // is normal bean, exclude beankey
			throw new RuntimeException("table.value need a normal bean : " + getSpace().Path(".", getName()));
		}
	}

	public final void Depends(HashSet<Zeze.Gen.Types.Type> depends) {
		getKeyType().Depends(depends);
		getValueType().Depends(depends);
	}
}