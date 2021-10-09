package Zeze.Transaction;

import Zeze.Serialize.*;
import Zeze.*;
import java.util.*;

public interface DynamicBeanReadOnly {
	public long getTypeId();
	public Bean getBean();
}