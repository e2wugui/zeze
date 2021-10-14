// auto-generated
package Game.Buf;

import Zeze.Serialize.*;

public final class BBufs extends Zeze.Transaction.Bean implements BBufsReadOnly {
    private Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf> _Bufs;

    public Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf> getBufs() {
        return _Bufs;
    }


    public BBufs() {
         this(0);
    }

    public BBufs(int _varId_) {
        super(_varId_);
        _Bufs = new Zeze.Transaction.Collections.PMap2<Integer, Game.Buf.BBuf>(getObjectId() + 1, (_v) -> new Log__Bufs(this, _v));
    }

    public void Assign(BBufs other) {
        getBufs().clear();
        for (var e : other.getBufs().entrySet()) {
            getBufs().put(e.getKey(), e.getValue().Copy());
        }
    }

    public BBufs CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BBufs Copy() {
        var copy = new BBufs();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BBufs a, BBufs b) {
        BBufs save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = -6095071065680829700L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final class Log__Bufs extends Zeze.Transaction.Collections.PMap.LogV<Integer, Game.Buf.BBuf> {
        public Log__Bufs(BBufs host, org.pcollections.PMap<Integer, Game.Buf.BBuf> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 1; }
        public BBufs getBeanTyped() { return (BBufs)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Bufs); }
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
        sb.append(" ".repeat(level * 4)).append("Game.Buf.BBufs: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("Bufs").append("=[").append(System.lineSeparator());
        level++;
        for (var _kv_ : getBufs().entrySet()) {
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
            _os_.WriteInt(getBufs().size());
            for  (var _e_ : getBufs().entrySet())
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
                        getBufs().clear();
                        for (int size = _os_.ReadInt(); size > 0; --size) {
                            int _k_;
                            _k_ = _os_.ReadInt();
                            Game.Buf.BBuf _v_ = new Game.Buf.BBuf();
                            _v_.Decode(_os_);
                            getBufs().put(_k_, _v_);
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
        _Bufs.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        for (var _v_ : getBufs().values())
        {
            if (_v_.NegativeCheck()) return true;
        }
        return false;
    }

}
