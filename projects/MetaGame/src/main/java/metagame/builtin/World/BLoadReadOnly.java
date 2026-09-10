// auto-generated @formatter:off
package metagame.builtin.World;

// 地图实例（线）的负载
public interface BLoadReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BLoad copy();
    BLoad.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getPlayerCount();
    long getComputeCount();
    long getComputeCountPS();
    long getComputeCountLast();
    long getComputeCountTime();
}
