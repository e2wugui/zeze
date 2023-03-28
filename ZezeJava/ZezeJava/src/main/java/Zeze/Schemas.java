package Zeze;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Action1;
import Zeze.Util.IntHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 1 启动数据库时，用来判断当前代码的数据定义结构是否和当前数据库的定义结构兼容。
 * 当前包含以下兼容检测。
 * a) 对于每个 Variable.Id，Type不能修改。
 * b) 不能复用已经删除的 Variable.Id。
 * 但是允许"反悔"，也就是说可以重新使用已经删除的Variable.Id时，只要Type和原来一样，就允许。
 * 这是为了处理多人使用同一个数据库进行开发时的冲突（具体不解释了）。
 * c) beankey 被应用于map.Key或set.Value或table.Key以后就不能再删除变量了。
 * 当作key以后，如果删除变量，beankey.encode() 就可能不再唯一。
 * <p>
 * 2 通过查询类型信息，从数据转换到具体实例。合服可能需要。
 * 如果是通用合并的insert，应该在二进制接口上操作（目前还没有）。
 * 如果合并时需要处理冲突，此时应用是知道具体类型的。
 * 所以这个功能暂时先不提供了。
 */
public class Schemas implements Serializable {
	public static class Checked {
		private Bean previous;
		private Bean current;

		public final Bean getPrevious() {
			return previous;
		}

		public final void setPrevious(Bean value) {
			previous = value;
		}

		public final Bean getCurrent() {
			return current;
		}

		public final void setCurrent(Bean value) {
			current = value;
		}

		@Override
		public int hashCode() {
			final int _prime_ = 31;
			int _h_ = 0;
			_h_ = _h_ * _prime_ + getPrevious().name.hashCode();
			_h_ = _h_ * _prime_ + getCurrent().name.hashCode();
			return _h_;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			boolean tempVar = obj instanceof Checked;
			Checked other = tempVar ? (Checked)obj : null;
			return (tempVar) && getPrevious() == other.getPrevious() && getCurrent() == other.getCurrent();
		}
	}

	public static class CheckResult {
		private Bean bean;
		private final ArrayList<Action1<Bean>> updates = new ArrayList<>();
		private final ArrayList<Action1<Bean>> updateVariables = new ArrayList<>();

		public final Bean getBean() {
			return bean;
		}

		public final void setBean(Bean value) {
			bean = value;
		}

		private ArrayList<Action1<Bean>> getUpdates() {
			return updates;
		}

		private ArrayList<Action1<Bean>> getUpdateVariables() {
			return updateVariables;
		}

		public final void addUpdate(Action1<Bean> Update, Action1<Bean> UpdateVariable) {
			getUpdates().add(Update);
			if (null != UpdateVariable) {
				getUpdateVariables().add(UpdateVariable);
			}
		}

		public final void update() throws Exception {
			for (var update : getUpdates()) {
				update.run(getBean());
			}
			for (var update : getUpdateVariables()) {
				update.run(getBean());
			}
		}
	}

	public static class Context {
		private Schemas current;
		private Schemas previous;
		private final HashMap<Checked, CheckResult> checked = new HashMap<>();
		private final HashMap<Bean, CheckResult> copyBeanIfRemoved = new HashMap<>();
		private Config config;
		private long renameCount;

		public final Schemas getCurrent() {
			return current;
		}

		public final void setCurrent(Schemas value) {
			current = value;
		}

		public final Schemas getPrevious() {
			return previous;
		}

		public final void setPrevious(Schemas value) {
			previous = value;
		}

		public final HashMap<Checked, CheckResult> getChecked() {
			return checked;
		}

		public final HashMap<Bean, CheckResult> getCopyBeanIfRemoved() {
			return copyBeanIfRemoved;
		}

		public final Config getConfig() {
			return config;
		}

		public final void setConfig(Config value) {
			config = value;
		}

		public final CheckResult getCheckResult(Bean previous, Bean current) {
			Schemas.Checked tempVar = new Schemas.Checked();
			tempVar.setPrevious(previous);
			tempVar.setCurrent(current);
			return getChecked().get(tempVar);
		}

