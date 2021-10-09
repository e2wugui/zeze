package Zeze;

import Zeze.Serialize.*;
import java.util.*;

	public static class Table implements Serializable {
		private String Name;
		public final String getName() {
			return Name;
		}
		public final void setName(String value) {
			Name = value;
		}
		private String KeyName;
		public final String getKeyName() {
			return KeyName;
		}
		public final void setKeyName(String value) {
			KeyName = value;
		}
		private String ValueName;
		public final String getValueName() {
			return ValueName;
		}
		public final void setValueName(String value) {
			ValueName = value;
		}
		private Type KeyType;
		public final Type getKeyType() {
			return KeyType;
		}
		private void setKeyType(Type value) {
			KeyType = value;
		}
		private Type ValueType;
		public final Type getValueType() {
			return ValueType;
		}
		private void setValueType(Type value) {
			ValueType = value;
		}

		public final void Decode(ByteBuffer bb) {
			setName(bb.ReadString());
			setKeyName(bb.ReadString());
			setValueName(bb.ReadString());
		}

		public final void Encode(ByteBuffer bb) {
			bb.WriteString(getName());
			bb.WriteString(getKeyName());
			bb.WriteString(getValueName());
		}

		public final boolean IsCompatible(Table other, Context context) {
			return getName().equals(other.getName()) && getKeyType().IsCompatible(other.getKeyType(), context, (bean) -> {
						setKeyName(bean.Name);
						setKeyType(bean);
			}, null) && getValueType().IsCompatible(other.getValueType(), context, (bean) -> {
						setValueName(bean.Name);
						setValueType(bean);
					}, null);
		}
		public final void Compile(Schemas s) {
			setKeyType(s.Compile(getKeyName(), "", ""));
			boolean tempVar = KeyType instanceof Bean;
			Bean bean = tempVar ? (Bean)KeyType : null;
			if (tempVar) {
				bean.setKeyRefCount(bean.getKeyRefCount() + 1);
			}
			setValueType(s.Compile(getValueName(), "", ""));
		}
	}

	private HashMap<String, Table> Tables = new HashMap<String, Table> ();
	public final HashMap<String, Table> getTables() {
		return Tables;
	}
	private HashMap<String, Bean> Beans = new HashMap<String, Bean> ();
	public final HashMap<String, Bean> getBeans() {
		return Beans;
	}

	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	public final boolean IsCompatible(Schemas other, Config config) {
		if (null == other) {
			return true;
		}

		var context = new Context();
		context.setCurrent(this);
		context.setPrevious(other);
		context.setConfig(config);
		for (var table : getTables().values()) {
			if (other.getTables().containsKey(table.Name) && (var otherTable = other.getTables().get(table.Name)) == var otherTable) {
				if (false == table.IsCompatible(otherTable, context)) {
					logger.Error("Not Compatible. table={0}", table.Name);
					return false;
				}
			}
		}
		context.Update();
		return true;
	}

	public final void Decode(ByteBuffer bb) {
		for (int count = bb.ReadInt(); count > 0; --count) {
			var table = new Table();
			table.Decode(bb);
			getTables().put(table.getName(), table);
		}
		for (int count = bb.ReadInt(); count > 0; --count) {
			var bean = new Bean();
			bean.Decode(bb);
			getBeans().put(bean.getName(), bean);
		}
	}

	public final void Encode(ByteBuffer bb) {
		bb.WriteInt(getTables().size());
		for (var table : getTables().values()) {
			table.Encode(bb);
		}
		bb.WriteInt(getBeans().size());
		for (var bean : getBeans().values()) {
			bean.Encode(bb);
		}
	}

	public final void Compile() {
		for (var table : getTables().values()) {
			table.Compile(this);
		}
		for (var bean : getBeans().values()) {
			bean.Compile(this);
		}
	}

	private HashMap<String, Type> BasicTypes = new HashMap<String, Type> ();
	private HashMap<String, Type> getBasicTypes() {
		return BasicTypes;
	}

	public final Type Compile(String type, String key, String value) {
		if (tangible.StringHelper.isNullOrEmpty(type)) {
			return null;
		}

		if (getBeans().containsKey(type) && (var bean = getBeans().get(type)) == var bean) {
			return bean;
		}

		// 除了Bean，其他基本类型和容器类型都动态创建。
		if (getBasicTypes().containsKey(String.format("%1$s:%2$s:%3$s", type, key, value)) && (var o = getBasicTypes().get(String.format("%1$s:%2$s:%3$s", type, key, value))) == var o) {
			return o;
		}

		var n = new Type();
		n.setName(type);
		n.setKeyName(key);
		n.setValueName(value);
		getBasicTypes().put(String.format("%1$s:%2$s:%3$s", type, key, value), n);
		n.Compile(this); // 容器需要编译。这里的时机不是太好。
		return n;
	}

	public final void AddBean(Bean bean) {
		getBeans().put(bean.getName(), bean);
	}

	public final void AddTable(Table table) {
		getTables().put(table.getName(), table);
	}
}
