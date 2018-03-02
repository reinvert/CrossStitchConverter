package com.stitch.converter;

public class LogPrinter {
	
	private static Logger logger = new Logger() {
		@Override
		public void print(final String str) {
			System.out.println(str);
		}

		@Override
		public void print(final Throwable throwable) {
			throwable.printStackTrace();
		}
	};
	
	private LogPrinter() {
		throw new AssertionError();
	}
	
	public static void setPrinter(Logger logger) {
		LogPrinter.logger = logger;
	}
	
	public static void print(String str) {
		logger.print(str);
	}
	
	public static void print(Throwable throwable) {
		logger.print(throwable);
	}
	
	public interface Logger {
		public void print(final String str);
		public void print(final Throwable throwable);
	}
}
