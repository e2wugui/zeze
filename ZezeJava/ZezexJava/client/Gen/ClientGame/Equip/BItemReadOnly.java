// auto-generated @formatter:off
package ClientGame.Equip;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BItemReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BItem copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getId();
    int getNumber();
    Zeze.Transaction.DynamicBeanReadOnly getExtraReadOnly();
    ClientGame.Item.BHorseExtraReadOnly getExtra_ClientGame_Item_BHorseExtraReadOnly();
    ClientGame.Item.BFoodExtraReadOnly getExtra_ClientGame_Item_BFoodExtraReadOnly();
    ClientGame.Equip.BEquipExtraReadOnly getExtra_ClientGame_Equip_BEquipExtraReadOnly();
}
