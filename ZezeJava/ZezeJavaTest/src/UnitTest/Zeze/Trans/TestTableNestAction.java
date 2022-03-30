package UnitTest.Zeze.Trans;

import java.util.Objects;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestTableNestAction {
	
	@Before
	public final void testInit() throws Throwable {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		demo.App.getInstance().Stop();
	}


	@Test
	public final void testNestProcedure() throws Throwable{

		OutObject<Integer> value1 = new OutObject<>(0);
		OutObject<Integer> value2 = new OutObject<>(0);
		demo.App.getInstance().Zeze.NewProcedure( () -> {

			Transaction.getCurrent().RunWhileCommit(() -> {
				value1.Value++; //1
				System.out.println("value1:" + value1.Value);
			});

			demo.App.getInstance().Zeze.NewProcedure(() -> {

				Transaction.getCurrent().RunWhileCommit(() -> {
					value1.Value++;
				});

				Transaction.getCurrent().RunWhileRollback(() -> {
					assert value1.Value == value2.Value + 1;
					value2.Value++; // 1
					System.out.println(value1.Value);
					System.out.println(value2.Value);
				});
				return Procedure.Exception;
			}, "nest procedure1").Call();

			demo.App.getInstance().Zeze.NewProcedure(() -> {

				Transaction.getCurrent().RunWhileCommit(() -> {
					assert Objects.equals(value1.Value, value2.Value);
					value1.Value++; // 2
				});


				demo.App.getInstance().Zeze.NewProcedure(() -> {

					Transaction.getCurrent().RunWhileCommit(() -> {
						value1.Value++;
					});

					Transaction.getCurrent().RunWhileRollback(() -> {
						assert value1.Value == value2.Value + 1;
						value2.Value++; // 2
						System.out.println(value1.Value);
						System.out.println(value2.Value);
					});
					return Procedure.Exception;
				}, "nest procedure1").Call();

				return Procedure.Success;
			}, "nest procedure2").Call();


			Transaction.getCurrent().RunWhileCommit(() -> {
				assert value1.Value == value2.Value;
				value1.Value++; // 3
			});
			return Procedure.Success;
		}, "out").Call();

		assert value1.Value == 3;
		assert value2.Value == 2;

	}
}