package Game.Skill;

import Zeze.Serialize.*;
import Game.*;

public final class BSkills extends Zeze.Transaction.Bean implements BSkillsReadOnly {
	private Zeze.Transaction.Collections.PMap2<Integer, Game.Skill.BSkill> _Skills;
	private Zeze.Transaction.Collections.PMapReadOnly<Integer,Game.Skill.BSkillReadOnly,Game.Skill.BSkill> _SkillsReadOnly;

	public Zeze.Transaction.Collections.PMap2<Integer, Game.Skill.BSkill> getSkills() {
		return _Skills;
	}
	private System.Collections.Generic.IReadOnlyDictionary<Integer,Game.Skill.BSkillReadOnly> Game.Skill.BSkillsReadOnly.Skills -> _SkillsReadOnly;


	public BSkills() {
		this(0);
	}

	public BSkills(int _varId_) {
		super(_varId_);
		_Skills = new Zeze.Transaction.Collections.PMap2<Integer, Game.Skill.BSkill>(getObjectId() + 1, _v -> new Log__Skills(this, _v));
		_SkillsReadOnly = new Zeze.Transaction.Collections.PMapReadOnly<Integer,Game.Skill.BSkillReadOnly,Game.Skill.BSkill>(_Skills);
	}

	public void Assign(BSkills other) {
		getSkills().Clear();
		for (var e : other.getSkills()) {
			getSkills().Add(e.Key, e.Value.Copy());
		}
	}

	public BSkills CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BSkills Copy() {
		var copy = new BSkills();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BSkills a, BSkills b) {
		BSkills save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -6850420950998469956;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Skills extends Zeze.Transaction.Collections.PMap2<Integer, Game.Skill.BSkill>.LogV {
		public Log__Skills(BSkills host, System.Collections.Immutable.ImmutableDictionary<Integer, Game.Skill.BSkill> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 1;
		}
		public BSkills getBeanTyped() {
			return (BSkills)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._Skills);
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Skill.BSkills: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Skills").Append("=[").Append(System.lineSeparator());
		level++;
		for (var _kv_ : getSkills()) {
			sb.append("(").Append(System.lineSeparator());
			var Key = _kv_.Key;
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Key").Append("=").Append(Key).Append(",").Append(System.lineSeparator());
			var Value = _kv_.Value;
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Value").Append("=").Append(System.lineSeparator());
			Value.BuildString(sb, level + 1);
			sb.append(",").Append(System.lineSeparator());
			sb.append(")").Append(System.lineSeparator());
		}
		level--;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("]").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(1); // Variables.Count
		_os_.WriteInt(ByteBuffer.MAP | 1 << ByteBuffer.TAG_SHIFT); {
			int _state_;
			tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
			_os_.BeginWriteSegment(tempOut__state_);
		_state_ = tempOut__state_.outArgValue;
			_os_.WriteInt(ByteBuffer.INT);
			_os_.WriteInt(ByteBuffer.BEAN);
			_os_.WriteInt(getSkills().Count);
			for (var _e_ : getSkills()) {
				_os_.WriteInt(_e_.Key);
				_e_.Value.Encode(_os_);
			}
			_os_.EndWriteSegment(_state_);
		}
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.MAP | 1 << ByteBuffer.TAG_SHIFT: {
						int _state_;
						tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
						_os_.BeginReadSegment(tempOut__state_);
					_state_ = tempOut__state_.outArgValue;
						_os_.ReadInt(); // skip key typetag
						_os_.ReadInt(); // skip value typetag
						getSkills().Clear();
						for (int size = _os_.ReadInt(); size > 0; --size) {
							int _k_;
							_k_ = _os_.ReadInt();
							Game.Skill.BSkill _v_ = new Game.Skill.BSkill();
							_v_.Decode(_os_);
							getSkills().Add(_k_, _v_);
						}
						_os_.EndReadSegment(_state_);
				}
					break;
				default:
					ByteBuffer.SkipUnknownField(_tagid_, _os_);
					break;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		_Skills.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		for (var _v_ : getSkills().Values) {
			if (_v_.NegativeCheck()) {
				return true;
			}
		}
		return false;
	}

}