		public final void addCheckResult(Bean previous, Bean current, CheckResult result) {
			Schemas.Checked tempVar = new Schemas.Checked();
			tempVar.setPrevious(previous);
			tempVar.setCurrent(current);
			if (null != getChecked().put(tempVar, result))
				throw new IllegalStateException("duplicate var in Checked Map");
		}

		public final CheckResult getCopyBeanIfRemovedResult(Bean bean) {
			return getCopyBeanIfRemoved().get(bean);
		}

		public final void addCopyBeanIfRemovedResult(Bean bean, CheckResult result) {
			if (null != getCopyBeanIfRemoved().put(bean, result))
				throw new IllegalStateException("duplicate bean in CopyBeanIfRemoved Map");
		}

		public final void update() throws Exception {
			for (var result : getChecked().values())
				result.update();
			for (var result : getCopyBeanIfRemoved().values())
				result.update();
		}

		public final String generateUniqueName() {
			renameCount++;
			return "_" + renameCount;
		}
	}

	public static class Type implements Serializable {
		public String name;
		public String keyName = "";
		public String valueName = "";
		public Type key;
		public Type value;

		private static final Map<String, Integer> compatibleTable = new HashMap<>();

		static {
			compatibleTable.put("bool", 1);
			compatibleTable.put("boolean", 1);
			compatibleTable.put("byte", 1);
			compatibleTable.put("short", 1);
			compatibleTable.put("int", 1);
			compatibleTable.put("long", 1);
			compatibleTable.put("float", 1);
			compatibleTable.put("double", 1);
			compatibleTable.put("binary", 2);
			compatibleTable.put("string", 2);
			compatibleTable.put("list", 3);
			compatibleTable.put("set", 3);
			compatibleTable.put("map", 4);
			compatibleTable.put("vector2", 5);
			compatibleTable.put("vector3", 5);
			compatibleTable.put("vector4", 5);
			compatibleTable.put("quaternion", 5);
			compatibleTable.put("vector2int", 5);
			compatibleTable.put("vector3int", 5);
		}
		//VectorBean

		private static boolean isTypeNameCompatible(String typeName0, String typeName1) {
			if (typeName0.equals(typeName1))
				return true;
			var t0 = compatibleTable.get(typeName0);
			var t1 = compatibleTable.get(typeName1);
			return t0 != null && t0.equals(t1);
		}

		public boolean isCompatible(Type other, Context context,
									Action1<Bean> Update,
									Action1<Bean> UpdateVariable) {
			if (other == this)
				return true;
			if (other == null)
				return false;
			if (!isTypeNameCompatible(name, other.name))
				return false;

			// Name 相同的情况下，下面的 Key Value 仅在 Collection 时有值。
			// 当 this.Key == null && other.Key != null 在 Name 相同的情况下是不可能发生的。
			if (null != key) {
				if (!key.isCompatible(other.key, context,
						(bean) -> {
							keyName = bean.name;
							key = bean;
						}, UpdateVariable)) {
					return false;
				}
			} else if (other.key != null)
				throw new IllegalStateException("(this.Key == null && other.Key != null) Impossible!");

			if (null != value) {
				return value.isCompatible(other.value, context,
						(bean) -> {
							valueName = bean.name;
							value = bean;
						}, UpdateVariable);
			}
			if (other.value != null)
				throw new IllegalStateException("(this.Value == null && other.Value != null) Impossible!");

			return true;
		}

		@Override
		public void decode(ByteBuffer bb) {
			name = bb.ReadString();
			keyName = bb.ReadString();
			valueName = bb.ReadString();
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteString(name);
			bb.WriteString(keyName);
			bb.WriteString(valueName);
		}

		public void compile(Schemas s) {
			key = s.compile(keyName, "", "");
			if (key != null && key instanceof Bean)
				((Bean)key).keyRefCount++;

			value = s.compile(valueName, "", "");
			if (value != null && name.equals("set") && value instanceof Bean)
				((Bean)value).keyRefCount++;
		}

