package demo.Module1;

import Zeze.Collections.BeanFactory;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;
import org.jetbrains.annotations.NotNull;

public class ModuleModule1 extends AbstractModule {
	public static final BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Serializable s) {
		return s.typeId();
	}

	public static @NotNull Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static @NotNull Data createDataFromSpecialTypeId(long typeId) {
		return beanFactory.createDataFromSpecialTypeId(typeId);
	}

	public void Start(demo.App app) {
	}

	public void Stop(demo.App app) {
	}

	public tMemorySize tMemorySize() {
		return _tMemorySize;
	}

	@Override
	protected long ProcessProtocol3(Protocol3 p) throws Exception {
		p.Send(p.getSender());
		return 0;
	}

	@Override
	protected long ProcessProtocol4(Protocol4 p) throws Exception {
		return Zeze.Transaction.Procedure.NotImplement;
	}

	@Override
	protected long ProcessRpc1Request(Rpc1 r) throws Exception {
		System.out.println("ProcessRpc1Request");
		new Rpc2().Send(r.getSender(), (p) -> {
			System.out.println("ProcessRpc2Response");
			return 0;
		});
		r.SendResult();
		return 0;
	}

	public tflush getTflush() {
		return _tflush;
	}

	public Table1 getTable1() {
		return _Table1;
	}

	public Table2 getTable2() {
		return _Table2;
	}

	public Table3 getTable3() {
		return _Table3;
	}

	public tWalkPage tWalkPage() {
		return _tWalkPage;
	}

	public TableImportant getTableImportant() {
		return _TableImportant;
	}

	@Override
	public long ProcessProtocolNoProcedure(ProtocolNoProcedure p) {
		return Zeze.Transaction.Procedure.NotImplement;
	}

	@Override
	protected long ProcessProtocolUseData(demo.Module1.ProtocolUseData p) {
		return Zeze.Transaction.Procedure.NotImplement;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleModule1(demo.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
