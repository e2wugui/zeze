package Zeze.Arch.Gen;

import java.lang.reflect.Field;
import java.util.List;

import Zeze.Arch.Online;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;
import Zeze.Util.StringBuilderCs;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-82回归：Gen对Data形参encode不写typeId、decode却按typeId经
 * beanFactory.createDataFromSpecialTypeId反建——非默认Data时decode把编码体首字节
 * （非零tag）当typeId消费，查表miss抛误导性"unknown data typeId"异常；全默认Data
 * 时typeId读0命中EmptyBean.Data，吞掉后续参数字节，本参数及其后所有参数静默乱解。
 * 修复：encode侧对Data（与Bean同款）先WriteLong(varName.typeId())再写编码体，
 * 与decode侧配对。
 * <p>
 * 双层断言：①直接调genEncode/genDecode（同包）断言生成源码对称——Data元素
 * encode必须先写typeId再encode，decode为createDataFromSpecialTypeId(ReadLong())；
 * Bean与已知类型作护栏。该对函数同时服务形参与结果字段四个生成点（同一代码路径）。
 * ②用真实Data类按修复后的线格式跑round-trip（typeId前缀+编码体 → 工厂反建+decode），
 * 覆盖非默认与全默认（EmptyBean typeId=0豁免路径）两种形态。
 */
@Fast
public class TestFnd882GenDataParamTypeidSymmetry {

	/** Data与Bean静态类型形参/结果字段的载体：走Serializable分支的两条路径。 */
	@SuppressWarnings("unused")
	public static class Fixture {
		public Bean bean; // 既有对称路径护栏
		public Data row; // 缺陷路径
		public int plain; // 已知序列化器路径护栏
	}

	/** 生成源码对称性：Data的encode必须补写typeId（修复前只有encode行）。 */
	@Test
	public void testGeneratedSourceSymmetric() throws Exception {
		List<Field> fields = List.of(Fixture.class.getField("bean"), Fixture.class.getField("row"),
				Fixture.class.getField("plain"));

		var sbEnc = new StringBuilderCs();
		Gen.instance.genEncode(sbEnc, "", "_b_", "_m_", "", fields, null);
		var enc = sbEnc.toString();
		var rowTypeid = enc.indexOf("_b_.WriteLong(row.typeId());");
		var rowEncode = enc.indexOf("row.encode(_b_);");
		Assertions.assertTrue(rowTypeid >= 0, "Data形参encode必须先写typeId（FND8-82：decode按typeId反建）");
		Assertions.assertTrue(rowEncode > rowTypeid, "typeId必须写在编码体之前");
		Assertions.assertTrue(enc.contains("_b_.WriteLong(bean.typeId());"), "Bean路径对称性不得回归");
		Assertions.assertTrue(enc.contains("_b_.WriteLong(plain);"), "已知类型护栏不得回归");

		var sbDec = new StringBuilderCs();
		Gen.instance.genDecode(sbDec, "", "_b_", "_m_", "", fields);
		var dec = sbDec.toString();
		Assertions.assertTrue(dec.contains("row = beanFactory.createDataFromSpecialTypeId(_b_.ReadLong());"),
				"decode侧Data按typeId反建（既有契约，encode侧须与之配对）");
		Assertions.assertTrue(dec.contains("row.decode(_b_);"));
		Assertions.assertTrue(dec.contains("bean = beanFactory.createBeanFromSpecialTypeId(_b_.ReadLong());"),
				"Bean路径decode不得回归");
	}

	/** 修复后线格式round-trip：typeId前缀+编码体，decode工厂反建，非默认与全默认Data均闭合。 */
	@Test
	public void testWireRoundTrip() throws Exception {
		// 非默认Data：修复前该形态decode首字节被当typeId→查表miss→误导性异常
		var data = new BValue.Data();
		data.setInt_1(882);
		data.setLong2(882882L);
		data.setString3("a7fnd882");
		assertRoundTrip(data);

		// 全默认Data：encode体恰为终止符0x00；修复前typeId读0命中EmptyBean.Data吞掉后续字节
		assertRoundTrip(new BValue.Data());
	}

	private static void assertRoundTrip(Data data) throws Exception {
		// 按修复后生成的encode序列：WriteLong(typeId()) + encode(bb)
		var bb = ByteBuffer.Allocate();
		bb.WriteLong(data.typeId());
		data.encode(bb);

		// 按修复后生成的decode序列：createDataFromSpecialTypeId(ReadLong()) + decode(bb)
		var reader = ByteBuffer.Wrap(bb.Bytes, 0, bb.WriteIndex);
		var decoded = Online.beanFactory.createDataFromSpecialTypeId(reader.ReadLong());
		decoded.decode(reader);
		Assertions.assertEquals(reader.WriteIndex, reader.ReadIndex, "解码后读指针应恰好在编码体末尾");

		var bb2 = ByteBuffer.Allocate();
		bb2.WriteLong(decoded.typeId());
		decoded.encode(bb2);
		Assertions.assertArrayEquals(java.util.Arrays.copyOf(bb.Bytes, bb.WriteIndex),
				java.util.Arrays.copyOf(bb2.Bytes, bb2.WriteIndex), "round-trip字节必须一致");
		Assertions.assertEquals(BValue.Data.class, decoded.getClass(), "必须反建出真实Data类型");
	}
}
