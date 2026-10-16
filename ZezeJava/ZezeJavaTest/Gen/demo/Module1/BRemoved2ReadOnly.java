// auto-generated @formatter:off
package demo.Module1;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

/*
		bean的
		多行注释
*/
public interface BRemoved2ReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BRemoved2 copy();
    BRemoved2.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getInt_1();
}
