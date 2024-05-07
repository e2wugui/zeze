package Zeze.Transaction;

import java.util.ArrayList;
import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.Serialize.Serializable;

public interface BeanKey extends Serializable {
	default ArrayList<BVariable.Data> variables() {
		return new ArrayList<>();
	}
}
