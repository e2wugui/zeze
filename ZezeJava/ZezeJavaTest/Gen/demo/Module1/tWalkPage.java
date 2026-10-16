// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import demo.Bean1ReadOnly;
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tWalkPage extends TableX<Integer, demo.Bean1>
        implements TableReadOnly<Integer, demo.Bean1, demo.Bean1ReadOnly> {
    public tWalkPage() {
        super(705996042, "demo_Module1_tWalkPage");
    }

    public tWalkPage(String _s_) {
        super(705996042, "demo_Module1_tWalkPage", _s_);
    }

    @Override
    public Class<Integer> getKeyClass() {
        return Integer.class;
    }

    @Override
    public Class<demo.Bean1> getValueClass() {
        return demo.Bean1.class;
    }

    @Override
    public boolean isRelationalMapping() {
        return true;
    }

    public static final int VAR_V1 = 1;
    public static final int VAR_V2 = 2;

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
    public Integer decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        int _v_;
        _v_ = _s_.getInt("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Integer _v_) {
        _s_.appendInt("__key", _v_);
    }

    @Override
    public demo.Bean1 newValue() {
        return new demo.Bean1();
    }

    @Override
    public @Nullable Bean1ReadOnly getReadOnly(Integer _k_) {
        return get(_k_);
    }
}
