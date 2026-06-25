// auto-generated @formatter:off
package metagame.builtin.World;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// key is serverId，一台服务器一个记录。
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tLoad extends TableX<metagame.builtin.World.BServerMapKey, metagame.builtin.World.BLoadMap>
        implements TableReadOnly<metagame.builtin.World.BServerMapKey, metagame.builtin.World.BLoadMap, metagame.builtin.World.BLoadMapReadOnly> {
    public tLoad() {
        super(-856501501, "metagame_builtin_World_tLoad");
    }

    public tLoad(String _s_) {
        super(-856501501, "metagame_builtin_World_tLoad", _s_);
    }

    @Override
    public Class<metagame.builtin.World.BServerMapKey> getKeyClass() {
        return metagame.builtin.World.BServerMapKey.class;
    }

    @Override
    public Class<metagame.builtin.World.BLoadMap> getValueClass() {
        return metagame.builtin.World.BLoadMap.class;
    }

    public static final int VAR_MapId = 1;
    public static final int VAR_LoadSum = 2;
    public static final int VAR_Instances = 3;

    @Override
    public metagame.builtin.World.BServerMapKey decodeKey(ByteBuffer _os_) {
        var _v_ = new metagame.builtin.World.BServerMapKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(metagame.builtin.World.BServerMapKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public metagame.builtin.World.BServerMapKey decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new metagame.builtin.World.BServerMapKey();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, metagame.builtin.World.BServerMapKey _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public metagame.builtin.World.BLoadMap newValue() {
        return new metagame.builtin.World.BLoadMap();
    }

    @Override
    public metagame.builtin.World.BLoadMapReadOnly getReadOnly(metagame.builtin.World.BServerMapKey _k_) {
        return get(_k_);
    }
}
