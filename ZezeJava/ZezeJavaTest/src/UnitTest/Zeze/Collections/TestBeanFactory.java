package UnitTest.Zeze.Collections;

import Zeze.Collections.BeanFactory;
import demo.Module1.BValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestBeanFactory {
	@Test
	public void testFindClass() {
		Assertions.assertEquals(BValue.class, BeanFactory.findClass(BValue.TYPEID));
	}
}
