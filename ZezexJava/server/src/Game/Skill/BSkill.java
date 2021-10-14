package Game.Skill;

import Zeze.Serialize.*;
import Game.*;

public final class BSkill extends Zeze.Transaction.Bean implements BSkillReadOnly {
	private int _Id;
	private Zeze.Transaction.DynamicBean _Extra;

	public int getId() {
		if (false == this.isManaged()) {
			return _Id;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Id;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Id)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _Id;
	}
	public void setId(int value) {
		if (false == this.isManaged()) {
			_Id = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Id(this, value));
	}

	public Zeze.Transaction.DynamicBean getExtra() {
		return _Extra;
	}
	private Zeze.Transaction.DynamicBeanReadOnly Game.Skill.BSkillReadOnly.Extra -> getExtra();

	public Game.Skill.BSkillAttackExtra getExtraGameSkillBSkillAttackExtra() {
		return (Game.Skill.BSkillAttackExtra)getExtra().Bean;
	}
	public void setExtraGameSkillBSkillAttackExtra(Game.Skill.BSkillAttackExtra value) {
		getExtra().Bean = value;
	}

	private Game.Skill.BSkillAttackExtraReadOnly Game.Skill.BSkillReadOnly.Extra_Game_Skill_BSkillAttackExtra -> getExtraGameSkillBSkillAttackExtra();


	public BSkill() {
		this(0);
	}

	public BSkill(int _varId_) {
		super(_varId_);
		_Extra = new Zeze.Transaction.DynamicBean(2, GetSpecialTypeIdFromBean_Extra, CreateBeanFromSpecialTypeId_Extra);
	}

	public void Assign(BSkill other) {
		setId(other.getId());
		getExtra().Assign(other.getExtra());
	}

	public BSkill CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BSkill Copy() {
		var copy = new BSkill();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BSkill a, BSkill b) {
		BSkill save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -567310546869368175;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Id extends Zeze.Transaction.Log<BSkill, Integer> {
		public Log__Id(BSkill self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Id = this.getValue();
		}
	}

	public static long GetSpecialTypeIdFromBean_Extra(Zeze.Transaction.Bean bean) {
		switch (bean.TypeId) {
			case Zeze.Transaction.EmptyBean.TYPEID:
				return Zeze.Transaction.EmptyBean.TYPEID;
			case 5301582953788202231:
				return 5301582953788202231; // Game.Skill.BSkillAttackExtra
		}
		throw new RuntimeException("Unknown Bean! dynamic@Game.Skill.BSkill:Extra");
	}

	public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_Extra(long typeId) {
		switch (typeId) {
			case 5301582953788202231:
				return new Game.Skill.BSkillAttackExtra();
		}
		return null;
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Skill.BSkill: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Id").Append("=").Append(getId()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Extra").Append("=").Append(System.lineSeparator());
		getExtra().Bean.BuildString(sb, level + 1);
		sb.append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(2); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getId());
		_os_.WriteInt(ByteBuffer.DYNAMIC | 2 << ByteBuffer.TAG_SHIFT);
		getExtra().Encode(_os_);
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setId(_os_.ReadInt());
					break;
				case ByteBuffer.DYNAMIC | 2 << ByteBuffer.TAG_SHIFT:
					getExtra().Decode(_os_);
					break;
				default:
					ByteBuffer.SkipUnknownField(_tagid_, _os_);
					break;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		_Extra.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		if (getId() < 0) {
			return true;
		}
		return false;
	}

}