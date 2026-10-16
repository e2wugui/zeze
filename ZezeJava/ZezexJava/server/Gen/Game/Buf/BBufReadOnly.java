// auto-generated @formatter:off
package Game.Buf;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// buf 应该尽量通过配置控制：冲突，覆盖，增加属性，生命期等
public interface BBufReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BBuf copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getId();
    long getAttachTime();
    long getContinueTime();
    Zeze.Transaction.DynamicBeanReadOnly getExtraReadOnly();
    Game.Buf.BBufExtraReadOnly getExtra_Game_Buf_BBufExtraReadOnly();
}
