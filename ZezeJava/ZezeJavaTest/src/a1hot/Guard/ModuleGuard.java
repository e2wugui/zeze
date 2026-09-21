package a1hot.Guard;

/**
 * TestHotRedirectLoadFirst 冷抢载守卫用例的classpath占位类：
 * HotModule装载"a1hot.Guard.ModuleGuard"时，本类经双亲委派抢先命中，
 * 用于触发HotModule构造器的loader身份fail-fast。
 */
public class ModuleGuard {
}
