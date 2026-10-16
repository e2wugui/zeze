// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// Zeze.Transaction.Database.Operates 实现协议
public interface BSetInUseReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BSetInUse copy();
    BSetInUse.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getLocalId();
    String getGlobal();
}
