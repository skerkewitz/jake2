/*
 * Timer.java
 * Copyright (C) 2005
 */
package jake2.sys;

import jake2.client.Context;
import jake2.qcommon.Command;


public abstract class Timer {

	abstract public long currentTimeMillis();
	static Timer t;
	
	static {
		try {
			t = new NanoTimer();
		} catch (Throwable e) {
			try {
				t = new HighPrecisionTimer();
			} catch (Throwable e1) {
				t = new StandardTimer();
			}
		}
		Command.Println("using " + t.getClass().getName());
	}
	
	public static int Milliseconds() {
		return Context.curtime = (int)(t.currentTimeMillis());
	}
}
