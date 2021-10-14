package Zezex.Linkd;

import Zeze.Serialize.*;
import Zezex.*;

public final class BAuth extends Zeze.Transaction.Bean implements BAuthReadOnly {
	private String _Account;
	private String _Token; // security. maybe password

	public String getAccount() {
		if (false == this.isManaged()) {
			return _Account;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Account;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Account)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _Account;
	}
	public void setAccount(String value) {
		if (value.equals(null)) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_Account = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Account(this, value));
	}

	public String getToken() {
		if (false == this.isManaged()) {
			return _Token;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Token;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Token)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _Token;
	}
	public void setToken(String value) {
		if (value.equals(null)) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_Token = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Token(this, value));
	}


	public BAuth() {
		this(0);
	}

	public BAuth(int _varId_) {
		super(_varId_);
		_Account = "";
		_Token = "";
	}

	public void Assign(BAuth other) {
		setAccount(other.getAccount());
		setToken(other.getToken());
	}

	public BAuth CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BAuth Copy() {
		var copy = new BAuth();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BAuth a, BAuth b) {
		BAuth save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -4648907590533302561;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Account extends Zeze.Transaction.Log<BAuth, String> {
		public Log__Account(BAuth self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Account = this.getValue();
		}
	}

	private final static class Log__Token extends Zeze.Transaction.Log<BAuth, String> {
		public Log__Token(BAuth self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Token = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Linkd.BAuth: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Account").Append("=").Append(getAccount()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Token").Append("=").Append(getToken()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(2); // Variables.Count
		_os_.WriteInt(ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getAccount());
		_os_.WriteInt(ByteBuffer.STRING | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getToken());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT:
					setAccount(_os_.ReadString());
					break;
				case ByteBuffer.STRING | 2 << ByteBuffer.TAG_SHIFT:
					setToken(_os_.ReadString());
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