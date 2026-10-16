// auto-generated @formatter:off
package Game.Map;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BEnterWorldNowReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BEnterWorldNow copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getMapInstanceId();
    float getX();
    float getY();
    float getZ();
    int getResouceId();
}
