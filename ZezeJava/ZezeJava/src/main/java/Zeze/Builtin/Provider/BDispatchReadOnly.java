// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// link to gs
public interface BDispatchReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BDispatch copy();
    BDispatch.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    long getLinkSid();
    String getAccount();
    long getProtocolType();
    Zeze.Net.Binary getProtocolData();
    String getContext();
    Zeze.Net.Binary getContextx();
    String getOnlineSetName();
}
