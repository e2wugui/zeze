// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BBrowseReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BBrowse copy();
    BBrowse.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    long getId();
    int getLimit();
    float getOffsetFactor();
    boolean isReset();
    Zeze.Builtin.LogService.BConditionReadOnly getConditionReadOnly();
}
