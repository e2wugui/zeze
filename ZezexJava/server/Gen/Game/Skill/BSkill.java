// auto-generated
package Game.Skill;

import Zeze.Serialize.*;

public final class BSkill extends Zeze.Transaction.Bean implements BSkillReadOnly {
    private int _Id;
    private Zeze.Transaction.DynamicBean _Extra;

    public int getId(){
        if (false == this.isManaged())
            return _Id;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _Id;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Id)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _Id;
    }

    public void setId(int value){
        if (false == this.isManaged()) {
            _Id = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Id(this, value));
    }

    public Zeze.Transaction.DynamicBean getExtra() {
        return _Extra;
    }

    public Game.Skill.BSkillAttackExtra getExtra_Game_Skill_BSkillAttackExtra(){
        return (Game.Skill.BSkillAttackExtra)getExtra().getBean();
    }
    public void setExtra(Game.Skill.BSkillAttackExtra value) {
        getExtra().setBean(value);
    }


    public BSkill() {
         this(0);
    }

    public BSkill(int _varId_) {
        super(_varId_);
        _Extra = new Zeze.Transaction.DynamicBean(2, (_b_) -> GetSpecialTypeIdFromBean_Extra(_b_), (_i_) -> CreateBeanFromSpecialTypeId_Extra(_i_));
    }

    public void Assign(BSkill other) {
        setId(other.getId());
        getExtra().Assign(other.getExtra());
    }

    public BSkill CopyIfManaged() {
        return isManaged() ? Copy() : this;
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

    public final static long TYPEID = -567310546869368175L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__Id extends Zeze.Transaction.Log1<BSkill, Integer> {
        public Log__Id(BSkill self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._Id = this.getValue(); }
    }

    public static long GetSpecialTypeIdFromBean_Extra(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.getTypeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_typeId_ == 5301582953788202231L)
            return 5301582953788202231; // Game.Skill.BSkillAttackExtra
        throw new RuntimeException("Unknown Bean! dynamic@Game.Skill.BSkill:Extra");
    }

    public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_Extra(long typeId) {
        if (typeId == 5301582953788202231L)
            return new Game.Skill.BSkillAttackExtra();
        return null;
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
        sb.append(" ".repeat(level * 4)).append("Game.Skill.BSkill: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("Id").append("=").append(getId()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("Extra").append("=").append(System.lineSeparator());
        getExtra().getBean().BuildString(sb, level + 1);
        sb.append("").append(System.lineSeparator());
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
                case (ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT): 
                    setId(_os_.ReadInt());
                    break;
                case (ByteBuffer.DYNAMIC | 2 << ByteBuffer.TAG_SHIFT): 
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
        if (getId() < 0) return true;
        return false;
    }

}
