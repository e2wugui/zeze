// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

public final class BLocal extends Zeze.Transaction.Bean {
    private final Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.Game.Online.BAny> _Datas;

    public Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.Game.Online.BAny> getDatas() {
        return _Datas;
    }

    public BLocal() {
         this(0);
    }

    public BLocal(int _varId_) {
        super(_varId_);
        _Datas = new Zeze.Transaction.Collections.PMap2<>(getObjectId() + 1, (_v) -> new Log__Datas(this, _v));
    }

    public void Assign(BLocal other) {
        getDatas().clear();
        for (var e : other.getDatas().entrySet())
            getDatas().put(e.getKey(), e.getValue().Copy());
    }

    public BLocal CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BLocal Copy() {
        var copy = new BLocal();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BLocal a, BLocal b) {
        BLocal save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 1038509325594826174L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__Datas extends Zeze.Transaction.Collections.PMap.LogV<Integer, Zeze.Builtin.Game.Online.BAny> {
        public Log__Datas(BLocal host, org.pcollections.PMap<Integer, Zeze.Builtin.Game.Online.BAny> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 1; }
        public BLocal getBeanTyped() { return (BLocal)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Datas); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Online.BLocal: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Datas").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getDatas().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().BuildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            var _x_ = getDatas();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().Encode(_o_);
                }
            }
        }
        _o_.WriteByte(0);
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = getDatas();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Game.Online.BAny(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Datas.InitRootInfo(root, this);
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean NegativeCheck() {
        return false;
    }
}
