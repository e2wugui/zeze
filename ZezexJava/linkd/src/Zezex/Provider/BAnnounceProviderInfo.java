package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BAnnounceProviderInfo extends Zeze.Transaction.Bean implements BAnnounceProviderInfoReadOnly {
	private String _ServiceNamePrefix;
	private String _ServiceIndentity;

	public String getServiceNamePrefix() {
		if (false == this.isManaged()) {
			return _ServiceNamePrefix;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ServiceNamePrefix;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ServiceNamePrefix)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _ServiceNamePrefix;
	}
	public void setServiceNamePrefix(String value) {
		if (value.equals(null)) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_ServiceNamePrefix = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ServiceNamePrefix(this, value));
	}

	public String getServiceIndentity() {
		if (false == this.isManaged()) {
			return _ServiceIndentity;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ServiceIndentity;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ServiceIndentity)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _ServiceIndentity;
	}
	public void setServiceIndentity(String value) {
		if (value.equals(null)) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_ServiceIndentity = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ServiceIndentity(this, value));
	}


	public BAnnounceProviderInfo() {
		this(0);
	}

	public BAnnounceProviderInfo(int _varId_) {
		super(_varId_);
		_ServiceNamePrefix = "";
		_ServiceIndentity = "";
	}

	public void Assign(BAnnounceProviderInfo other) {
		setServiceNamePrefix(other.getServiceNamePrefix());
		setServiceIndentity(other.getServiceIndentity());
	}

	public BAnnounceProviderInfo CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BAnnounceProviderInfo Copy() {
		var copy = new BAnnounceProviderInfo();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BAnnounceProviderInfo a, BAnnounceProviderInfo b) {
		BAnnounceProviderInfo save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -8838502681221980258;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__ServiceNamePrefix extends Zeze.Transaction.Log<BAnnounceProviderInfo, String> {
		public Log__ServiceNamePrefix(BAnnounceProviderInfo self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ServiceNamePrefix = this.getValue();
		}
	}

	private final static class Log__ServiceIndentity extends Zeze.Transaction.Log<BAnnounceProviderInfo, String> {
		public Log__ServiceIndentity(BAnnounceProviderInfo self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ServiceIndentity = this.getValue();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		BuildString(sb, 0);
		sb.append(System.lineSeparator());
		return sb.toString();
	}

	@Override
	public void BuildString(StringBuilder sb, int level) {
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BAnnounceProviderInfo: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ServiceNamePrefix").Append("=").Append(getServiceNamePrefix()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ServiceIndentity").Append("=").Append(getServiceIndentity()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(2); // Variables.Count
		_os_.WriteInt(ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getServiceNamePrefix());
		_os_.WriteInt(ByteBuffer.STRING | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getServiceIndentity());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT:
					setServiceNamePrefix(_os_.ReadString());
					break;
				case ByteBuffer.STRING | 2 << ByteBuffer.TAG_SHIFT:
					setServiceIndentity(_os_.ReadString());
					break;
				default:
					ByteBuffer.SkipUnknownField(_tagid_, _os_);
					break;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
	}

	@Override
	public boolean NegativeCheck() {
		return false;
	}

}