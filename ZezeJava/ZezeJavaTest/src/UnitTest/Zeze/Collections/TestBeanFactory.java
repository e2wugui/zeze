package UnitTest.Zeze.Collections;

import Zeze.Collections.BeanFactory;
import demo.Module1.BValue;
import org.junit.Assert;
import org.junit.Test;

public class TestBeanFactory {
	@Test
	public void testFindClass() {
		Assert.assertEquals(BValue.class, BeanFactory.findClass(BValue.TYPEID));
	}
}