		public void tryCopyBeanIfRemoved(Context context,
										 Action1<Bean> Update,
										 Action1<Bean> UpdateVariable) {
			if (key != null) {
				key.tryCopyBeanIfRemoved(context, bean -> {
							keyName = bean.name;
							key = bean;
						},
						UpdateVariable);
			}
			if (value != null) {
				value.tryCopyBeanIfRemoved(context, bean -> {
							valueName = bean.name;
							value = bean;
						},
						UpdateVariable);
			}
		}

		private static final Map<String, String> sqlTypeTable = new HashMap<>();

		static {
			sqlTypeTable.put("bool", "BOOL");
			//sqlTypeTable.put("boolean", "BOOL");
			sqlTypeTable.put("byte", "TINYINT");
			sqlTypeTable.put("short", "SMALLINT");
			sqlTypeTable.put("int", "INT");
			sqlTypeTable.put("long", "BIGINT");
			sqlTypeTable.put("float", "FLOAT");
			sqlTypeTable.put("double", "DOUBLE");
			sqlTypeTable.put("binary", "VARBINARY(65535)");
			sqlTypeTable.put("string", "VARCHAR(65535)");
			// json
			sqlTypeTable.put("dynamic", "VARCHAR(65535)");
			sqlTypeTable.put("list", "VARCHAR(65535)");
			sqlTypeTable.put("set", "VARCHAR(65535)");
			sqlTypeTable.put("map", "VARCHAR(65535)");

			// 下面的类型在处理完diff之后会被展开，这里的类型不是映射的实际类型。。
			sqlTypeTable.put("vector2", "vector2");
			sqlTypeTable.put("vector2int", "vector2int");
			sqlTypeTable.put("vector3", "vector3");
			sqlTypeTable.put("vector3int", "vector3int");
			sqlTypeTable.put("vector4", "vector4");
			sqlTypeTable.put("quaternion", "quaternion");
		}

		public String toSqlType() {
			var sqlType = sqlTypeTable.get(name);
			if (null == sqlType)
				throw new RuntimeException("unknown sql type=" + name);
			return sqlType;
		}

		public static String toColumnName(ArrayList<String> varNames) {
			var sb = new StringBuilder();
			sb.append(varNames.get(0));
			for (int i = 1; i < varNames.size(); ++i)
				sb.append("_").append(varNames.get(i));
			return sb.toString();
		}

		public void buildRelationalColumns(Table table, Bean bean, Variable variable,
										   ArrayList<String> varNames, ArrayList<Column> columns) {
			var column = new Column(toColumnName(varNames), table, bean, variable, toSqlType());
			columns.add(column);
		}
	}

	public static class Variable implements Serializable {
		public int id;
		public String name;
		public String typeName;
		public String keyName = "";
		public String valueName = "";
		public Type type;
		public boolean deleted;

		public Variable() {
		}

		@Override
		public void decode(ByteBuffer bb) {
			id = bb.ReadInt();
			name = bb.ReadString();
			typeName = bb.ReadString();
			keyName = bb.ReadString();
			valueName = bb.ReadString();
			deleted = bb.ReadBool();
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(id);
			bb.WriteString(name);
			bb.WriteString(typeName);
			bb.WriteString(keyName);
			bb.WriteString(valueName);
			bb.WriteBool(deleted);
		}

		public void compile(Schemas s) {
			type = s.compile(typeName, keyName, valueName);
		}

		public boolean isCompatible(Variable other, Context context) {
			return this.type.isCompatible(other.type, context,
					(bean) ->
					{
						typeName = bean.name;
						type = bean;
					},
					(bean) ->
					{
						keyName = type.keyName;
						valueName = type.valueName;
					});
		}

		public final void update() {
			keyName = this.type.keyName;
			valueName = this.type.valueName;
		}

		public final void tryCopyBeanIfRemoved(Context context) {
			this.type.tryCopyBeanIfRemoved(context, (bean) -> {
				typeName = bean.name;
				type = bean;
			}, (bean) -> {
				keyName = type.keyName;
				valueName = type.valueName;
			});
		}
	}

