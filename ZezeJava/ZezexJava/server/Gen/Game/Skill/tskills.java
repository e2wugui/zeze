// auto-generated @formatter:off
package Game.Skill;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import Game.Skill.BSkillsReadOnly;
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tskills extends TableX<Long, Game.Skill.BSkills>
        implements TableReadOnly<Long, Game.Skill.BSkills, Game.Skill.BSkillsReadOnly> {
    public tskills() {
        super(-1442053111, "Game_Skill_tskills");
    }

    public tskills(String _s_) {
        super(-1442053111, "Game_Skill_tskills", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Game.Skill.BSkills> getValueClass() {
        return Game.Skill.BSkills.class;
    }

    public static final int VAR_Skills = 1;

    @Override
    public Long decodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(_v_));
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Long decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        long _v_;
        _v_ = _s_.getLong("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Long _v_) {
        _s_.appendLong("__key", _v_);
    }

    @Override
    public Game.Skill.BSkills newValue() {
        return new Game.Skill.BSkills();
    }

    @Override
    public @Nullable BSkillsReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
