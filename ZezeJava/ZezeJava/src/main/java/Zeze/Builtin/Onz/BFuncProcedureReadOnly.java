// auto-generated @formatter:off
package Zeze.Builtin.Onz;

// <enum name="eFlushPeriod" value="3"/>
public interface BFuncProcedureReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BFuncProcedure copy();
    BFuncProcedure.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    long getOnzTid();
    String getFuncName();
    Zeze.Net.Binary getFuncArgument();
    int getFlushMode();
    int getFlushTimeout();
}