	public static class Bean extends Type {
		private static final Logger logger = LogManager.getLogger(Bean.class);

		private final IntHashMap<Variable> variables = new IntHashMap<>();
		private boolean isBeanKey;
		private int keyRefCount;
		// 这个变量当前是不需要的，作为额外的属性记录下来，以后可能要用。
		private boolean deleted;
		// 这里记录在当前版本Schemas中Bean的实际名字，只有生成的bean包含这个。
		private String realName = "";

		public final IntHashMap<Variable> getVariables() {
			return variables;
		}

		public final int getKeyRefCount() {
			return keyRefCount;
		}

		public final void setKeyRefCount(int value) {
			keyRefCount = value;
		}

		public final boolean getDeleted() {
			return deleted;
		}

		public final String getRealName() {
			return realName;
		}

		private void setRealName(String value) {
			realName = value;
		}

		public Bean() {
		}

		public Bean(String name, boolean isBeanKey) {
			this.name = name;
			this.isBeanKey = isBeanKey;
		}

		/**
		 * var可能增加，也可能删除，所以兼容仅判断var.id相同的。
		 * 并且和谁比较谁没有关系。
		 *
		 * @param other another Type
		 * @return true: compatible
		 */
		@Override
		public boolean isCompatible(Type other, Context context, Action1<Bean> Update, Action1<Bean> UpdateVariable) {
			if (other == null)
				return false;

			if (!(other instanceof Bean))
				return false;

			Bean beanOther = (Bean)other;

			CheckResult result = context.getCheckResult(beanOther, this);
			if (null != result) {
				result.addUpdate(Update, UpdateVariable);
				return true;
			}
			result = new CheckResult(); // result在后面可能被更新。
			result.setBean(this);
			context.addCheckResult(beanOther, this, result);

			ArrayList<Variable> deleteds = new ArrayList<>();
			for (var it = beanOther.getVariables().iterator(); it.moveToNext(); ) {
				var vOther = it.value();
				var vThis = getVariables().get(vOther.id);
				if (null != vThis) {
					if (vThis.deleted) {
						// bean 可能被多个地方使用，前面比较的时候，创建或者复制了被删除的变量。
						// 所以可能存在已经被删除var，这个时候忽略比较就行了。
						continue;
					}
					if (vOther.deleted) {
						if (context.getConfig().getAllowSchemasReuseVariableIdWithSameType() && vThis.isCompatible(vOther, context)) {
							// 反悔
							continue;
						}
						// 重用了已经被删除的var。此时vOther.Type也是null。
						logger.error("Not Compatible. bean={} variable={} Can Not Reuse Deleted Variable.Id", name, vThis.name);
						return false;
					}
					if (!vThis.isCompatible(vOther, context)) {
						logger.error("Not Compatible. bean={} variable={}", name, vOther.name);
						return false;
					}
				} else {
					// 新删除或以前删除的都创建一个新的。
					Variable tempVar2 = new Variable();
					tempVar2.id = vOther.id;
					tempVar2.name = vOther.name;
					tempVar2.typeName = vOther.typeName;
					tempVar2.keyName = vOther.keyName;
					tempVar2.valueName = vOther.valueName;
					tempVar2.type = vOther.type;
					tempVar2.deleted = true;
					deleteds.add(tempVar2);
				}
			}
			// 限制beankey的var只能增加，不能减少。
			// 如果发生了Bean和BeanKey改变，忽略这个检查。
			// 如果没有被真正当作Key，忽略这个检查。
			if (isBeanKey && getKeyRefCount() > 0 && beanOther.isBeanKey && beanOther.getKeyRefCount() > 0) {
				if (getVariables().size() < beanOther.getVariables().size()) {
					logger.error("Not Compatible. beankey={} Variables.Count < DB.Variables.Count,Must Be Reduced", name);
					return false;
				}
				for (var it = beanOther.getVariables().iterator(); it.moveToNext(); ) {
					var vOther = it.value();
					if (vOther.deleted) {
						// 当作Key前允许删除变量，所以可能存在已经被删除的变量。
						continue;
					}
					if (!getVariables().containsKey(vOther.id)) {
						// 被当作Key以后就不能再删除变量了。
						logger.error("Not Compatible. beankey={} variable={} Not Exist", name, vOther.name);
						return false;
					}
				}
			}

			if (!deleteds.isEmpty()) {
				Bean newBean = ShadowCopy(context);
				context.getCurrent().addBean(newBean);
				result.setBean(newBean);
				result.addUpdate(Update, UpdateVariable);
				for (var vDelete : deleteds) {
					vDelete.tryCopyBeanIfRemoved(context);
					newBean.getVariables().put(vDelete.id, vDelete);
				}
			}
			return true;
		}

