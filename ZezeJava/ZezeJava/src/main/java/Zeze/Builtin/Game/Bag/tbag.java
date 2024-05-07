// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// key is bag name
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tbag extends TableX<String, Zeze.Builtin.Game.Bag.BBag>
        implements TableReadOnly<String, Zeze.Builtin.Game.Bag.BBag, Zeze.Builtin.Game.Bag.BBagReadOnly> {
    public tbag() {
        super(863603985, "Zeze_Builtin_Game_Bag_tbag");
    }

    public tbag(String suffix) {
        super(863603985, "Zeze_Builtin_Game_Bag_tbag", suffix);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.Game.Bag.BBag> getValueClass() {
        return Zeze.Builtin.Game.Bag.BBag.class;
    }

    public static final int VAR_Capacity = 1;
    public static final int VAR_Items = 2;

    @Override
    public String decodeKey(ByteBuffer _os_) {
        String _v_;
        _v_ = _os_.ReadString();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(String _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteString(_v_);
        return _os_;
    }

    @Override
    public String decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        String _v_;
        _v_ = rs.getString("__key");
        if (_v_ == null)
            _v_ = "";
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, String _v_) {
        st.appendString("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Game.Bag.BBag newValue() {
        return new Zeze.Builtin.Game.Bag.BBag();
    }

    @Override
    public Zeze.Builtin.Game.Bag.BBagReadOnly getReadOnly(String key) {
        return get(key);
    }
}
