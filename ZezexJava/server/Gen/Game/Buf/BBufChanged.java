// auto-generated
package Game.Buf;

import Zeze.Serialize.*;

public final class BBufChanged extends Zeze.Transaction.Bean implements BBufChangedReadOnly {
    public static final int ChangeTagNormalChanged = 0; // 普通增量修改。
    public static final int ChangeTagRecordIsRemoved = 1; // 整个记录删除了。
    public static final int ChangeTagRecordChanged = 2; // 整个记录发生了变更，需要先清除本地数据，再替换进去。

    private Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf> _Replace; // key is bufId
    private Zeze.Transaction.Collections.PSet1<Integer > _Remove; // key is bufId
    private int _ChangeTag;

    public Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf> getReplace() {
        return _Replace;
    }

    public Zeze.Transaction.Collections.PSet1<Integer > getRemove() {
        return _Remove;
    }

    public int getChangeTag(){
        if (false == this.isManaged())
            return _ChangeTag;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ChangeTag;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ChangeTag)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _ChangeTag;
    }

    public void setChangeTag(int value){
        if (false == this.isManaged()) {
            _ChangeTag = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ChangeTag(this, value));
    }


    public BBufChanged() {
         this(0);
    }

    public BBufChanged(int _varId_) {
        super(_varId_);
        _Replace = new Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf>(getObjectId() + 1, (_v) -> new Log__Replace(this, _v));
        _Remove = new Zeze.Transaction.Collections.PSet1<Integer >(getObjectId() + 2, (_v) -> new Log__Remove(this, _v));
    }

    public void Assign(BBufChanged other) {
        getReplace().clear();
        for (var e : other.getReplace().entrySet()) {
            getReplace().put(e.getKey(), e.getValue().Copy());
        }
        getRemove().clear();
        for (var e : other.getRemove()) {
            getRemove().add(e);
        }
        setChangeTag(other.getChangeTag());
    }

    public BBufChanged CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BBufChanged Copy() {
        var copy = new BBufChanged();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BBufChanged a, BBufChanged b) {
        BBufChanged save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 8288333028989651451L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final class Log__Replace extends Zeze.Transaction.Collections.PMap.LogV<Integer, Game.Buf.BBuf> {
        public Log__Replace(BBufChanged host, org.pcollections.PMap<Integer, Game.Buf.BBuf> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 1; }
        public BBufChanged getBeanTyped() { return (BBufChanged)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Replace); }
    }

    private final class Log__Remove extends Zeze.Transaction.Collections.PSet.LogV<Integer> {
        public Log__Remove(BBufChanged host, org.pcollections.PSet<Integer> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 2; }
        public BBufChanged getBeanTyped() { return (BBufChanged)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Remove); }
    }

    private final static class Log__ChangeTag extends Zeze.Transaction.Log1<BBufChanged, Integer> {
        public Log__ChangeTag(BBufChanged self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._ChangeTag = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Game.Buf.BBufChanged: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("Replace").append("=[").append(System.lineSeparator());
        level++;
        for (var _kv_ : getReplace().entrySet()) {
            sb.append("(").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Key").append("=").append(_kv_.getKey()).append(",").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Value").append("=").append(System.lineSeparator());
            _kv_.getValue().BuildString(sb, level + 1);
            sb.append(",").append(System.lineSeparator());
            sb.append(")").append(System.lineSeparator());
        }
        level--;
        sb.append(" ".repeat(level * 4)).append("],").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("Remove").append("=[").append(System.lineSeparator());
        level++;
        for (var _item_ : getRemove()) {
            sb.append(" ".repeat(level * 4)).append("Item").append("=").append(_item_).append(",").append(System.lineSeparator());
        }
        level--;
        sb.append(" ".repeat(level * 4)).append("],").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ChangeTag").append("=").append(getChangeTag()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(3); // Variables.Count
        _os_.WriteInt(ByteBuffer.MAP | 1 << ByteBuffer.TAG_SHIFT);
        {
            var _state_ = _os_.BeginWriteSegment();
            _os_.WriteInt(ByteBuffer.INT);
            _os_.WriteInt(ByteBuffer.BEAN);
            _os_.WriteInt(getReplace().size());
            for  (var _e_ : getReplace().entrySet())
            {
                _os_.WriteInt(_e_.getKey());
                _e_.getValue().Encode(_os_);
            }
            _os_.EndWriteSegment(_state_); 
        }
        _os_.WriteInt(ByteBuffer.SET | 2 << ByteBuffer.TAG_SHIFT);
        {
            var _state_ = _os_.BeginWriteSegment();
            _os_.WriteInt(ByteBuffer.INT);
            _os_.WriteInt(getRemove().size());
            for (var _v_ : getRemove()) {
                _os_.WriteInt(_v_);
            }
            _os_.EndWriteSegment(_state_); 
        }
        _os_.WriteInt(ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getChangeTag());
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
                        getReplace().clear();
                        for (int size = _os_.ReadInt(); size > 0; --size) {
                            int _k_;
                            _k_ = _os_.ReadInt();
                            Game.Buf.BBuf _v_ = new Game.Buf.BBuf();
                            _v_.Decode(_os_);
                            getReplace().put(_k_, _v_);
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case (ByteBuffer.SET | 2 << ByteBuffer.TAG_SHIFT):
                    {
                        var _state_ = _os_.BeginReadSegment();
                        _os_.ReadInt(); // skip collection.value typetag
                        getRemove().clear();
                        for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
                            int _v_;
                            _v_ = _os_.ReadInt();
                            getRemove().add(_v_);
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case (ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT): 
                    setChangeTag(_os_.ReadInt());
                    break;
                default:
                    ByteBuffer.SkipUnknownField(_tagid_, _os_);
                    break;
            }
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Replace.InitRootInfo(root, this);
        _Remove.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        for (var _v_ : getReplace().values())
        {
            if (_v_.NegativeCheck()) return true;
        }
        for (var _v_ : getRemove())
        {
            if (_v_ < 0) return true;
        }
        if (getChangeTag() < 0) return true;
        return false;
    }

}