		@Override
		public void tryCopyBeanIfRemoved(Context context,
										 Action1<Bean> Update,
										 Action1<Bean> UpdateVariable) {
			CheckResult result = context.getCopyBeanIfRemovedResult(this);
			if (null != result) {
				result.addUpdate(Update, UpdateVariable);
				return;
			}
			result = new CheckResult();
			result.setBean(this);
			context.addCopyBeanIfRemovedResult(this, result);

			if (name.startsWith("_")) {
				// bean 是内部创建的，可能是原来删除的，也可能是合并改名引起的。
				if (context.getCurrent().beans.containsKey(getRealName())) {
					return;
				}

				var newBean = ShadowCopy(context);
				newBean.setRealName(getRealName()); // 原来是新建的Bean，要使用这个。
				context.getCurrent().addBean(newBean);
				result.setBean(newBean);
				result.addUpdate(Update, UpdateVariable);
				return;
			}

			// 通过查找当前Schemas来发现RefZero。
			if (context.getCurrent().beans.containsKey(name)) {
				return;
			}

			var newBean2 = ShadowCopy(context);
			newBean2.deleted = true;
			context.getCurrent().addBean(newBean2);
			result.setBean(newBean2);
			result.addUpdate(Update, UpdateVariable);

			getVariables().foreachValue(v -> v.tryCopyBeanIfRemoved(context));
		}

		private Bean ShadowCopy(Context context) {
			var newBean = new Bean();
			newBean.name = context.generateUniqueName();
			newBean.isBeanKey = this.isBeanKey;
			newBean.keyRefCount = this.getKeyRefCount();
			newBean.realName = this.name;
			newBean.deleted = this.deleted;
			getVariables().foreachValue(v -> newBean.getVariables().put(v.id, v));
			return newBean;
		}

		@Override
		public void decode(ByteBuffer bb) {
			name = bb.ReadString();
			isBeanKey = bb.ReadBool();
			deleted = bb.ReadBool();
			realName = bb.ReadString();
			for (int count = bb.ReadInt(); count > 0; --count) {
				var v = new Variable();
				v.decode(bb);
				getVariables().put(v.id, v);
			}
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteString(name);
			bb.WriteBool(isBeanKey);
			bb.WriteBool(deleted);
			bb.WriteString(realName);
			bb.WriteInt(getVariables().size());
			getVariables().foreachValue(v -> v.encode(bb));
		}

		@Override
		public void compile(Schemas s) {
			getVariables().foreachValue(v -> v.compile(s));
		}

		public final void addVariable(Variable var) {
			getVariables().put(var.id, var);
		}

		@Override
		public void buildRelationalColumns(Table table, Bean bean, Variable variable,
										   ArrayList<String> varNames, ArrayList<Column> columns) {
			variables.foreach((key, value) -> {
				varNames.add(value.name);
				if (value.type.key != null || value.type.value != null) // is collection or map
					value.type.buildRelationalColumns(table, this, value, varNames, columns); // 实际上单独判断了也不需要特别处理。先明确写一下。
				else
					value.type.buildRelationalColumns(table, this, value, varNames, columns);
				varNames.remove(varNames.size() - 1);
			});
		}
	}

