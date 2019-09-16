package io.narayana.txdemo.demos;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DemoHelper {

	private DemoHelper() {
		// do not instantiate this class 
	}

	/**
	 * Retrieves an collection of all demos which
	 * are common for all the services.
	 * 
	 * @return
	 */
	public static Collection<Demo> getCommonDemos() {
		List<Demo> demos = Arrays.asList(new SuccessTransactionDemo(), new TimeoutTransactionDemo(), new PrepareFailDemo(),
				new ClientDrivenRollbackDemo(), new TimeoutWithRecoveryDemo());
		int noDemos = demos.size();
		Set<Demo> demosUnique = new HashSet<>(demos);
		if(noDemos != demosUnique.size()) {
			throw new RuntimeException("There is at least one pair of demos which has got the same id. Fix it.");
		}
		return demosUnique;
	}
}
