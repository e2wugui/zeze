// auto-generated @formatter:off
package Game.Rank;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface TestHashResultReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    TestHashResult copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getHash();
    int getOut();
    int getServerId();
    boolean isB_oolean();
    byte getB_yte();
    short getS_hort();
    float getF_loat();
    long getL_ong();
    double getD_ouble();
    String getS_tring();
    Zeze.Net.Binary getB_inary();
    Game.Rank.TestToServerResultReadOnly getB_eanReadOnly();
}
