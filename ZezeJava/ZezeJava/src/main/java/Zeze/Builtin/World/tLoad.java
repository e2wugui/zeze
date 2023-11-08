// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// key is serverId，一台服务器一个记录。
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tLoad extends TableX<Zeze.Builtin.World.BServerMapKey, Zeze.Builtin.World.BLoadMap>
        implements TableReadOnly<Zeze.Builtin.World.BServerMapKey, Zeze.Builtin.World.BLoadMap, Zeze.Builtin.World.BLoadMapReadOnly> {
    public tLoad() {
        super(794282960, "Zeze_Builtin_World_tLoad");
    }

    public tLoad(String suffix) {
        super(794282960, "Zeze_Builtin_World_tLoad", suffix);
    }

    public static final int VAR_MapId = 1;
    public static final int VAR_LoadSum = 2;
    public static final int VAR_Instances = 3;

    @Override
    public Zeze.Builtin.World.BServerMapKey decodeKey(ByteBuffer _os_) {
        Zeze.Builtin.World.BServerMapKey _v_ = new Zeze.Builtin.World.BServerMapKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.World.BServerMapKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.World.BServerMapKey decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        var parents = new java.util.ArrayList<String>();
        Zeze.Builtin.World.BServerMapKey _v_ = new Zeze.Builtin.World.BServerMapKey();
        parents.add("__key");
        _v_.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Zeze.Builtin.World.BServerMapKey _v_) {
        var parents = new java.util.ArrayList<String>();
        parents.add("__key");
        _v_.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }

    @Override
    public Zeze.Builtin.World.BLoadMap newValue() {
        return new Zeze.Builtin.World.BLoadMap();
    }

    @Override
    public Zeze.Builtin.World.BLoadMapReadOnly getReadOnly(Zeze.Builtin.World.BServerMapKey key) {
        return get(key);
    }
}
