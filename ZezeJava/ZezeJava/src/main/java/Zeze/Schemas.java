package Zeze;

import java.util.ArrayList;
import java.util.HashMap;
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
		private Bean Previous;

		public final Bean getPrevious() {
			return Previous;
		}

		public final void setPrevious(Bean value) {
			Previous = value;
		}

		private Bean Current;

		public final Bean getCurrent() {
			return Current;
		}

		public final void setCurrent(Bean value) {
			Current = value;
		}

		@Override
		public int hashCode() {
			final int _prime_ = 31;
			int _h_ = 0;
			_h_ = _h_ * _prime_ + getPrevious().Name.hashCode();
			_h_ = _h_ * _prime_ + getCurrent().Name.hashCode();
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
		private Bean Bean;

		public final Bean getBean() {
			return Bean;
		}

		public final void setBean(Bean value) {
			Bean = value;
		}

		private final ArrayList<Action1<Bean>> Updates = new ArrayList<>();

		private ArrayList<Action1<Bean>> getUpdates() {
			return Updates;
		}

		private final ArrayList<Action1<Bean>> UpdateVariables = new ArrayList<>();

		private ArrayList<Action1<Bean>> getUpdateVariables() {
			return UpdateVariables;
		}

		public final void AddUpdate(Action1<Bean> Update, Action1<Bean> UpdateVariable) {
			getUpdates().add(Update);
			if (null != UpdateVariable) {
				getUpdateVariables().add(UpdateVariable);
			}
		}

		public final void Update() throws Throwable {
			for (var update : getUpdates()) {
				update.run(getBean());
			}
			for (var update : getUpdateVariables()) {
				update.run(getBean());
			}
		}
	}

	public static class Context {
		private Schemas Current;

		public final Schemas getCurrent() {
			return Current;
		}

		public final void setCurrent(Schemas value) {
			Current = value;
		}

		private Schemas Previous;

		public final Schemas getPrevious() {
			return Previous;
		}

		public final void setPrevious(Schemas value) {
			Previous = value;
		}

		private final HashMap<Checked, CheckResult> Checked = new HashMap<>();

		public final HashMap<Checked, CheckResult> getChecked() {
			return Checked;
		}

		private final HashMap<Bean, CheckResult> CopyBeanIfRemoved = new HashMap<>();

		public final HashMap<Bean, CheckResult> getCopyBeanIfRemoved() {
			return CopyBeanIfRemoved;
		}

		private Zeze.Config Config;

		public final Zeze.Config getConfig() {
			return Config;
		}

		public final void setConfig(Zeze.Config value) {
			Config = value;
		}

		public final CheckResult GetCheckResult(Bean previous, Bean current) {
			Schemas.Checked tempVar = new Schemas.Checked();
			tempVar.setPrevious(previous);
			tempVar.setCurrent(current);
			return getChecked().get(tempVar);
		}

		public final void AddCheckResult(Bean previous, Bean current, CheckResult result) {
			Schemas.Checked tempVar = new Schemas.Checked();
			tempVar.setPrevious(previous);
			tempVar.setCurrent(current);
			if (null != getChecked().put(tempVar, result))
				throw new IllegalStateException("duplicate var in Checked Map");
		}

		public final CheckResult GetCopyBeanIfRemovedResult(Bean bean) {
			return getCopyBeanIfRemoved().get(bean);
		}

		public final void AddCopyBeanIfRemovedResult(Bean bean, CheckResult result) {
			if (null != getCopyBeanIfRemoved().put(bean, result))
				throw new IllegalStateException("duplicate bean in CopyBeanIfRemoved Map");
		}

		public final void Update() throws Throwable {
			for (var result : getChecked().values()) {
				result.Update();
			}
			for (var result : getCopyBeanIfRemoved().values()) {
				result.Update();
			}
		}

		private long ReNameCount = 0;

		public final String GenerateUniqueName() {
			++ReNameCount;
			return "_" + ReNameCount;
		}
	}

	public static class Type implements Serializable {
		public String Name;
		public String KeyName = "";
		public String ValueName = "";
		public Type Key;
		public Type Value;

		public boolean IsCompatible(Type other, Context context,
									Action1<Bean> Update,
									Action1<Bean> UpdateVariable) {
			if (other == this) {
				return true;
			}

			if (other == null) {
				return false;
			}

			if (!Name.equals(other.Name)) {
				return false;
			}

			// Name 相同的情况下，下面的 Key Value 仅在 Collection 时有值。
			// 当 this.Key == null && other.Key != null 在 Name 相同的情况下是不可能发生的。
			if (null != Key) {
				if (!Key.IsCompatible(other.Key, context,
						(bean) -> {
							KeyName = bean.Name;
							Key = bean;
						}, UpdateVariable)) {
					return false;
				}
			} else if (other.Key != null) {
				throw new IllegalStateException("(this.Key == null && other.Key != null) Impossible!");
			}

			if (null != Value) {
				return Value.IsCompatible(other.Value, context,
						(bean) -> {
							ValueName = bean.Name;
							Value = bean;
						}, UpdateVariable);
			}
			if (other.Value != null) {
				throw new IllegalStateException("(this.Value == null && other.Value != null) Impossible!");
			}

			return true;
		}

		@Override
		public void decode(ByteBuffer bb) {
			Name = bb.ReadString();
			KeyName = bb.ReadString();
			ValueName = bb.ReadString();
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteString(Name);
			bb.WriteString(KeyName);
			bb.WriteString(ValueName);
		}

		public void Compile(Schemas s) {
			Key = s.Compile(KeyName, "", "");
			if (null != Key && Key instanceof Bean) {
				((Bean)Key).KeyRefCount++;
			}

			Value = s.Compile(ValueName, "", "");
			if (null != Value) {
				if (Name.equals("set") && Value instanceof Bean)
					((Bean)Value).KeyRefCount++;
			}
		}

		public void TryCopyBeanIfRemoved(Context context,
										 Action1<Bean> Update,
										 Action1<Bean> UpdateVariable) {
			if (null != Key)
				Key.TryCopyBeanIfRemoved(context,
						(bean) ->
						{
							KeyName = bean.Name;
							Key = bean;
						},
						UpdateVariable);

			if (null != Value)
				Value.TryCopyBeanIfRemoved(context,
						(bean) ->
						{
							ValueName = bean.Name;
							Value = bean;
						},
						UpdateVariable);
		}
	}

	public static class Variable implements Serializable {
		public int Id;
		public String Name;
		public String TypeName;
		public String KeyName = "";
		public String ValueName = "";
		public Type Type;
		public boolean Deleted = false;

		public Variable() {

		}

		@Override
		public void decode(ByteBuffer bb) {
			Id = bb.ReadInt();
			Name = bb.ReadString();
			TypeName = bb.ReadString();
			KeyName = bb.ReadString();
			ValueName = bb.ReadString();
			Deleted = bb.ReadBool();
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(Id);
			bb.WriteString(Name);
			bb.WriteString(TypeName);
			bb.WriteString(KeyName);
			bb.WriteString(ValueName);
			bb.WriteBool(Deleted);
		}

		public void Compile(Schemas s) {
			Type = s.Compile(TypeName, KeyName, ValueName);
		}

		public boolean IsCompatible(Variable other, Context context) {
			return this.Type.IsCompatible(other.Type, context,
					(bean) ->
					{
						TypeName = bean.Name;
						Type = bean;
					},
					(bean) ->
					{
						KeyName = Type.KeyName;
						ValueName = Type.ValueName;
					});
		}

		public final void Update() {
			KeyName = this.Type.KeyName;
			ValueName = this.Type.ValueName;
		}

		public final void TryCopyBeanIfRemoved(Context context) {
			this.Type.TryCopyBeanIfRemoved(context, (bean) -> {
				TypeName = bean.Name;
				Type = bean;
			}, (bean) -> {
				KeyName = Type.KeyName;
				ValueName = Type.ValueName;
			});
		}
	}

	public static class Bean extends Type {
		private static final Logger logger = LogManager.getLogger(Bean.class);

		private final IntHashMap<Variable> Variables = new IntHashMap<>();

		public final IntHashMap<Variable> getVariables() {
			return Variables;
		}

		private boolean IsBeanKey = false;
		private int KeyRefCount = 0;

		public final int getKeyRefCount() {
			return KeyRefCount;
		}

		public final void setKeyRefCount(int value) {
			KeyRefCount = value;
		}

		// 这个变量当前是不需要的，作为额外的属性记录下来，以后可能要用。
		private boolean Deleted = false;

		public final boolean getDeleted() {
			return Deleted;
		}

		// 这里记录在当前版本Schemas中Bean的实际名字，只有生成的bean包含这个。
		private String RealName = "";

		public final String getRealName() {
			return RealName;
		}

		private void setRealName(String value) {
			RealName = value;
		}

		public Bean() {

		}

		public Bean(String name, boolean isBeanKey) {
			Name = name;
			IsBeanKey = isBeanKey;
		}

		/**
		 * var可能增加，也可能删除，所以兼容仅判断var.id相同的。
		 * 并且和谁比较谁没有关系。
		 *
		 * @param other another Type
		 * @return true: compatible
		 */
		@Override
		public boolean IsCompatible(Type other, Context context, Action1<Bean> Update, Action1<Bean> UpdateVariable) {
			if (other == null) {
				return false;
			}

			if (!(other instanceof Bean))
				return false;

			Bean beanOther = (Bean)other;

			CheckResult result = context.GetCheckResult(beanOther, this);
			if (null != result) {
				result.AddUpdate(Update, UpdateVariable);
				return true;
			}
			result = new CheckResult(); // result在后面可能被更新。
			result.setBean(this);
			context.AddCheckResult(beanOther, this, result);

			ArrayList<Variable> Deleteds = new ArrayList<>();
			for (var it = beanOther.getVariables().iterator(); it.moveToNext(); ) {
				var vOther = it.value();
				var vThis = getVariables().get(vOther.Id);
				if (null != vThis) {
					if (vThis.Deleted) {
						// bean 可能被多个地方使用，前面比较的时候，创建或者复制了被删除的变量。
						// 所以可能存在已经被删除var，这个时候忽略比较就行了。
						continue;
					}
					if (vOther.Deleted) {
						if (context.getConfig().getAllowSchemasReuseVariableIdWithSameType() && vThis.IsCompatible(vOther, context)) {
							// 反悔
							continue;
						}
						// 重用了已经被删除的var。此时vOther.Type也是null。
						logger.error("Not Compatible. bean={} variable={} Can Not Reuse Deleted Variable.Id", Name, vThis.Name);
						return false;
					}
					if (!vThis.IsCompatible(vOther, context)) {
						logger.error("Not Compatible. bean={} variable={}", Name, vOther.Name);
						return false;
					}
				} else {
					// 新删除或以前删除的都创建一个新的。
					Variable tempVar2 = new Variable();
					tempVar2.Id = vOther.Id;
					tempVar2.Name = vOther.Name;
					tempVar2.TypeName = vOther.TypeName;
					tempVar2.KeyName = vOther.KeyName;
					tempVar2.ValueName = vOther.ValueName;
					tempVar2.Type = vOther.Type;
					tempVar2.Deleted = true;
					Deleteds.add(tempVar2);
				}
			}
			// 限制beankey的var只能增加，不能减少。
			// 如果发生了Bean和BeanKey改变，忽略这个检查。
			// 如果没有被真正当作Key，忽略这个检查。
			if (IsBeanKey && getKeyRefCount() > 0 && beanOther.IsBeanKey && beanOther.getKeyRefCount() > 0) {
				if (getVariables().size() < beanOther.getVariables().size()) {
					logger.error("Not Compatible. beankey={} Variables.Count < DB.Variables.Count,Must Be Reduced", Name);
					return false;
				}
				for (var it = beanOther.getVariables().iterator(); it.moveToNext(); ) {
					var vOther = it.value();
					if (vOther.Deleted) {
						// 当作Key前允许删除变量，所以可能存在已经被删除的变量。
						continue;
					}
					if (!getVariables().containsKey(vOther.Id)) {
						// 被当作Key以后就不能再删除变量了。
						logger.error("Not Compatible. beankey={} variable={} Not Exist", Name, vOther.Name);
						return false;
					}
				}
			}

			if (!Deleteds.isEmpty()) {
				Bean newBean = ShadowCopy(context);
				context.getCurrent().AddBean(newBean);
				result.setBean(newBean);
				result.AddUpdate(Update, UpdateVariable);
				for (var vDelete : Deleteds) {
					vDelete.TryCopyBeanIfRemoved(context);
					newBean.getVariables().put(vDelete.Id, vDelete);
				}
			}
			return true;
		}

		@Override
		public void TryCopyBeanIfRemoved(Context context,
										 Action1<Bean> Update,
										 Action1<Bean> UpdateVariable) {
			CheckResult result = context.GetCopyBeanIfRemovedResult(this);
			if (null != result) {
				result.AddUpdate(Update, UpdateVariable);
				return;
			}
			result = new CheckResult();
			result.setBean(this);
			context.AddCopyBeanIfRemovedResult(this, result);

			if (Name.startsWith("_")) {
				// bean 是内部创建的，可能是原来删除的，也可能是合并改名引起的。
				if (context.getCurrent().Beans.containsKey(getRealName())) {
					return;
				}

				var newBean = ShadowCopy(context);
				newBean.setRealName(getRealName()); // 原来是新建的Bean，要使用这个。
				context.getCurrent().AddBean(newBean);
				result.setBean(newBean);
				result.AddUpdate(Update, UpdateVariable);
				return;
			}

			// 通过查找当前Schemas来发现RefZero。
			if (context.getCurrent().Beans.containsKey(Name)) {
				return;
			}

			var newBean2 = ShadowCopy(context);
			newBean2.Deleted = true;
			context.getCurrent().AddBean(newBean2);
			result.setBean(newBean2);
			result.AddUpdate(Update, UpdateVariable);

			getVariables().foreachValue(v -> v.TryCopyBeanIfRemoved(context));
		}

		private Bean ShadowCopy(Context context) {
			var newBean = new Bean();
			newBean.Name = context.GenerateUniqueName();
			newBean.IsBeanKey = this.IsBeanKey;
			newBean.KeyRefCount = this.getKeyRefCount();
			newBean.RealName = this.Name;
			newBean.Deleted = this.Deleted;
			getVariables().foreachValue(v -> newBean.getVariables().put(v.Id, v));
			return newBean;
		}

		@Override
		public void decode(ByteBuffer bb) {
			Name = bb.ReadString();
			IsBeanKey = bb.ReadBool();
			Deleted = bb.ReadBool();
			RealName = bb.ReadString();
			for (int count = bb.ReadInt(); count > 0; --count) {
				var v = new Variable();
				v.decode(bb);
				getVariables().put(v.Id, v);
			}
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteString(Name);
			bb.WriteBool(IsBeanKey);
			bb.WriteBool(Deleted);
			bb.WriteString(RealName);
			bb.WriteInt(getVariables().size());
			getVariables().foreachValue(v -> v.encode(bb));
		}

		@Override
		public void Compile(Schemas s) {
			getVariables().foreachValue(v -> v.Compile(s));
		}

		public final void AddVariable(Variable var) {
			getVariables().put(var.Id, var);
		}
	}

	public static class Table implements Serializable {
		public String Name; // FullName, sample: demo_Module1_Table1
		public String KeyName;
		public String ValueName;
		public Type KeyType;
		public Type ValueType;

		public Table() {

		}

		public Table(String n, String k, String v) {
			Name = n;
			KeyName = k;
			ValueName = v;
		}

		@Override
		public void decode(ByteBuffer bb) {
			Name = bb.ReadString();
			KeyName = bb.ReadString();
			ValueName = bb.ReadString();
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteString(Name);
			bb.WriteString(KeyName);
			bb.WriteString(ValueName);
		}

		public boolean IsCompatible(Table other, Context context) {
			return Name.equals(other.Name)
					&& KeyType.IsCompatible(other.KeyType, context,
					(bean) ->
					{
						KeyName = bean.Name;
						KeyType = bean;
					},
					null)
					&& ValueType.IsCompatible(other.ValueType, context,
					(bean) ->
					{
						ValueName = bean.Name;
						ValueType = bean;
					},
					null);
		}

		public void Compile(Schemas s) {
			KeyType = s.Compile(KeyName, "", "");
			if (KeyType instanceof Bean) {
				((Bean)KeyType).KeyRefCount++;
			}
			ValueType = s.Compile(ValueName, "", "");
		}
	}

	public final HashMap<String, Table> Tables = new HashMap<>();
	public final HashMap<String, Bean> Beans = new HashMap<>();

	// private final static Logger logger = LogManager.getLogger(Table.class);

	public void CheckCompatible(Schemas other, Zeze.Application app) throws Throwable {
		if (null == other)
			return;

		var context = new Context();
		{
			context.setCurrent(this);
			context.setPrevious(other);
			context.setConfig(app.getConfig());
		}

		for (var table : Tables.values()) {
			var zTable = app.GetTableSlow(table.Name);
			if (zTable == null || zTable.isNew() || app.getConfig().autoResetTable())
				continue;
			var otherTable = other.Tables.get(table.Name);
			if (null != otherTable) {
				if (!table.IsCompatible(otherTable, context))
					throw new IllegalStateException("Not Compatible Table=" + table.Name);
			}
		}
		context.Update();
	}

	@Override
	public void decode(ByteBuffer bb) {
		for (int count = bb.ReadInt(); count > 0; --count) {
			var table = new Table();
			table.decode(bb);
			if (null != Tables.put(table.Name, table))
				throw new IllegalStateException("duplicate table=" + table.Name);
		}
		for (int count = bb.ReadInt(); count > 0; --count) {
			var bean = new Bean();
			bean.decode(bb);
			if (null != Beans.put(bean.Name, bean))
				throw new IllegalStateException("duplicate bean=" + bean.Name);
		}
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(Tables.size());
		for (var table : Tables.values()) {
			table.encode(bb);
		}
		bb.WriteInt(Beans.size());
		for (var bean : Beans.values()) {
			bean.encode(bb);
		}
	}

	public void Compile() {
		for (var table : Tables.values()) {
			table.Compile(this);
		}
		for (var bean : Beans.values()) {
			bean.Compile(this);
		}
	}

	private final HashMap<String, Type> BasicTypes = new HashMap<>();

	public Type Compile(String type, String key, String value) {
		if (null == type || type.isEmpty())
			return null;

		var beanExist = Beans.get(type);
		if (null != beanExist)
			return beanExist;

		var fullTypeName = type + ":" + key + ":" + value;

		// 除了Bean，其他基本类型和容器类型都动态创建。
		var typeExist = BasicTypes.get(fullTypeName);
		if (null != typeExist)
			return typeExist;

		var n = new Type();
		{
			n.Name = type;
			n.KeyName = key;
			n.ValueName = value;
		}
		BasicTypes.put(fullTypeName, n);
		n.Compile(this); // 容器需要编译。这里的时机不是太好。
		return n;
	}

	public void AddBean(Bean bean) {
		if (null != Beans.put(bean.Name, bean))
			throw new IllegalStateException("AddBean duplicate=" + bean.Name);
	}

	public void AddTable(Table table) {
		if (null != Tables.put(table.Name, table))
			throw new IllegalStateException("AddTable duplicate=" + table.Name);
	}
}
