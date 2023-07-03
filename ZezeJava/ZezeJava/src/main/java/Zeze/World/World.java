package Zeze.World;

import Zeze.Builtin.World.Move;
import Zeze.Builtin.World.Stop;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;

public class World extends AbstractWorld {
	private final static BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Bean bean) {
		return bean.typeId();
	}

	public static long getSpecialTypeIdFromBean(Data data) {
		return data.typeId();
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static Data createDataFromSpecialTypeId(long typeId) {
		return beanFactory.createDataFromSpecialTypeId(typeId);
	}

	@Override
	protected long ProcessMove(Move p) throws Exception {
		return 0;
	}

	@Override
	protected long ProcessStop(Stop p) throws Exception {
		return 0;
	}
}
