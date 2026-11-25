package Zeze.Hot;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Transaction.BeanKey;
import Zeze.Transaction.Table;
import Zeze.Util.OutObject;

/**
 * 内存表热更迁移：把旧表数据逐行重解码进新表并禁用旧表，键类型不兼容则整体中止。
 */
public class HotUpgradeMemoryTable {
	private final Table old;
	private final Table cur;

	public HotUpgradeMemoryTable(Table old, Table cur) {
		this.old = old;
		this.cur = cur;
	}

	private static boolean isKeyTypeCompatible(Class<?> oldKeyClass, Class<?> curKeyClass) {
		if (oldKeyClass == curKeyClass)
			return true;
		// 热更模块 -privateBean 模式下，同名 BeanKey 由新旧 HotModule 各自的类加载器独立加载：
		// Class 身份不同但属于同一逻辑 BeanKey 换代，可通过 old.encodeKey -> cur.decodeKey 跨类加载器迁移。
		return oldKeyClass.getName().equals(curKeyClass.getName())
				&& BeanKey.class.isAssignableFrom(oldKeyClass)
				&& BeanKey.class.isAssignableFrom(curKeyClass);
	}

	public void upgrade() throws Exception {
		var oldKeyClass = old.getKeyClass();
		var curKeyClass = cur.getKeyClass();
		if (!isKeyTypeCompatible(oldKeyClass, curKeyClass)) {
			// fail-fast回滚：键类型不兼容即在迁移前整体中止且保留旧表（不触碰cur、零半迁移）。
			// 本调用处于热更"不能出错阶段"，异常上抛按该阶段契约停止程序，外部拉起修正jar后重试。
			throw new IllegalStateException("HotUpgradeMemoryTable: new table rejects old key type ("
					+ oldKeyClass.getName() + " -> " + curKeyClass.getName() + "), keep old table enabled");
		}
		var crossLoaderBeanKey = oldKeyClass != curKeyClass;
		var first = new OutObject<>(true);
		var incompatible = new OutObject<Exception>();
		old.walkMemoryAny((k, v) -> {
			// 非跨类加载器 BeanKey 保留首键 encodeKey 校验；跨类加载器同名 BeanKey 经由序列化字节迁移。
			//noinspection DataFlowIssue
			if (first.value) {
				first.value = false;
				if (!crossLoaderBeanKey) {
					try {
						cur.encodeKey(k);
					} catch (Exception ex) {
						incompatible.value = ex;
						return false; // 首键即中止：cur内零迁移数据，无半迁移
					}
				}
			}
			// key value retreat
			var bbKey = old.encodeKey(k);
			var newKey = cur.decodeKey(bbKey);
			if (crossLoaderBeanKey && !bbKey.isEmpty()) {
				incompatible.value = new IllegalStateException(
						"HotUpgradeMemoryTable: key decode left trailing bytes: " + bbKey.size());
				return false;
			}
			var bbValue = ByteBuffer.Allocate();
			v.encode(bbValue);
			var newValue = cur.newValue();
			newValue.decode(bbValue);
			cur.__direct_put_cache__(newKey, newValue, GlobalCacheManagerConst.StateModify);
			return true;
		});
		if (incompatible.value != null) {
			// fail-fast回滚：首键不兼容即整体中止且保留旧表——静默容忍+仍禁用旧表是半迁移
			// 假成功（数据留在已禁用表上不可达）。本调用处于热更"不能出错阶段"，异常上抛
			// 按该阶段契约停止程序，外部拉起修正jar后重试。
			throw new IllegalStateException("HotUpgradeMemoryTable: new table rejects old key type, keep old table enabled", incompatible.value);
		}
		old.disable();
	}
}