	public static class Table implements Serializable {
		public String name; // FullName, sample: demo_Module1_Table1
		public String keyName;
		public String valueName;
		public Type keyType;
		public Type valueType;

		public Table() {
		}

		public Table(String n, String k, String v) {
			name = n;
			keyName = k;
			valueName = v;
		}

		@Override
		public void decode(ByteBuffer bb) {
			name = bb.ReadString();
			keyName = bb.ReadString();
			valueName = bb.ReadString();
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteString(name);
			bb.WriteString(keyName);
			bb.WriteString(valueName);
		}

		public boolean isCompatible(Table other, Context context) {
			return name.equals(other.name)
					&& keyType.isCompatible(other.keyType, context,
					(bean) ->
					{
						keyName = bean.name;
						keyType = bean;
					},
					null)
					&& valueType.isCompatible(other.valueType, context,
					(bean) ->
					{
						valueName = bean.name;
						valueType = bean;
					},
					null);
		}

		public void compile(Schemas s) {
			keyType = s.compile(keyName, "", "");
			if (keyType instanceof Bean) {
				((Bean)keyType).keyRefCount++;
			}
			valueType = s.compile(valueName, "", "");
		}

		public void buildRelationalColumns(ArrayList<Column> columns) {
			var varNames = new ArrayList<String>();
			valueType.buildRelationalColumns(this, null, null, varNames, columns);
		}
	}

	public final HashMap<String, Table> tables = new HashMap<>();
	public final HashMap<String, Bean> beans = new HashMap<>();
	private final HashMap<String, Type> basicTypes = new HashMap<>();

	public void checkCompatible(Schemas other, Application app) throws Exception {
		if (other == null)
			return;

		var context = new Context();
		{
			context.setCurrent(this);
			context.setPrevious(other);
			context.setConfig(app.getConfig());
		}

		for (var table : tables.values()) {
			var zTable = app.getTable(table.name);
			if (zTable == null || zTable.isNew() || app.getConfig().autoResetTable())
				continue;
			var otherTable = other.tables.get(table.name);
			if (null != otherTable) {
				if (!table.isCompatible(otherTable, context))
					throw new IllegalStateException("Not Compatible Table=" + table.name);
			}
		}
		context.update();
	}

	@Override
	public void decode(ByteBuffer bb) {
		for (int count = bb.ReadInt(); count > 0; --count) {
			var table = new Table();
			table.decode(bb);
			if (null != tables.put(table.name, table))
				throw new IllegalStateException("duplicate table=" + table.name);
		}
		for (int count = bb.ReadInt(); count > 0; --count) {
			var bean = new Bean();
			bean.decode(bb);
			if (null != beans.put(bean.name, bean))
				throw new IllegalStateException("duplicate bean=" + bean.name);
		}
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(tables.size());
		for (var table : tables.values())
			table.encode(bb);
		bb.WriteInt(beans.size());
		for (var bean : beans.values())
			bean.encode(bb);
	}

	public void compile() {
		for (var table : tables.values())
			table.compile(this);
		for (var bean : beans.values())
			bean.compile(this);
	}

	public Type compile(String type, String key, String value) {
		if (type == null || type.isEmpty())
			return null;

		var beanExist = beans.get(type);
		if (beanExist != null)
			return beanExist;

		var fullTypeName = type + ":" + key + ":" + value;

		// 除了Bean，其他基本类型和容器类型都动态创建。
		var typeExist = basicTypes.get(fullTypeName);
		if (typeExist != null)
			return typeExist;

		var n = new Type();
		{
			n.name = type;
			n.keyName = key;
			n.valueName = value;
		}
		basicTypes.put(fullTypeName, n);
		n.compile(this); // 容器需要编译。这里的时机不是太好。
		return n;
	}

	public void addBean(Bean bean) {
		if (beans.put(bean.name, bean) != null)
			throw new IllegalStateException("AddBean duplicate=" + bean.name);
	}

	public void addTable(Table table) {
		if (tables.put(table.name, table) != null)
			throw new IllegalStateException("AddTable duplicate=" + table.name);
	}

