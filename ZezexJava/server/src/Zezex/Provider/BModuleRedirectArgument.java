package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BModuleRedirectArgument extends Zeze.Transaction.Bean implements BModuleRedirectArgumentReadOnly {
	private int _ModuleId;
	private int _HashCode; // server 计算。see BBind.ChoiceType。
	private String _MethodFullName; // format="ModuleFullName:MethodName"
	private Zeze.Net.Binary _Params;
	private String _ServiceNamePrefix;

	public int getModuleId() {
		if (false == this.isManaged()) {
			return _ModuleId;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ModuleId;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ModuleId)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _ModuleId;
	}
	public void setModuleId(int value) {
		if (false == this.isManaged()) {
			_ModuleId = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ModuleId(this, value));
	}

	public int getHashCode() {
		if (false == this.isManaged()) {
			return _HashCode;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _HashCode;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__HashCode)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _HashCode;
	}
	public void setHashCode(int value) {
		if (false == this.isManaged()) {
			_HashCode = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__HashCode(this, value));
	}

	public String getMethodFullName() {
		if (false == this.isManaged()) {
			return _MethodFullName;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _MethodFullName;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__MethodFullName)txn.GetLog(this.getObjectId() + 4);
		return log != null ? log.getValue() : _MethodFullName;
	}
	public void setMethodFullName(String value) {
		if (value.equals(null)) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_MethodFullName = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__MethodFullName(this, value));
	}

	public Zeze.Net.Binary getParams() {
		if (false == this.isManaged()) {
			return _Params;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Params;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Params)txn.GetLog(this.getObjectId() + 5);
		return log != null ? log.getValue() : _Params;
	}
	public void setParams(Zeze.Net.Binary value) {
		if (null == value) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_Params = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Params(this, value));
	}

	public String getServiceNamePrefix() {
		if (false == this.isManaged()) {
			return _ServiceNamePrefix;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ServiceNamePrefix;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ServiceNamePrefix)txn.GetLog(this.getObjectId() + 6);
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


	public BModuleRedirectArgument() {
		this(0);
	}

	public BModuleRedirectArgument(int _varId_) {
		super(_varId_);
		_MethodFullName = "";
		_Params = Zeze.Net.Binary.Empty;
		_ServiceNamePrefix = "";
	}

	public void Assign(BModuleRedirectArgument other) {
		setModuleId(other.getModuleId());
		setHashCode(other.getHashCode());
		setMethodFullName(other.getMethodFullName());
		setParams(other.getParams());
		setServiceNamePrefix(other.getServiceNamePrefix());
	}

	public BModuleRedirectArgument CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BModuleRedirectArgument Copy() {
		var copy = new BModuleRedirectArgument();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BModuleRedirectArgument a, BModuleRedirectArgument b) {
		BModuleRedirectArgument save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -2791236366487169007;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__ModuleId extends Zeze.Transaction.Log<BModuleRedirectArgument, Integer> {
		public Log__ModuleId(BModuleRedirectArgument self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ModuleId = this.getValue();
		}
	}

	private final static class Log__HashCode extends Zeze.Transaction.Log<BModuleRedirectArgument, Integer> {
		public Log__HashCode(BModuleRedirectArgument self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._HashCode = this.getValue();
		}
	}

	private final static class Log__MethodFullName extends Zeze.Transaction.Log<BModuleRedirectArgument, String> {
		public Log__MethodFullName(BModuleRedirectArgument self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 4;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._MethodFullName = this.getValue();
		}
	}

	private final static class Log__Params extends Zeze.Transaction.Log<BModuleRedirectArgument, Zeze.Net.Binary> {
		public Log__Params(BModuleRedirectArgument self, Zeze.Net.Binary value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 5;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Params = this.getValue();
		}
	}

	private final static class Log__ServiceNamePrefix extends Zeze.Transaction.Log<BModuleRedirectArgument, String> {
		public Log__ServiceNamePrefix(BModuleRedirectArgument self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 6;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ServiceNamePrefix = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BModuleRedirectArgument: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ModuleId").Append("=").Append(getModuleId()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("HashCode").Append("=").Append(getHashCode()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("MethodFullName").Append("=").Append(getMethodFullName()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Params").Append("=").Append(getParams()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ServiceNamePrefix").Append("=").Append(getServiceNamePrefix()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(5); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getModuleId());
		_os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getHashCode());
		_os_.WriteInt(ByteBuffer.STRING | 4 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getMethodFullName());
		_os_.WriteInt(ByteBuffer.BYTES | 5 << ByteBuffer.TAG_SHIFT);
		_os_.WriteBinary(getParams());
		_os_.WriteInt(ByteBuffer.STRING | 6 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getServiceNamePrefix());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setModuleId(_os_.ReadInt());
					break;
				case ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT:
					setHashCode(_os_.ReadInt());
					break;
				case ByteBuffer.STRING | 4 << ByteBuffer.TAG_SHIFT:
					setMethodFullName(_os_.ReadString());
					break;
				case ByteBuffer.BYTES | 5 << ByteBuffer.TAG_SHIFT:
					setParams(_os_.ReadBinary());
					break;
				case ByteBuffer.STRING | 6 << ByteBuffer.TAG_SHIFT:
					setServiceNamePrefix(_os_.ReadString());
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
		if (getModuleId() < 0) {
			return true;
		}
		if (getHashCode() < 0) {
			return true;
		}
		return false;
	}

}