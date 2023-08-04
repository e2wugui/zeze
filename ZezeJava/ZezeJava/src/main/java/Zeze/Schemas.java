package Zeze;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Action1;
import Zeze.Util.KV;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
	public static final class Checked {
		private Bean previous;
		private Bean current;

		public Bean getPrevious() {
			return previous;
		}

		public void setPrevious(Bean value) {
			previous = value;
		}

		public Bean getCurrent() {
			return current;
		}

		public void setCurrent(Bean value) {
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
		public boolean equals(@Nullable Object obj) {
			if (this == obj) {
				return true;
			}
			boolean tempVar = obj instanceof Checked;
			Checked other = tempVar ? (Checked)obj : null;
			return (tempVar) && getPrevious() == other.getPrevious() && getCurrent() == other.getCurrent();
		}
	}

	public static final class CheckResult {
		private Bean bean;
		private final ArrayList<Action1<Bean>> updates = new ArrayList<>();
		private final ArrayList<Action1<Bean>> updateVariables = new ArrayList<>();

		public Bean getBean() {
			return bean;
		}

		public void setBean(Bean value) {
			bean = value;
		}

		private @NotNull ArrayList<Action1<Bean>> getUpdates() {
			return updates;
		}

		private @NotNull ArrayList<Action1<Bean>> getUpdateVariables() {
			return updateVariables;
		}

		public void addUpdate(@NotNull Action1<Bean> Update, @Nullable Action1<Bean> UpdateVariable) {
			getUpdates().add(Update);
			if (null != UpdateVariable) {
				getUpdateVariables().add(UpdateVariable);
			}
		}

		public void update() throws Exception {
			for (var update : getUpdates()) {
				update.run(getBean());
			}
			for (var update : getUpdateVariables()) {
				update.run(getBean());
			}
		}
	}

	public static final class Context {
		private Schemas current;
		private Schemas previous;
		private final HashMap<Checked, CheckResult> checked = new HashMap<>();
		private final HashMap<Bean, CheckResult> copyBeanIfRemoved = new HashMap<>();
		private Config config;
		private long renameCount;

		public Schemas getCurrent() {
			return current;
		}

		public void setCurrent(Schemas value) {
			current = value;
		}

		public Schemas getPrevious() {
			return previous;
		}

		public void setPrevious(Schemas value) {
			previous = value;
		}

		public @NotNull HashMap<Checked, CheckResult> getChecked() {
			return checked;
		}

		public @NotNull HashMap<Bean, CheckResult> getCopyBeanIfRemoved() {
			return copyBeanIfRemoved;
		}

		public Config getConfig() {
			return config;
		}

		public void setConfig(Config value) {
			config = value;
		}

		public @Nullable CheckResult getCheckResult(Bean previous, Bean current) {
			Schemas.Checked tempVar = new Schemas.Checked();
			tempVar.setPrevious(previous);
			tempVar.setCurrent(current);
			return getChecked().get(tempVar);
		}

		public void addCheckResult(Bean previous, Bean current, @NotNull CheckResult result) {
			Schemas.Checked tempVar = new Schemas.Checked();
			tempVar.setPrevious(previous);
			tempVar.setCurrent(current);
			if (null != getChecked().put(tempVar, result))
				throw new IllegalStateException("duplicate var in Checked Map");
		}

		public @Nullable CheckResult getCopyBeanIfRemovedResult(@NotNull Bean bean) {
			return getCopyBeanIfRemoved().get(bean);
		}

		public void addCopyBeanIfRemovedResult(@NotNull Bean bean, @NotNull CheckResult result) {
			if (null != getCopyBeanIfRemoved().put(bean, result))
				throw new IllegalStateException("duplicate bean in CopyBeanIfRemoved Map");
		}

		public void update() throws Exception {
			for (var result : getChecked().values())
				result.update();
			for (var result : getCopyBeanIfRemoved().values())
				result.update();
		}

		public @NotNull String generateUniqueName() {
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
			compatibleTable.put("array", 3);
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

		private static boolean isTypeNameCompatible(@NotNull String typeName0, @NotNull String typeName1) {
			if (typeName0.equals(typeName1))
				return true;
			var t0 = compatibleTable.get(typeName0);
			var t1 = compatibleTable.get(typeName1);
			return t0 != null && t0.equals(t1);
		}

		public boolean isCompatible(@Nullable Type other, @NotNull Context context,
									@NotNull Action1<Bean> Update, @Nullable Action1<Bean> UpdateVariable) {
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
		public void decode(@NotNull ByteBuffer bb) {
			name = bb.ReadString();
			keyName = bb.ReadString();
			valueName = bb.ReadString();
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteString(name);
			bb.WriteString(keyName);
			bb.WriteString(valueName);
		}

		public void compile(@NotNull Schemas s) {
			key = s.compile(keyName, "", "");
			if (key != null && key instanceof Bean)
				((Bean)key).keyRefCount++;

			value = s.compile(valueName, "", "");
			if (value != null && name.equals("set") && value instanceof Bean)
				((Bean)value).keyRefCount++;
		}

		public void tryCopyBeanIfRemoved(@NotNull Context context,
										 @NotNull Action1<Bean> Update,
										 @Nullable Action1<Bean> UpdateVariable) {
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
			sqlTypeTable.put("binary", "BLOB");
			sqlTypeTable.put("string", "TEXT");
			// json
			sqlTypeTable.put("dynamic", "TEXT");
			sqlTypeTable.put("list", "TEXT");
			sqlTypeTable.put("array", "TEXT");
			sqlTypeTable.put("set", "TEXT");
			sqlTypeTable.put("map", "TEXT");
			// 下面的类型会被展开，这里的类型展开后的实际类型。
			sqlTypeTable.put("vector2", "FLOAT");
			sqlTypeTable.put("vector2int", "INT");
			sqlTypeTable.put("vector3", "FLOAT");
			sqlTypeTable.put("vector3int", "INT");
			sqlTypeTable.put("vector4", "FLOAT");
			sqlTypeTable.put("quaternion", "FLOAT");
		}

		public @NotNull String toSqlType(boolean isKey) {
			var sqlType = sqlTypeTable.get(name);
			if (null == sqlType)
				throw new UnsupportedOperationException("unknown sql type=" + name);
			if (name.equals("string") && isKey)
				return "VARCHAR(256)";
			return sqlType;
		}

		public static @NotNull String toColumnName(@NotNull ArrayList<String> varNames) {
			var sb = new StringBuilder();
			sb.append(varNames.get(0));
			for (int i = 1; i < varNames.size(); ++i)
				sb.append("_").append(varNames.get(i));
			return sb.toString();
		}

		public static @NotNull String toColumnName(@NotNull ArrayList<String> varNames, @NotNull String lastName) {
			var sb = new StringBuilder();
			sb.append(varNames.get(0));
			for (int i = 1; i < varNames.size(); ++i)
				sb.append("_").append(varNames.get(i));
			sb.append("_").append(lastName);
			return sb.toString();
		}

		public static int @NotNull [] toVarIds(@NotNull ArrayList<Integer> varIds, int lastId) {
			var ids = new int[varIds.size() + 1];
			for (var i = 0; i < varIds.size(); ++i)
				ids[i] = varIds.get(i);
			ids[ids.length - 1] = lastId;
			return ids;
		}

		public void buildRelationalColumns(boolean isKey, Table table, Bean bean, Variable variable,
										   @NotNull ArrayList<String> varNames, @NotNull ArrayList<Integer> varIds,
										   @NotNull ArrayList<Column> columns) {
			switch (name) {
			case "vector2":
			case "vector2int":
				// int or float 由toSqlType的结果区分，其他都一样。
				columns.add(new Column(toColumnName(varNames, "x"), toVarIds(varIds, 1), table, bean, variable, toSqlType(isKey)));
				columns.add(new Column(toColumnName(varNames, "y"), toVarIds(varIds, 2), table, bean, variable, toSqlType(isKey)));
				break;
			case "vector3":
			case "vector3int":
				// int or float 由toSqlType的结果区分，其他都一样。
				columns.add(new Column(toColumnName(varNames, "x"), toVarIds(varIds, 1), table, bean, variable, toSqlType(isKey)));
				columns.add(new Column(toColumnName(varNames, "y"), toVarIds(varIds, 2), table, bean, variable, toSqlType(isKey)));
				columns.add(new Column(toColumnName(varNames, "z"), toVarIds(varIds, 3), table, bean, variable, toSqlType(isKey)));
				break;
			case "vector4":
			case "quaternion":
				columns.add(new Column(toColumnName(varNames, "x"), toVarIds(varIds, 1), table, bean, variable, toSqlType(isKey)));
				columns.add(new Column(toColumnName(varNames, "y"), toVarIds(varIds, 2), table, bean, variable, toSqlType(isKey)));
				columns.add(new Column(toColumnName(varNames, "z"), toVarIds(varIds, 3), table, bean, variable, toSqlType(isKey)));
				columns.add(new Column(toColumnName(varNames, "w"), toVarIds(varIds, 4), table, bean, variable, toSqlType(isKey)));
				break;

			default:
				var ids = new int[varIds.size()];
				for (var i = 0; i < varIds.size(); ++i)
					ids[i] = varIds.get(i);
				columns.add(new Column(toColumnName(varNames), ids, table, bean, variable, toSqlType(isKey)));
				break;
			}
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

		@Override
		public void decode(@NotNull ByteBuffer bb) {
			id = bb.ReadInt();
			name = bb.ReadString();
			typeName = bb.ReadString();
			keyName = bb.ReadString();
			valueName = bb.ReadString();
			deleted = bb.ReadBool();
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteInt(id);
			bb.WriteString(name);
			bb.WriteString(typeName);
			bb.WriteString(keyName);
			bb.WriteString(valueName);
			bb.WriteBool(deleted);
		}

		public void compile(@NotNull Schemas s) {
			type = s.compile(typeName, keyName, valueName);
		}

		public boolean isCompatible(@NotNull Variable other, @NotNull Context context) {
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

		private final TreeMap<Integer, Variable> variables = new TreeMap<>();
		private boolean isBeanKey;
		private int keyRefCount;
		// 这个变量当前是不需要的，作为额外的属性记录下来，以后可能要用。
		private boolean deleted;
		// 这里记录在当前版本Schemas中Bean的实际名字，只有生成的bean包含这个。
		private String realName = "";

		public final @NotNull TreeMap<Integer, Variable> getVariables() {
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
		public boolean isCompatible(@Nullable Type other, @NotNull Context context, @NotNull Action1<Bean> Update,
									@Nullable Action1<Bean> UpdateVariable) {
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
			for (var e : beanOther.getVariables().entrySet()) {
				var vOther = e.getValue();
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
				for (var e : beanOther.getVariables().entrySet()) {
					var vOther = e.getValue();
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
		public void tryCopyBeanIfRemoved(@NotNull Context context,
										 @NotNull Action1<Bean> Update,
										 @Nullable Action1<Bean> UpdateVariable) {
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

			for (var v : getVariables().values())
				v.tryCopyBeanIfRemoved(context);
		}

		private @NotNull Bean ShadowCopy(@NotNull Context context) {
			var newBean = new Bean();
			newBean.name = context.generateUniqueName();
			newBean.isBeanKey = this.isBeanKey;
			newBean.keyRefCount = this.getKeyRefCount();
			newBean.realName = this.name;
			newBean.deleted = this.deleted;
			for (var v : getVariables().values())
				newBean.getVariables().put(v.id, v);
			return newBean;
		}

		@Override
		public void decode(@NotNull ByteBuffer bb) {
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
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteString(name);
			bb.WriteBool(isBeanKey);
			bb.WriteBool(deleted);
			bb.WriteString(realName);
			bb.WriteInt(getVariables().size());
			for (var v : getVariables().values())
				v.encode(bb);
		}

		@Override
		public void compile(@NotNull Schemas s) {
			for (var v : getVariables().values())
				v.compile(s);
		}

		public final void addVariable(@NotNull Variable var) {
			getVariables().put(var.id, var);
		}

		@Override
		public void buildRelationalColumns(boolean isKey, Table table, Bean bean, Variable variable,
										   @NotNull ArrayList<String> varNames, @NotNull ArrayList<Integer> varIds,
										   @NotNull ArrayList<Column> columns) {
			for (var e : variables.entrySet()) {
				// var key = e.getKey();
				var value = e.getValue();
				varNames.add(value.name);
				varIds.add(value.id);
				if (value.type.key != null || value.type.value != null) // is collection or map
					// 实际上单独判断了也不需要特别处理。先明确写一下。
					value.type.buildRelationalColumns(isKey, table, this, value, varNames, varIds, columns);
				else
					value.type.buildRelationalColumns(isKey, table, this, value, varNames, varIds, columns);
				varIds.remove(varIds.size() - 1);
				varNames.remove(varNames.size() - 1);
			}
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
		public void decode(@NotNull ByteBuffer bb) {
			name = bb.ReadString();
			keyName = bb.ReadString();
			valueName = bb.ReadString();
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteString(name);
			bb.WriteString(keyName);
			bb.WriteString(valueName);
		}

		public boolean isCompatible(@NotNull Table other, @NotNull Context context) {
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

		public void compile(@NotNull Schemas s) {
			keyType = s.compile(keyName, "", "");
			if (keyType instanceof Bean) {
				((Bean)keyType).keyRefCount++;
			}
			valueType = s.compile(valueName, "", "");
		}

		public @NotNull String buildRelationalColumns(@NotNull ArrayList<Column> columns) {
			String keyColumns;

			// 构造一个虚拟变量。Column需要用到。
			var varKey = new Variable();
			varKey.name = "__key";
			varKey.id = 1;
			varKey.typeName = keyType.name;
			varKey.type = keyType;
			if (keyType instanceof Bean) {
				// 肯定是BeanKey。
				var varNames = new ArrayList<String>();
				varNames.add(varKey.name);
				var varIds = new ArrayList<Integer>();
				varIds.add(varKey.id);
				keyType.buildRelationalColumns(true, this, null, varKey, varNames, varIds, columns);
				columns.sort(new ColumnComparator());
				var sb = new StringBuilder();
				for (var column : columns) {
					if (sb.length() > 0)
						sb.append(", ");
					sb.append(column.name);
				}
				keyColumns = sb.toString();
			} else {
				keyColumns = varKey.name;
				var varIds = new int[]{varKey.id};
				columns.add(new Column(varKey.name, varIds, this, null, varKey, keyType.toSqlType(true)));
			}
			var varNames = new ArrayList<String>();
			var varIds = new ArrayList<Integer>();
			varIds.add(2);
			valueType.buildRelationalColumns(false, this, null, null, varNames, varIds, columns);

			// 构建好就排序，不能再diff的时候排序，有可能diff不会被调用。
			var comparator = new ColumnComparator();
			columns.sort(comparator);

			return keyColumns;
		}
	}

	public final TreeMap<String, Table> tables = new TreeMap<>();
	public final TreeMap<String, Bean> beans = new TreeMap<>();
	private final TreeMap<String, Type> basicTypes = new TreeMap<>();

	private int appPublishVersion = 0;

	public int getAppPublishVersion() {
		return appPublishVersion;
	}

	public void setAppPublishVersion(int version) {
		appPublishVersion = version;
	}

	public void checkCompatible(@Nullable Schemas other, @NotNull Application app) throws Exception {
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
	public void decode(@NotNull ByteBuffer bb) {
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
		// 新增的appPublishVersion在旧版里面可能没有。
		if (bb.WriteIndex > bb.ReadIndex) {
			appPublishVersion = bb.ReadInt();
		}
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteInt(tables.size());
		for (var table : tables.values())
			table.encode(bb);
		bb.WriteInt(beans.size());
		for (var bean : beans.values())
			bean.encode(bb);
		bb.WriteInt(appPublishVersion);
	}

	public void compile() {
		for (var table : tables.values())
			table.compile(this);
		for (var bean : beans.values())
			bean.compile(this);
	}

	public @Nullable Type compile(@Nullable String type, @NotNull String key, @NotNull String value) {
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
		public final String name;
		public final int[] varIds;
		public final String sqlType;

		// 辅助信息
		public final Table table;
		public final Bean bean;
		public final Variable variable;

		// diff 时设置，可能。
		public Column change;

		@Override
		public @NotNull String toString() {
			return name + ":" + variable.id;
		}

		public Column(String name, int[] varIds, Table table, Bean bean, Variable variable, String sqlType) {
			this.name = name;
			this.varIds = varIds;
			this.table = table;
			this.bean = bean;
			this.variable = variable;
			this.sqlType = sqlType;
		}
	}

	public static class ColumnComparator implements Comparator<Column> {
		@Override
		public int compare(@NotNull Column o1, @NotNull Column o2) {
			return Arrays.compare(o1.varIds, o2.varIds);
		}
	}

	public static class RelationalTable {
		public final @NotNull String tableName;
		public final ArrayList<Column> current = new ArrayList<>();
		public String currentKeyColumns;
		public final ArrayList<Column> previous = new ArrayList<>();

		// diff 结果
		public final ArrayList<Column> change = new ArrayList<>();
		public final ArrayList<Column> add = new ArrayList<>();
		public final ArrayList<Column> remove = new ArrayList<>();

		public RelationalTable(@NotNull String name) {
			this.tableName = name;
		}

		public @NotNull String createTableSql() {
			if (current.isEmpty())
				throw new IllegalStateException("no column");
			var sb = new StringBuilder();
			sb.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append("(");
			for (var c : current) {
				sb.append(c.name).append(" ").append(c.sqlType).append(",");
			}
			sb.append("PRIMARY KEY(");
			sb.append(currentKeyColumns);
			sb.append(")");
			sb.append(")"); // end table
			return sb.toString();
		}

		private static @NotNull KV<Integer, Integer> catType(@NotNull String type) {
			switch (type) {
			//@formatter:off
			case "bool":
			case "byte": return KV.create(0, 1); // bool<->byte
			case "short": return KV.create(0, 2); // byte->short->int->long->float->double
			case "int": return KV.create(0, 3);
			case "long": return KV.create(0, 4);
			case "float": return KV.create(0, 5);
			case "double": return KV.create(0, 6);

			case "string": return KV.create(1, 1); // string->binary
			case "binary": return KV.create(1, 2);

			case "vector2int":
			case "vector3int":
				return KV.create(2, 1); // 允许互转

			case "vector2":
			case "vector3":
			case "vector4":
			case "quaternion":
				return KV.create(3, 1); // 允许自由互转，返回同一个值即可。

			case "dynamic":
			case "list":
			case "array":
			case "set":
			case "map":
				return KV.create(4, 1); // 这几个类型不是都能互转的。他们的兼容性遵循ByteBuffer的要求，关系映射这里不做检查。
			//@formatter:on
			}
			throw new UnsupportedOperationException("unknown type=" + type);
		}

		// 检查兼容，并返回列是否需要change。
		private static boolean checkCompatibleAndChange(@NotNull Column a, @NotNull Column b) {
			var aType = catType(a.variable.type.name);
			var bType = catType(b.variable.type.name);
			if (!Objects.equals(aType.getKey(), bType.getKey()))
				throw new IllegalStateException("type change not compatible, cat!");
			if (aType.getValue() < bType.getValue())
				throw new IllegalStateException("type change not compatible, type!");

			// change detect
			if (!a.name.equals(b.name))
				return true;
			return !aType.getValue().equals(bType.getValue());
		}

		public void diff() {
			var comparator = new ColumnComparator();
			var itCur = current.iterator();
			var itPre = previous.iterator();
			if (itCur.hasNext() && itPre.hasNext()) {
				var eCur = itCur.next();
				var ePre = itPre.next();
				while (true) {
					var c = comparator.compare(eCur, ePre);
					if (c == 0) {
						if (checkCompatibleAndChange(eCur, ePre)) {
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

	// 根据新旧Schemas.Table信息，新建Schemas.RelationalTable。
	public static Schemas.RelationalTable newRelationalTable(Schemas.Table cur, Schemas.Table other) {
		var relational = new RelationalTable(cur.name);
		relational.currentKeyColumns = cur.buildRelationalColumns(relational.current);
		//System.out.println(relational.createTableSql());

		// build other. prepare to alter.
		// is null if new table
		if (null != other) {
			other.buildRelationalColumns(relational.previous);
			relational.diff();
		}
		return relational;
	}

	// 构建整个应用的需要关系映射的表。
	public void buildRelationalTables(@NotNull Application zeze, @Nullable Schemas other) {
		for (var db : zeze.getDatabases().values()) {
			for (var table : db.getTables()) {
				if (table.isRelationalMapping()) {
					var relational = newRelationalTable(
							this.tables.get(table.getName()),
							other != null ? other.tables.get(table.getName()) : null);
					relationalTables.put(table.getName(), relational);
				}
			}
		}
	}
}
