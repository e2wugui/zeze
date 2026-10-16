// auto-generated @formatter:off
package demo.Module1;

public class MappingCMappingBItem {
    public static CMappingBItem create(demo.Module1.BItem base0) {
        var subBean = base0.getSubclass().getBean();
        var subBeanTypeId = subBean.typeId();
        if (subBeanTypeId == demo.Bean1.TYPEID)
            return create(base0, (demo.Bean1)subBean);
        if (subBeanTypeId == demo.Module1.BSimple.TYPEID)
            return create(base0, (demo.Module1.BSimple)subBean);
        if (subBeanTypeId == demo.Module1.BFood.TYPEID)
            return create(base0, (demo.Module1.BFood)subBean);
        throw new UnsupportedOperationException("Unknown Dynamic Bean: " + subBeanTypeId);
    }

    public static CMappingBItemBean1 create(demo.Module1.BItem base0, demo.Bean1 base1) {
       return new CMappingBItemBean1(base0, base1);
    }

    public static CMappingBItemBSimple create(demo.Module1.BItem base0, demo.Module1.BSimple base1) {
       return new CMappingBItemBSimple(base0, base1);
    }

    public static CMappingBItemBFood create(demo.Module1.BItem base0, demo.Module1.BFood base1) {
        var subBean = base1.getSubclass().getBean();
        var subBeanTypeId = subBean.typeId();
        if (subBeanTypeId == demo.Bean1.TYPEID)
            return create(base0, base1, (demo.Bean1)subBean);
        if (subBeanTypeId == demo.Module1.BSimple.TYPEID)
            return create(base0, base1, (demo.Module1.BSimple)subBean);
        throw new UnsupportedOperationException("Unknown Dynamic Bean: " + subBeanTypeId);
    }

    public static CMappingBItemBFoodBean1 create(demo.Module1.BItem base0, demo.Module1.BFood base1, demo.Bean1 base2) {
       return new CMappingBItemBFoodBean1(base0, base1, base2);
    }

    public static CMappingBItemBFoodBSimple create(demo.Module1.BItem base0, demo.Module1.BFood base1, demo.Module1.BSimple base2) {
       return new CMappingBItemBFoodBSimple(base0, base1, base2);
    }
}
