package UnitTest.Zeze.Collections;

import harness.Fast;
import Zeze.Collections.BeanFactory;
import demo.Module1.BValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestBeanFactory {
	@Test
	public void testFindClass() {
		Assertions.assertEquals(BValue.class, BeanFactory.findClass(BValue.TYPEID));
	}
}
