// auto-generated
package Game.Bag;

public abstract class AbstractModule extends Zeze.IModule {
    public String getFullName() { return "Game.Bag"; }
    public String getName() { return "Bag"; }
    public int getId() { return 2; }

    public final static int ResultCodeFromInvalid = 1;
    public final static int ResultCodeToInvalid = 2;
    public final static int ResultCodeFromNotExsit = 3;
    public final static int ResultCodeTrySplitButTargetExsitDifferenceItem = 4;

    public abstract int ProcessCUse(Zeze.Net.Protocol _p);

    public abstract int ProcessDestroyRequest(Zeze.Net.Protocol _p);

    public abstract int ProcessGetBagRequest(Zeze.Net.Protocol _p);

    public abstract int ProcessMoveRequest(Zeze.Net.Protocol _p);

    public abstract int ProcessSortRequest(Zeze.Net.Protocol _p);

}
