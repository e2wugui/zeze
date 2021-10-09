package Zeze;

import Zeze.Serialize.*;
import java.util.*;

		public void Decode(ByteBuffer bb) {
			setName(bb.ReadString());
			setKeyName(bb.ReadString());
			setValueName(bb.ReadString());
		}

		public void Encode(ByteBuffer bb) {
			bb.WriteString(getName());
			bb.WriteString(getKeyName());
			bb.WriteString(getValueName());
		}

		public void Compile(Schemas s) {
			setKey(s.Compile(getKeyName(), "", ""));
			boolean tempVar = Key instanceof Bean;
			Bean key = tempVar ? (Bean)Key : null;
			if (null != getKey() && tempVar) {
				key.setKeyRefCount(key.getKeyRefCount() + 1);
			}

			setValue(s.Compile(getValueName(), "", ""));
			if (null != getValue()) {
				boolean tempVar2 = Value instanceof Bean;
				Bean value = tempVar2 ? (Bean)Value : null;
				if (getName().equals("set") && tempVar2) {
					value.setKeyRefCount(value.getKeyRefCount() + 1);
				}
			}
		}

		public void TryCopyBeanIfRemoved(Context context, tangible.Action1Param<Bean> Update, tangible.Action1Param<Bean> UpdateVariable) {
			if (getKey() != null) {
				getKey().TryCopyBeanIfRemoved;
			} {
					setKeyName(bean.Name);
					setKey(bean);
				}
			   , UpdateVariable);

			if (getValue() != null) {
				getValue().TryCopyBeanIfRemoved;
			} {
					setValueName(bean.Name);
					setValue(bean);
				}
			   , UpdateVariable);
		}
	}

	public static class Variable implements Serializable {
		private int Id;
		public final int getId() {
			return Id;
		}
		public final void setId(int value) {
			Id = value;
		}
		private String Name;
		public final String getName() {
			return Name;
		}
		public final void setName(String value) {
			Name = value;
		}
		private String TypeName;
		public final String getTypeName() {
			return TypeName;
		}
		public final void setTypeName(String value) {
			TypeName = value;
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
		private Type Type;
		public final Type getType() {
			return Type;
		}
		public final void setType(Type value) {
			Type = value;
		}
		private boolean Deleted = false;
		public final boolean getDeleted() {
			return Deleted;
		}
		public final void setDeleted(boolean value) {
			Deleted = value;
		}

		public final void Decode(ByteBuffer bb) {
			setId(bb.ReadInt());
			setName(bb.ReadString());
			setTypeName(bb.ReadString());
			setKeyName(bb.ReadString());
			setValueName(bb.ReadString());
			setDeleted(bb.ReadBool());
		}

		public final void Encode(ByteBuffer bb) {
			bb.WriteInt(getId());
			bb.WriteString(getName());
			bb.WriteString(getTypeName());
			bb.WriteString(getKeyName());
			bb.WriteString(getValueName());
			bb.WriteBool(getDeleted());
		}

		public final void Compile(Schemas s) {
			setType(s.Compile(getTypeName(), getKeyName(), getValueName()));
		}

		public final boolean IsCompatible(Variable other, Context context) {
			return this.getType().IsCompatible(other.getType(), context, (bean) -> {
						setTypeName(bean.Name);
						setType(bean);
			}, (bean) -> {
						setKeyName(getType().getKeyName());
						setValueName(getType().getValueName());
					});
		}