package Game.Map;

import Zeze.Serialize.*;
import Game.*;

public final class BEnterWorldNow extends Zeze.Transaction.Bean implements BEnterWorldNowReadOnly {
	private int _MapInstanceId;
	private float _x;
	private float _y;
	private float _z;
	private int _ResouceId;

	public int getMapInstanceId() {
		if (false == this.isManaged()) {
			return _MapInstanceId;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _MapInstanceId;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__MapInstanceId)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _MapInstanceId;
	}
	public void setMapInstanceId(int value) {
		if (false == this.isManaged()) {
			_MapInstanceId = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__MapInstanceId(this, value));
	}

	public float getX() {
		if (false == this.isManaged()) {
			return _x;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _x;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__x)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _x;
	}
	public void setX(float value) {
		if (false == this.isManaged()) {
			_x = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__x(this, value));
	}

	public float getY() {
		if (false == this.isManaged()) {
			return _y;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _y;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__y)txn.GetLog(this.getObjectId() + 3);
		return log != null ? log.getValue() : _y;
	}
	public void setY(float value) {
		if (false == this.isManaged()) {
			_y = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__y(this, value));
	}

	public float getZ() {
		if (false == this.isManaged()) {
			return _z;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _z;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__z)txn.GetLog(this.getObjectId() + 4);
		return log != null ? log.getValue() : _z;
	}
	public void setZ(float value) {
		if (false == this.isManaged()) {
			_z = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__z(this, value));
	}

	public int getResouceId() {
		if (false == this.isManaged()) {
			return _ResouceId;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ResouceId;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ResouceId)txn.GetLog(this.getObjectId() + 5);
		return log != null ? log.getValue() : _ResouceId;
	}
	public void setResouceId(int value) {
		if (false == this.isManaged()) {
			_ResouceId = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ResouceId(this, value));
	}


	public BEnterWorldNow() {
		this(0);
	}

	public BEnterWorldNow(int _varId_) {
		super(_varId_);
	}

	public void Assign(BEnterWorldNow other) {
		setMapInstanceId(other.getMapInstanceId());
		setX(other.getX());
		setY(other.getY());
		setZ(other.getZ());
		setResouceId(other.getResouceId());
	}

	public BEnterWorldNow CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BEnterWorldNow Copy() {
		var copy = new BEnterWorldNow();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BEnterWorldNow a, BEnterWorldNow b) {
		BEnterWorldNow save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 2861757223843224449;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__MapInstanceId extends Zeze.Transaction.Log<BEnterWorldNow, Integer> {
		public Log__MapInstanceId(BEnterWorldNow self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._MapInstanceId = this.getValue();
		}
	}

	private final static class Log__x extends Zeze.Transaction.Log<BEnterWorldNow, Float> {
		public Log__x(BEnterWorldNow self, float value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._x = this.getValue();
		}
	}

	private final static class Log__y extends Zeze.Transaction.Log<BEnterWorldNow, Float> {
		public Log__y(BEnterWorldNow self, float value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 3;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._y = this.getValue();
		}
	}

	private final static class Log__z extends Zeze.Transaction.Log<BEnterWorldNow, Float> {
		public Log__z(BEnterWorldNow self, float value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 4;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._z = this.getValue();
		}
	}

	private final static class Log__ResouceId extends Zeze.Transaction.Log<BEnterWorldNow, Integer> {
		public Log__ResouceId(BEnterWorldNow self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 5;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ResouceId = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Map.BEnterWorldNow: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("MapInstanceId").Append("=").Append(getMapInstanceId()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("X").Append("=").Append(getX()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Y").Append("=").Append(getY()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Z").Append("=").Append(getZ()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ResouceId").Append("=").Append(getResouceId()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(5); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getMapInstanceId());
		_os_.WriteInt(ByteBuffer.FLOAT | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteFloat(getX());
		_os_.WriteInt(ByteBuffer.FLOAT | 3 << ByteBuffer.TAG_SHIFT);
		_os_.WriteFloat(getY());
		_os_.WriteInt(ByteBuffer.FLOAT | 4 << ByteBuffer.TAG_SHIFT);
		_os_.WriteFloat(getZ());
		_os_.WriteInt(ByteBuffer.INT | 5 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getResouceId());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setMapInstanceId(_os_.ReadInt());
					break;
				case ByteBuffer.FLOAT | 2 << ByteBuffer.TAG_SHIFT:
					setX(_os_.ReadFloat());
					break;
				case ByteBuffer.FLOAT | 3 << ByteBuffer.TAG_SHIFT:
					setY(_os_.ReadFloat());
					break;
				case ByteBuffer.FLOAT | 4 << ByteBuffer.TAG_SHIFT:
					setZ(_os_.ReadFloat());
					break;
				case ByteBuffer.INT | 5 << ByteBuffer.TAG_SHIFT:
					setResouceId(_os_.ReadInt());
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
		if (getMapInstanceId() < 0) {
			return true;
		}
		if (getResouceId() < 0) {
			return true;
		}
		return false;
	}

}