// auto-generated
package Game.Buf;

import Zeze.Serialize.*;

public final class BBuf extends Zeze.Transaction.Bean implements BBufReadOnly {
    private int _Id;
    private long _AttachTime; // 加入时间
    private long _ContinueTime; // 持续时间
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

    public long getAttachTime(){
        if (false == this.isManaged())
            return _AttachTime;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _AttachTime;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__AttachTime)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _AttachTime;
    }

    public void setAttachTime(long value){
        if (false == this.isManaged()) {
            _AttachTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__AttachTime(this, value));
    }

    public long getContinueTime(){
        if (false == this.isManaged())
            return _ContinueTime;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ContinueTime;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ContinueTime)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _ContinueTime;
    }

    public void setContinueTime(long value){
        if (false == this.isManaged()) {
            _ContinueTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ContinueTime(this, value));
    }

    public Zeze.Transaction.DynamicBean getExtra() {
        return _Extra;
    }

    public Game.Buf.BBufExtra getExtra_Game_Buf_BBufExtra(){
        return (Game.Buf.BBufExtra)getExtra().getBean();
    }
    public void setExtra(Game.Buf.BBufExtra value) {
        getExtra().setBean(value);
    }


    public BBuf() {
         this(0);
    }

    public BBuf(int _varId_) {
        super(_varId_);
        _Extra = new Zeze.Transaction.DynamicBean(4, (_b_) -> GetSpecialTypeIdFromBean_Extra(_b_), (_i_) -> CreateBeanFromSpecialTypeId_Extra(_i_));
    }

    public void Assign(BBuf other) {
        setId(other.getId());
        setAttachTime(other.getAttachTime());
        setContinueTime(other.getContinueTime());
        getExtra().Assign(other.getExtra());
    }

    public BBuf CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BBuf Copy() {
        var copy = new BBuf();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BBuf a, BBuf b) {
        BBuf save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = -4634900835369009583L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__Id extends Zeze.Transaction.Log1<BBuf, Integer> {
        public Log__Id(BBuf self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._Id = this.getValue(); }
    }

    private final static class Log__AttachTime extends Zeze.Transaction.Log1<BBuf, Long> {
        public Log__AttachTime(BBuf self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._AttachTime = this.getValue(); }
    }

    private final static class Log__ContinueTime extends Zeze.Transaction.Log1<BBuf, Long> {
        public Log__ContinueTime(BBuf self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._ContinueTime = this.getValue(); }
    }

    public static long GetSpecialTypeIdFromBean_Extra(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.getTypeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_typeId_ == 7506982410669108623L)
            return 7506982410669108623; // Game.Buf.BBufExtra
        throw new RuntimeException("Unknown Bean! dynamic@Game.Buf.BBuf:Extra");
    }

    public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_Extra(long typeId) {
        if (typeId == 7506982410669108623L)
            return new Game.Buf.BBufExtra();
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
        sb.append(" ".repeat(level * 4)).append("Game.Buf.BBuf: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("Id").append("=").append(getId()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("AttachTime").append("=").append(getAttachTime()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ContinueTime").append("=").append(getContinueTime()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("Extra").append("=").append(System.lineSeparator());
        getExtra().getBean().BuildString(sb, level + 1);
        sb.append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(4); // Variables.Count
        _os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getId());
        _os_.WriteInt(ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getAttachTime());
        _os_.WriteInt(ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getContinueTime());
        _os_.WriteInt(ByteBuffer.DYNAMIC | 4 << ByteBuffer.TAG_SHIFT);
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
                case (ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT): 
                    setAttachTime(_os_.ReadLong());
                    break;
                case (ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT): 
                    setContinueTime(_os_.ReadLong());
                    break;
                case (ByteBuffer.DYNAMIC | 4 << ByteBuffer.TAG_SHIFT): 
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
        if (getAttachTime() < 0) return true;
        if (getContinueTime() < 0) return true;
        return false;
    }

}
