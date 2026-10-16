// auto-generated @formatter:off
package Game.Equip;

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
    Game.Item.BHorseExtraReadOnly getExtra_Game_Item_BHorseExtraReadOnly();
    Game.Item.BFoodExtraReadOnly getExtra_Game_Item_BFoodExtraReadOnly();
    Game.Equip.BEquipExtraReadOnly getExtra_Game_Equip_BEquipExtraReadOnly();
}
