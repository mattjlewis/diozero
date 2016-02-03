package com.diozero;

import java.util.function.Consumer;

public class LambdaTest {

	public static void main(String[] args) {
		Consumer<Void> consumer = (Void v) -> { System.out.println("Hello"); };
		
		consumer.accept(null);
		
		Command c = () -> { System.out.println("Hello"); };
		c.action();
	}
	
	interface Command extends Consumer<Void> {
		@Override
		default void accept(Void v) {
			action();
		}
		
		void action();
	}
}
