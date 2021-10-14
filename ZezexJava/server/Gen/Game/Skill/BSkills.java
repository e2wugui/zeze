// auto-generated
package Game.Skill;

import Zeze.Serialize.*;

public final class BSkills extends Zeze.Transaction.Bean implements BSkillsReadOnly {
    private Zeze.Transaction.Collections.PMap2<Integer, Game.Skill.BSkill> _Skills;

    public Zeze.Transaction.Collections.PMap2<Integer, Game.Skill.BSkill> getSkills() {
        return _Skills;
    }


    public BSkills() {
         this(0);
    }

    public BSkills(int _varId_) {
        super(_varId_);
        _Skills = new Zeze.Transaction.Collections.PMap2<Integer, Game.Skill.BSkill>(getObjectId() + 1, (_v) -> new Log__Skills(this, _v));
    }

    public void Assign(BSkills other) {
        getSkills().clear();
        for (var e : other.getSkills().entrySet()) {
            getSkills().put(e.getKey(), e.getValue().Copy());
        }
    }

    public BSkills CopyIfManaged() {
        return isManaged() ? Copy() : this;
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

    public final static long TYPEID = -6850420950998469956L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final class Log__Skills extends Zeze.Transaction.Collections.PMap.LogV<Integer, Game.Skill.BSkill> {
        public Log__Skills(BSkills host, org.pcollections.PMap<Integer, Game.Skill.BSkill> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 1; }
        public BSkills getBeanTyped() { return (BSkills)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Skills); }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(" ".repeat(level * 4)).append("Game.Skill.BSkills: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("Skills").append("=[").append(System.lineSeparator());
        level++;
        for (var _kv_ : getSkills().entrySet()) {
            sb.append("(").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Key").append("=").append(_kv_.getKey()).append(",").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Value").append("=").append(System.lineSeparator());
            _kv_.getValue().BuildString(sb, level + 1);
            sb.append(",").append(System.lineSeparator());
            sb.append(")").append(System.lineSeparator());
        }
        level--;
        sb.append(" ".repeat(level * 4)).append("]").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(1); // Variables.Count
        _os_.WriteInt(ByteBuffer.MAP | 1 << ByteBuffer.TAG_SHIFT);
        {
            var _state_ = _os_.BeginWriteSegment();
            _os_.WriteInt(ByteBuffer.INT);
            _os_.WriteInt(ByteBuffer.BEAN);
            _os_.WriteInt(getSkills().size());
            for  (var _e_ : getSkills().entrySet())
            {
                _os_.WriteInt(_e_.getKey());
                _e_.getValue().Encode(_os_);
            }
            _os_.EndWriteSegment(_state_); 
        }
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.MAP | 1 << ByteBuffer.TAG_SHIFT):
                    {
                        var _state_ = _os_.BeginReadSegment();
                        _os_.ReadInt(); // skip key typetag
                        _os_.ReadInt(); // skip value typetag
                        getSkills().clear();
                        for (int size = _os_.ReadInt(); size > 0; --size) {
                            int _k_;
                            _k_ = _os_.ReadInt();
                            Game.Skill.BSkill _v_ = new Game.Skill.BSkill();
                            _v_.Decode(_os_);
                            getSkills().put(_k_, _v_);
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
        for (var _v_ : getSkills().values())
        {
            if (_v_.NegativeCheck()) return true;
        }
        return false;
    }

}
