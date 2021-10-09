package Zeze;

import Zeze.Serialize.*;
import java.util.*;

/** 
 1 启动数据库时，用来判断当前代码的数据定义结构是否和当前数据库的定义结构兼容。
   当前包含以下兼容检测。
   a) 对于每个 Variable.Id，Type不能修改。
   b) 不能复用已经删除的 Variable.Id。
	  但是允许"反悔"，也就是说可以重新使用已经删除的Variable.Id时，只要Type和原来一样，就允许。
	  这是为了处理多人使用同一个数据库进行开发时的冲突（具体不解释了）。
   c) beankey 被应用于map.Key或set.Value或table.Key以后就不能再删除变量了。
	  当作key以后，如果删除变量，beankey.Encode() 就可能不再唯一。
	  
 2 通过查询类型信息，从数据转换到具体实例。合服可能需要。
   如果是通用合并的insert，应该在二进制接口上操作（目前还没有）。
   如果合并时需要处理冲突，此时应用是知道具体类型的。
   所以这个功能暂时先不提供了。
   
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
			_h_ = _h_ * _prime_ + getPrevious().getName().hashCode();
			_h_ = _h_ * _prime_ + getCurrent().getName().hashCode();
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
		private ArrayList<tangible.Action1Param<Bean>> Updates = new ArrayList<tangible.Action1Param<Bean>> ();
		private ArrayList<tangible.Action1Param<Bean>> getUpdates() {
			return Updates;
		}
		private ArrayList<tangible.Action1Param<Bean>> UpdateVariables = new ArrayList<tangible.Action1Param<Bean>> ();
		private ArrayList<tangible.Action1Param<Bean>> getUpdateVariables() {
			return UpdateVariables;
		}

		public final void AddUpdate(tangible.Action1Param<Bean> Update, tangible.Action1Param<Bean> UpdateVariable) {
			getUpdates().add(Update);
			if (null != UpdateVariable) {
				getUpdateVariables().add(UpdateVariable);
			}
		}

		public final void Update() {
			for (var update : getUpdates()) {
				update.invoke(getBean());
			}
			for (var update : getUpdateVariables()) {
				update.invoke(getBean());
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
		private HashMap<Checked, CheckResult> Checked = new HashMap<Checked, CheckResult> ();
		public final HashMap<Checked, CheckResult> getChecked() {
			return Checked;
		}
		private HashMap<Bean, CheckResult> CopyBeanIfRemoved = new HashMap<Bean, CheckResult> ();
		public final HashMap<Bean, CheckResult> getCopyBeanIfRemoved() {
			return CopyBeanIfRemoved;
		}
		private Config Config;
		public final Config getConfig() {
			return Config;
		}
		public final void setConfig(Config value) {
			Config = value;
		}

		public final CheckResult GetCheckResult(Bean previous, Bean current) {
			Schemas.Checked tempVar = new Schemas.Checked(), out var exist));
			tempVar.setPrevious(previous);
			tempVar.setCurrent(current);
			if (getChecked().containsKey(tempVar) && (var exist = getChecked().get(tempVar)) == var exist) {
				return exist;
			}
			return null;
		}

		public final void AddCheckResult(Bean previous, Bean current, CheckResult result) {
			Schemas.Checked tempVar = new Schemas.Checked(), result);
			tempVar.setPrevious(previous);
			tempVar.setCurrent(current);
			getChecked().put(tempVar, result);
		}

		public final CheckResult GetCopyBeanIfRemovedResult(Bean bean) {
			if (getCopyBeanIfRemoved().containsKey(bean) && (var exist = getCopyBeanIfRemoved().get(bean)) == var exist) {
				return exist;
			}
			return null;
		}

		public final void AddCopyBeanIfRemovedResult(Bean bean, CheckResult result) {
			getCopyBeanIfRemoved().put(bean, result);
		}

		public final void Update() {
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
		private String Name;
		public final String getName() {
			return Name;
		}
		public final void setName(String value) {
			Name = value;
		}
		private String KeyName = "";
		public final String getKeyName() {
			return KeyName;
		}
		public final void setKeyName(String value) {
			KeyName = value;
		}
		private String ValueName = "";
		public final String getValueName() {
			return ValueName;
		}
		public final void setValueName(String value) {
			ValueName = value;
		}
		private Type Key;
		public final Type getKey() {
			return Key;
		}
		private void setKey(Type value) {
			Key = value;
		}
		private Type Value;
		public final Type getValue() {
			return Value;
		}
		private void setValue(Type value) {
			Value = value;
		}

		public boolean IsCompatible(Type other, Context context, tangible.Action1Param<Bean> Update, tangible.Action1Param<Bean> UpdateVariable) {
			if (other == this) {
				return true;
			}

			if (other == null) {
				return false;
			}

			if (false == getName().equals(other.getName())) {
				return false;
			}

			// Name 相同的情况下，下面的 Key Value 仅在 Collection 时有值。
			// 当 this.Key == null && other.Key != null 在 Name 相同的情况下是不可能发生的。
			if (null != getKey()) {
				if (false == getKey().IsCompatible(other.getKey(), context, (bean) -> {
					setKeyName(bean.Name);
					setKey(bean);
				}, UpdateVariable)) {
					return false;
				}
			}
			else if (other.getKey() != null) {
				throw new RuntimeException("(this.Key == null && other.Key != null) Imposible!");
			}

			if (null != getValue()) {
				if (false == getValue().IsCompatible(other.getValue(), context, (bean) -> {
					setValueName(bean.Name);
					setValue(bean);
				}, UpdateVariable)) {
					return false;
				}
			}
			else if (other.getValue() != null) {
				throw new RuntimeException("(this.Value == null && other.Value != null) Imposible!");
			}

			return true;
		}