	public final HashMap<String, RelationalTable> relationalTables = new HashMap<>();

	public static class Column {
		public String name;
		public Table table;
		public Bean bean;
		public Variable variable;
		public String sqlType;
		public Column change;

		@Override
		public String toString() {
			return name + ":" + variable.id;
		}

		public Column(String name, Table table, Bean bean, Variable variable, String sqlType) {
			this.name = name;
			this.table = table;
			this.bean = bean;
			this.variable = variable;
			this.sqlType = sqlType;
		}
	}

	static class ColumnComparator implements Comparator<Column> {

		private static int compare(Bean o1, Bean o2) {
			if (o1 == null) {
				if (o2 == null)
					return 0;
				return -1;
			}
			if (o2 == null)
				return 1;
			return Integer.compare(System.identityHashCode(o1), System.identityHashCode(o2));
		}

		@Override
		public int compare(Column o1, Column o2) {
			var c = compare(o1.bean, o2.bean);
			if (c != 0)
				return c;
			c = Integer.compare(o1.variable.id, o2.variable.id);
			return c;
		}
	}

	public static class RelationalTable {
		public final ArrayList<Column> current = new ArrayList<>();
		public final ArrayList<Column> previous = new ArrayList<>();
		public final ArrayList<Column> change = new ArrayList<>();
		public final ArrayList<Column> add = new ArrayList<>();
		public final ArrayList<Column> remove = new ArrayList<>();
		public final String keyColumns = ""; // todo

		public String createTableSql(String name) {
			if (current.isEmpty())
				throw new RuntimeException("no column");
			var sb = new StringBuilder();
			sb.append("CREATE TABLE IF NOT EXISTS ").append(name).append("(");
			for (var c : current) {
				sb.append(c.name).append(" ").append(c.sqlType).append(",");
			}
			sb.append("PRIMARY KEY(");
			sb.append(keyColumns);
			sb.append(")");
			return sb.toString();
		}

		private boolean equals(Column a, Column b) {
			if (!a.name.equals(b.name))
				return false;
			return a.variable.type.name.equals(b.variable.type.name);
		}

		public void diff() {
			var comparator = new ColumnComparator();
			current.sort(comparator);
			previous.sort(comparator);

			var itCur = current.iterator();
			var itPre = previous.iterator();
			if (itCur.hasNext() && itPre.hasNext()) {
				var eCur = itCur.next();
				var ePre = itPre.next();
				while (true) {
					var c = comparator.compare(eCur, ePre);
					if (c == 0) {
						if (!equals(eCur, ePre)) {
							eCur.change = ePre;
							change.add(eCur);
						}

						// fetch both
						if (itCur.hasNext() && itPre.hasNext()) {
							eCur = itCur.next();
							ePre = itPre.next();
							continue;
						}
						break;
					}
					if (c < 0) {
						add.add(eCur);
						if (!itCur.hasNext()) {
							remove.add(ePre);
							break;
						}
						eCur = itCur.next();
						continue;
					}
					remove.add(ePre);
					if (!itPre.hasNext()) {
						add.add(eCur);
						break;
					}
					ePre = itPre.next();
				}
			}
			while (itCur.hasNext()) {
				var eCur = itCur.next();
				add.add(eCur);
			}
			while (itPre.hasNext()) {
				var ePre = itPre.next();
				remove.add(ePre);
			}
		}
	}

	public void buildRelationalTables(Application zeze, Schemas other) {
		for (var db : zeze.getDatabases().values()) {
			for (var table : db.getTables()) {
				if (table.isRelationalMapping()) {
					var tableName = table.getName();
					var relational = new RelationalTable();
					var cur = this.tables.get(tableName);
					cur.buildRelationalColumns(relational.current);
					System.out.println(relational.createTableSql(tableName));
					relationalTables.put(tableName, relational);

					// build other. prepare to alter.
					if (null != other) {
						var pre = other.tables.get(tableName);
						if (pre != null) // is null if new table
							pre.buildRelationalColumns(relational.previous);
					}
				}
			}
		}
	}
}
