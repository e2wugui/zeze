package Zeze;

import Zeze.Serialize.*;
import java.util.*;

		public final void Update() {
			setKeyName(this.getType().getKeyName());
			setValueName(this.getType().getValueName());
		}

		public final void TryCopyBeanIfRemoved(Context context) {
			this.getType().TryCopyBeanIfRemoved(context, (bean) -> {
						setTypeName(bean.Name);
						setType(bean);
			}, (bean)-> {
						setKeyName(getType().getKeyName());
						setValueName(getType().getValueName());
					});
		}
	}

	public static class Bean extends Type {
		private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

		private HashMap<Integer, Variable> Variables = new HashMap<Integer, Variable> ();
		public final HashMap<Integer, Variable> getVariables() {
			return Variables;
		}
		private boolean IsBeanKey = false;
		public final boolean isBeanKey() {
			return IsBeanKey;
		}
		public final void setBeanKey(boolean value) {
			IsBeanKey = value;
		}
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
		private void setDeleted(boolean value) {
			Deleted = value;
		}
		// 这里记录在当前版本Schemas中Bean的实际名字，只有生成的bean包含这个。
		private String RealName = "";
		public final String getRealName() {
			return RealName;
		}
		private void setRealName(String value) {
			RealName = value;
		}

		/** 
		 var可能增加，也可能删除，所以兼容仅判断var.id相同的。
		 并且和谁比较谁没有关系。
		 
		 @param other
		 @return 
		*/
		@Override
		public boolean IsCompatible(Type other, Context context, tangible.Action1Param<Bean> Update, tangible.Action1Param<Bean> UpdateVariable) {
			if (other == null) {
				return false;
			}

			boolean tempVar = other instanceof Bean;
			Bean beanOther = tempVar ? (Bean)other : null;
			if (tempVar) {
				CheckResult result = context.GetCheckResult(beanOther, this);
				if (null != result) {
					result.AddUpdate(Update, UpdateVariable);
					return true;
				}
				result = new CheckResult(); // result在后面可能被更新。
				result.setBean(this);
				context.AddCheckResult(beanOther, this, result);

				ArrayList<Variable> Deleteds = new ArrayList<Variable>();
				for (var vOther : beanOther.getVariables().values()) {
					if (getVariables().containsKey(vOther.Id) && (var vThis = getVariables().get(vOther.Id)) == var vThis) {
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
							logger.Error("Not Compatible. bean={0} variable={1} Can Not Reuse Deleted Variable.Id", getName(), vThis.Name);
							return false;
						}
						if (false == vThis.IsCompatible(vOther, context)) {
							logger.Error("Not Compatible. bean={0} variable={1}", getName(), vOther.Name);
							return false;
						}
					}
					else {
						// 新删除或以前删除的都创建一个新的。
						Variable tempVar2 = new Variable();
						tempVar2.setId(vOther.Id);
						tempVar2.setName(vOther.Name);
						tempVar2.setTypeName(vOther.TypeName);
						tempVar2.setKeyName(vOther.KeyName);
						tempVar2.setValueName(vOther.ValueName);
						tempVar2.setType(vOther.Type);
						tempVar2.setDeleted(true);
						Deleteds.add(tempVar2);
					}
				}
				// 限制beankey的var只能增加，不能减少。
				// 如果发生了Bean和BeanKey改变，忽略这个检查。
				// 如果没有被真正当作Key，忽略这个检查。
				if (isBeanKey() && getKeyRefCount() > 0 && beanOther.isBeanKey() && beanOther.getKeyRefCount() > 0) {
					if (getVariables().size() < beanOther.getVariables().size()) {
						logger.Error("Not Compatible. beankey={0} Variables.Count < DB.Variables.Count,Must Be Reduced", getName());
						return false;
					}
					for (var vOther : beanOther.getVariables().values()) {
						if (vOther.Deleted) {
							// 当作Key前允许删除变量，所以可能存在已经被删除的变量。
							continue;
						}
						if (false == (getVariables().containsKey(vOther.Id) && (var _ = getVariables().get(vOther.Id)) == var _)) {
							// 被当作Key以后就不能再删除变量了。
							logger.Error("Not Compatible. beankey={0} variable={1} Not Exist", getName(), vOther.Name);
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
						newBean.getVariables().put(vDelete.getId(), vDelete);
					}
				}
				return true;
			}
			return false;
		}

		@Override
		public void TryCopyBeanIfRemoved(Context context, tangible.Action1Param<Bean> Update, tangible.Action1Param<Bean> UpdateVariable) {
			CheckResult result = context.GetCopyBeanIfRemovedResult(this);
			if (null != result) {
				result.AddUpdate(Update, UpdateVariable);
				return;
			}
			result = new CheckResult();
			result.setBean(this);
			context.AddCopyBeanIfRemovedResult(this, result);

			if (getName().startsWith("_")) {
				// bean 是内部创建的，可能是原来删除的，也可能是合并改名引起的。
				if (context.getCurrent().getBeans().containsKey(getRealName()) && (var _ = context.getCurrent().getBeans().get(getRealName())) == var _) {
					return;
				}

				var newb = ShadowCopy(context);
				newb.setRealName(getRealName()); // 原来是新建的Bean，要使用这个。
				context.getCurrent().AddBean(newb);
				result.setBean(newb);
				result.AddUpdate(Update, UpdateVariable);
				return;
			}

			// 通过查找当前Schemas来发现RefZero。
			if (context.getCurrent().getBeans().containsKey(getName()) && (var _ = context.getCurrent().getBeans().get(getName())) == var _) {
				return;
			}

			var newb2 = ShadowCopy(context);
			newb2.setDeleted(true);
			context.getCurrent().AddBean(newb2);
			result.setBean(newb2);
			result.AddUpdate(Update, UpdateVariable);

			for (var v : getVariables().values()) {
				v.TryCopyBeanIfRemoved(context);
			}
		}

		private Bean ShadowCopy(Context context) {
			var newBean = new Bean();
			newBean.setName(context.GenerateUniqueName());
			newBean.setBeanKey(this.isBeanKey());
			newBean.setKeyRefCount(this.getKeyRefCount());
			newBean.setRealName(this.getName());
			newBean.setDeleted(this.getDeleted());
			for (var v : getVariables().values()) {
				newBean.getVariables().put(v.Id, v);
			}
			return newBean;
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setName(bb.ReadString());
			setBeanKey(bb.ReadBool());
			setDeleted(bb.ReadBool());
			setRealName(bb.ReadString());
			for (int count = bb.ReadInt(); count > 0; --count) {
				var v = new Variable();
				v.Decode(bb);
				getVariables().put(v.getId(), v);
			}
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteString(getName());
			bb.WriteBool(isBeanKey());
			bb.WriteBool(getDeleted());
			bb.WriteString(getRealName());
			bb.WriteInt(getVariables().size());
			for (var v : getVariables().values()) {
				v.Encode(bb);
			}
		}

		@Override
		public void Compile(Schemas s) {
			for (var v : getVariables().values()) {
				v.Compile(s);
			}
		}

		public final void AddVariable(Variable var) {
			getVariables().put(var.getId(), var);
		}
	}