package Zeze.Transaction;

import java.util.ArrayList;
import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;

public interface BeanKey extends Serializable {
	default @NotNull ArrayList<BVariable.Data> variables() {
		return new ArrayList<>();
	}
}
