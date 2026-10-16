// auto-generated @formatter:off
package Game.Fight;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import Game.Fight.BFighterReadOnly;
// key is fighter id. 战斗属性是计算值，每次登录或改变时重新计算。
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tfighters extends TableX<Game.Fight.BFighterId, Game.Fight.BFighter>
        implements TableReadOnly<Game.Fight.BFighterId, Game.Fight.BFighter, Game.Fight.BFighterReadOnly> {
    public tfighters() {
        super(-2068280414, "Game_Fight_tfighters");
    }

    public tfighters(String _s_) {
        super(-2068280414, "Game_Fight_tfighters", _s_);
    }

    @Override
    public Class<Game.Fight.BFighterId> getKeyClass() {
        return Game.Fight.BFighterId.class;
    }

    @Override
    public Class<Game.Fight.BFighter> getValueClass() {
        return Game.Fight.BFighter.class;
    }

    public static final int VAR_Attack = 1;
    public static final int VAR_Defence = 2;

    @Override
    public Game.Fight.BFighterId decodeKey(ByteBuffer _os_) {
        var _v_ = new Game.Fight.BFighterId();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Game.Fight.BFighterId _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Game.Fight.BFighterId decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new Game.Fight.BFighterId();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.removeLast();
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Game.Fight.BFighterId _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.removeLast();
    }

    @Override
    public Game.Fight.BFighter newValue() {
        return new Game.Fight.BFighter();
    }

    @Override
    public @Nullable BFighterReadOnly getReadOnly(Game.Fight.BFighterId _k_) {
        return get(_k_);
    }
}
