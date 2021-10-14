package Game.Fight;

import Zeze.Serialize.*;
import Game.*;

public final class BFighter extends Zeze.Transaction.Bean implements BFighterReadOnly {
	private float _Attack;
	private float _Defence;

	public float getAttack() {
		if (false == this.isManaged()) {
			return _Attack;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Attack;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Attack)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _Attack;
	}
	public void setAttack(float value) {
		if (false == this.isManaged()) {
			_Attack = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Attack(this, value));
	}

	public float getDefence() {
		if (false == this.isManaged()) {
			return _Defence;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Defence;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Defence)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _Defence;
	}
	public void setDefence(float value) {
		if (false == this.isManaged()) {
			_Defence = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Defence(this, value));
	}


	public BFighter() {
		this(0);
	}

	public BFighter(int _varId_) {
		super(_varId_);
	}

	public void Assign(BFighter other) {
		setAttack(other.getAttack());
		setDefence(other.getDefence());
	}

	public BFighter CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BFighter Copy() {
		var copy = new BFighter();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BFighter a, BFighter b) {
		BFighter save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -527486826100084428;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Attack extends Zeze.Transaction.Log<BFighter, Float> {
		public Log__Attack(BFighter self, float value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Attack = this.getValue();
		}
	}

	private final static class Log__Defence extends Zeze.Transaction.Log<BFighter, Float> {
		public Log__Defence(BFighter self, float value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Defence = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Fight.BFighter: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Attack").Append("=").Append(getAttack()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Defence").Append("=").Append(getDefence()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(2); // Variables.Count
		_os_.WriteInt(ByteBuffer.FLOAT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteFloat(getAttack());
		_os_.WriteInt(ByteBuffer.FLOAT | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteFloat(getDefence());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.FLOAT | 1 << ByteBuffer.TAG_SHIFT:
					setAttack(_os_.ReadFloat());
					break;
				case ByteBuffer.FLOAT | 2 << ByteBuffer.TAG_SHIFT:
					setDefence(_os_.ReadFloat());
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