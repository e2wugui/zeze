// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tbag extends TableX<Zeze.Builtin.Game.Bag.BBagKey, Zeze.Builtin.Game.Bag.BBag>
        implements TableReadOnly<Zeze.Builtin.Game.Bag.BBagKey, Zeze.Builtin.Game.Bag.BBag, Zeze.Builtin.Game.Bag.BBagReadOnly> {
    public tbag() {
        super(863603985, "Zeze_Builtin_Game_Bag_tbag");
    }

    public tbag(String _s_) {
        super(863603985, "Zeze_Builtin_Game_Bag_tbag", _s_);
    }

    @Override
    public Class<Zeze.Builtin.Game.Bag.BBagKey> getKeyClass() {
        return Zeze.Builtin.Game.Bag.BBagKey.class;
    }

    @Override
    public Class<Zeze.Builtin.Game.Bag.BBag> getValueClass() {
        return Zeze.Builtin.Game.Bag.BBag.class;
    }

    public static final int VAR_Capacity = 1;
    public static final int VAR_Items = 2;

    @Override
    public Zeze.Builtin.Game.Bag.BBagKey decodeKey(ByteBuffer _os_) {
        var _v_ = new Zeze.Builtin.Game.Bag.BBagKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Game.Bag.BBagKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Game.Bag.BBagKey decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new Zeze.Builtin.Game.Bag.BBagKey();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.removeLast();
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Zeze.Builtin.Game.Bag.BBagKey _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.removeLast();
    }

    @Override
    public Zeze.Builtin.Game.Bag.BBag newValue() {
        return new Zeze.Builtin.Game.Bag.BBag();
    }

    @Override
    public Zeze.Builtin.Game.Bag.BBagReadOnly getReadOnly(Zeze.Builtin.Game.Bag.BBagKey _k_) {
        return get(_k_);
    }
}
