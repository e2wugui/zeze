package Zeze.Collections;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import Zeze.Transaction.Bean;
import dynamicred.Demo.Order;
import harness.Fast;
import harness.ForkedJvm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Fast
@ForkedJvm
public class TestRuntimeBeanTypeWatch {
	@Test
	public void replacementWatcherReceivesInFlightRegistration() {
		var observed = new ArrayList<Class<? extends Bean>>();
		Consumer<Class<? extends Bean>> replacement = observed::add;
		var initial = new Consumer<Class<? extends Bean>>() {
			@Override
			public void accept(Class<? extends Bean> type) {
				BeanFactory.unwatchRuntimeBeanTypes(this);
				assertFalse(BeanFactory.watchRuntimeBeanTypes(replacement).contains(type));
			}
		};

		BeanFactory.watchRuntimeBeanTypes(initial);
		try {
			new BeanFactory().register(Order.class);
			assertEquals(List.of(Order.class), observed);
		} finally {
			BeanFactory.unwatchRuntimeBeanTypes(initial);
			BeanFactory.unwatchRuntimeBeanTypes(replacement);
		}
	}
}
