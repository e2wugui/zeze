// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// key is 1, only one record
@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tItemClasses extends TableX<Integer, Zeze.Builtin.Game.Bag.BItemClasses>
        implements TableReadOnly<Integer, Zeze.Builtin.Game.Bag.BItemClasses, Zeze.Builtin.Game.Bag.BItemClassesReadOnly> {
    public tItemClasses() {
        super(1057953754, "Zeze_Builtin_Game_Bag_tItemClasses");
    }

    public tItemClasses(String suffix) {
        super(1057953754, "Zeze_Builtin_Game_Bag_tItemClasses", suffix);
    }

    public static final int VAR_ItemClasses = 1;

    @Override
    public Integer decodeKey(ByteBuffer _os_) {
        int _v_;
        _v_ = _os_.ReadInt();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Integer _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(_v_));
        _os_.WriteInt(_v_);
        return _os_;
    }

    @Override
    public Integer decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        int _v_;
        _v_ = rs.getInt("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Integer _v_) {
        st.appendInt("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Game.Bag.BItemClasses newValue() {
        return new Zeze.Builtin.Game.Bag.BItemClasses();
    }

    @Override
    public Zeze.Builtin.Game.Bag.BItemClassesReadOnly getReadOnly(Integer key) {
        return get(key);
    }
}
