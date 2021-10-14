// auto-generated
package Game.Equip;

public abstract class AbstractModule extends Zeze.IModule {
    public String getFullName() { return "Game.Equip"; }
    public String getName() { return "Equip"; }
    public int getId() { return 7; }

    public final static int ResultCodeCannotEquip = 1;
    public final static int ResultCodeItemNotFound = 2;
    public final static int ResultCodeBagIsFull = 3;
    public final static int ResultCodeEquipNotFound = 4;

    public abstract int ProcessEquipementRequest(Zeze.Net.Protocol _p);

    public abstract int ProcessUnequipementRequest(Zeze.Net.Protocol _p);

}
