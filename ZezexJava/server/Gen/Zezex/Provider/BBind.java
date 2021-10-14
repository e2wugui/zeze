// auto-generated
package Zezex.Provider;

import Zeze.Serialize.*;

public final class BBind extends Zeze.Transaction.Bean implements BBindReadOnly {
    public static final int ResultSuccess = 0;
    public static final int ResultFaild = 1;

    private Zeze.Transaction.Collections.PMap2<Integer, Zezex.Provider.BModule> _modules; // moduleId -> type
    private Zeze.Transaction.Collections.PSet1<Long > _linkSids;

    public Zeze.Transaction.Collections.PMap2<Integer, Zezex.Provider.BModule> getModules() {
        return _modules;
    }

    public Zeze.Transaction.Collections.PSet1<Long > getLinkSids() {
        return _linkSids;
    }


    public BBind() {
         this(0);
    }

    public BBind(int _varId_) {
        super(_varId_);
        _modules = new Zeze.Transaction.Collections.PMap2<Integer, Zezex.Provider.BModule>(getObjectId() + 1, (_v) -> new Log__modules(this, _v));
        _linkSids = new Zeze.Transaction.Collections.PSet1<Long >(getObjectId() + 2, (_v) -> new Log__linkSids(this, _v));
    }

    public void Assign(BBind other) {
        getModules().clear();
        for (var e : other.getModules().entrySet()) {
            getModules().put(e.getKey(), e.getValue().Copy());
        }
        getLinkSids().clear();
        for (var e : other.getLinkSids()) {
            getLinkSids().add(e);
        }
    }

    public BBind CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BBind Copy() {
        var copy = new BBind();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BBind a, BBind b) {
        BBind save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 8933110584444310889L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final class Log__modules extends Zeze.Transaction.Collections.PMap.LogV<Integer, Zezex.Provider.BModule> {
        public Log__modules(BBind host, org.pcollections.PMap<Integer, Zezex.Provider.BModule> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 1; }
        public BBind getBeanTyped() { return (BBind)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._modules); }
    }

    private final class Log__linkSids extends Zeze.Transaction.Collections.PSet.LogV<Long> {
        public Log__linkSids(BBind host, org.pcollections.PSet<Long> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 2; }
        public BBind getBeanTyped() { return (BBind)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._linkSids); }
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
        sb.append(" ".repeat(level * 4)).append("Zezex.Provider.BBind: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("modules").append("=[").append(System.lineSeparator());
        level++;
        for (var _kv_ : getModules().entrySet()) {
            sb.append("(").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Key").append("=").append(_kv_.getKey()).append(",").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Value").append("=").append(System.lineSeparator());
            _kv_.getValue().BuildString(sb, level + 1);
            sb.append(",").append(System.lineSeparator());
            sb.append(")").append(System.lineSeparator());
        }
        level--;
        sb.append(" ".repeat(level * 4)).append("],").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("linkSids").append("=[").append(System.lineSeparator());
        level++;
        for (var _item_ : getLinkSids()) {
            sb.append(" ".repeat(level * 4)).append("Item").append("=").append(_item_).append(",").append(System.lineSeparator());
        }
        level--;
        sb.append(" ".repeat(level * 4)).append("]").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(2); // Variables.Count
        _os_.WriteInt(ByteBuffer.MAP | 1 << ByteBuffer.TAG_SHIFT);
        {
            var _state_ = _os_.BeginWriteSegment();
            _os_.WriteInt(ByteBuffer.INT);
            _os_.WriteInt(ByteBuffer.BEAN);
            _os_.WriteInt(getModules().size());
            for  (var _e_ : getModules().entrySet())
            {
                _os_.WriteInt(_e_.getKey());
                _e_.getValue().Encode(_os_);
            }
            _os_.EndWriteSegment(_state_); 
        }
        _os_.WriteInt(ByteBuffer.SET | 2 << ByteBuffer.TAG_SHIFT);
        {
            var _state_ = _os_.BeginWriteSegment();
            _os_.WriteInt(ByteBuffer.LONG);
            _os_.WriteInt(getLinkSids().size());
            for (var _v_ : getLinkSids()) {
                _os_.WriteLong(_v_);
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
                        getModules().clear();
                        for (int size = _os_.ReadInt(); size > 0; --size) {
                            int _k_;
                            _k_ = _os_.ReadInt();
                            Zezex.Provider.BModule _v_ = new Zezex.Provider.BModule();
                            _v_.Decode(_os_);
                            getModules().put(_k_, _v_);
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case (ByteBuffer.SET | 2 << ByteBuffer.TAG_SHIFT):
                    {
                        var _state_ = _os_.BeginReadSegment();
                        _os_.ReadInt(); // skip collection.value typetag
                        getLinkSids().clear();
                        for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
                            long _v_;
                            _v_ = _os_.ReadLong();
                            getLinkSids().add(_v_);
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
        _modules.InitRootInfo(root, this);
        _linkSids.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        for (var _v_ : getModules().values())
        {
            if (_v_.NegativeCheck()) return true;
        }
        for (var _v_ : getLinkSids())
        {
            if (_v_ < 0) return true;
        }
        return false;
    }

}
