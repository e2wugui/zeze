// auto-generated @formatter:off
package Game.Fight;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// 战斗相关属性，用于技能等。
public interface BFighterReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BFighter copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    float getAttack();
    float getDefence();
}
