// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import demo.Bean1ReadOnly;
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tAutoKeyRandom extends TableX<Zeze.Net.Binary, demo.Bean1>
        implements TableReadOnly<Zeze.Net.Binary, demo.Bean1, demo.Bean1ReadOnly> {
    public tAutoKeyRandom() {
        super(447337544, "demo_Module1_tAutoKeyRandom");
    }

    public tAutoKeyRandom(String _s_) {
        super(447337544, "demo_Module1_tAutoKeyRandom", _s_);
    }

    @Override
    public Class<Zeze.Net.Binary> getKeyClass() {
        return Zeze.Net.Binary.class;
    }

    @Override
    public Class<demo.Bean1> getValueClass() {
        return demo.Bean1.class;
    }

    public static final int VAR_V1 = 1;
    public static final int VAR_V2 = 2;

    public Zeze.Net.Binary insert(demo.Bean1 _v_) {
        var _k_ = Zeze.Util.Random.nextBinary(16);
        while (!tryAdd(_k_, _v_))
            _k_ = Zeze.Util.Random.nextBinary(16);
        return _k_;
    }

    @Override
    public Zeze.Net.Binary decodeKey(ByteBuffer _os_) {
        Zeze.Net.Binary _v_;
        _v_ = _os_.ReadBinary();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Net.Binary _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteBinary(_v_);
        return _os_;
    }

    @Override
    public Zeze.Net.Binary decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        Zeze.Net.Binary _v_;
        _v_ = new Zeze.Net.Binary(_s_.getBytes("__key"));
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Zeze.Net.Binary _v_) {
        _s_.appendBinary("__key", _v_);
    }

    @Override
    public demo.Bean1 newValue() {
        return new demo.Bean1();
    }

    @Override
    public @Nullable Bean1ReadOnly getReadOnly(Zeze.Net.Binary _k_) {
        return get(_k_);
    }
}
