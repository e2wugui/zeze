// auto-generated @formatter:off
package demo.Game;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BRoleReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BRole copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getLevel();
    long getExp();
    long getMoney();
    int getHp();
    long getDamageTotal();
    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, demo.Game.BItem, demo.Game.BItemReadOnly> getBagReadOnly();
    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, demo.Game.BItem, demo.Game.BItemReadOnly> getEquipReadOnly();
    Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getFriendsReadOnly();
    Zeze.Transaction.Collections.PList1ReadOnly<Long> getTracksReadOnly();
    Zeze.Transaction.Collections.PList1ReadOnly<Long> getQuestLogReadOnly();
    Zeze.Transaction.Collections.PList1ReadOnly<Long> getGiftLogReadOnly();
    Zeze.Transaction.Collections.PList2ReadOnly<demo.Game.BItem, demo.Game.BItemReadOnly> getWarehouseReadOnly();
}
