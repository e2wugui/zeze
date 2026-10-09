package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 底层长度有余数时removeAndExchangeLastVector必须交换"完整末向量"：
 * 旧实现lastIdx=count-K未向下对齐，把尾部余数字节当向量的一部分读取
 * （wrap[1,2,3,4,99]交换删除idx0得到Vector2(4,99)，应交换的完整末向量
 * 是(3,4)）。交换后余数保持在尾部（对齐removeVector的余数规则），
 * K=2/3/4各余数形态回归。
 */
@Fast
public class TestVectorListExchangeLastWithRemainder {

	@Test
	public void vector2ListExchangesCompleteLastVector() {
		var list = Vector2List.wrap(new float[]{1, 2, 3, 4, 99}, 5); // 两个完整向量+1个余数

		list.removeAndExchangeLastVector(0);
		assertEquals(1, list.vectorSize(), "交换删除一个向量后剩一个完整向量");
		assertEquals(3f, list.getX(0), "idx0应是完整末向量的x（修复前余数99参与交换）");
		assertEquals(4f, list.getY(0), "idx0应是完整末向量的y");
	}

	@Test
	public void vector3ListExchangesCompleteLastVector() {
		var list = Vector3List.wrap(new float[]{1, 2, 3, 4, 5, 6, 98, 99}, 8); // 两个完整向量+2个余数

		list.removeAndExchangeLastVector(0);
		assertEquals(1, list.vectorSize());
		assertEquals(4f, list.getX(0));
		assertEquals(5f, list.getY(0));
		assertEquals(6f, list.getZ(0));
	}

	@Test
	public void vector4ListExchangesCompleteLastVector() {
		var list = Vector4List.wrap(new float[]{1, 2, 3, 4, 5, 6, 7, 8, 97, 98, 99}, 11); // 两个完整向量+3个余数

		list.removeAndExchangeLastVector(0);
		assertEquals(1, list.vectorSize());
		assertEquals(5f, list.getX(0));
		assertEquals(6f, list.getY(0));
		assertEquals(7f, list.getZ(0));
		assertEquals(8f, list.getW(0));
	}

	/** 无余数（对齐）路径语义保持。 */
	@Test
	public void alignedBufferBehaviorUnchanged() {
		var list = Vector2List.wrap(new float[]{1, 2, 3, 4}, 4);
		list.removeAndExchangeLastVector(0);
		assertEquals(1, list.vectorSize());
		assertEquals(3f, list.getX(0));
		assertEquals(4f, list.getY(0));
	}
}
