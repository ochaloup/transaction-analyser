package io.narayana.txdemo.demos;

import java.util.Arrays;
import java.util.Collection;

public class DemoHelper {

	private DemoHelper() {
		// do not instantiate this class
	}

	/**
	 * Retrieves an immutable collection of all demos (cached, if possible) which
	 * are common for all the services.
	 * 
	 * @return
	 */
	public static Collection<Demo> getCommonDemos() {
		return Arrays.asList(new SuccessTransactionDemo(), new TimeoutTransactionDemo(), new PrepareFailDemo(),
				new ClientDrivenRollbackDemo(), new TimeoutWithRecoveryDemo());
	}
}
