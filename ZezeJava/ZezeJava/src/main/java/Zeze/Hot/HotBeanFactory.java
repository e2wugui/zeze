package Zeze.Hot;

import Zeze.Collections.BeanFactory;

/**
 * 用于zeze内部跟BeanFactory相关的重置。
 * 一般是内部组件用了dynamic创建bean实例，当这些组件由HotModule使用时，
 * 需要在热更后修改BeanFactory并且清除缓存(Cache)。
 *
 * 1. checkpoint
 * 2. BeanFactory.resetHot(bcs, hotModules);
 * 3. for (var hotbc:HotBeanFactory) hotbc.reset();
 */
public interface HotBeanFactory {
	void clearTableCache();
	BeanFactory beanFactory();
	boolean hasFreshStopModuleDynamicOnce();
}
