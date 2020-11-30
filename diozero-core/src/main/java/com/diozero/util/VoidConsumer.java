package com.diozero.util;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents an operation that accepts a no-valued argument and returns no
 * result. This is a specialization of {@link Consumer} for no parameters.
 * Unlike most other functional interfaces, {@code VoidConsumer} is expected to
 * operate via side-effects.
 *
 * <p>
 * This is a <a href="package-summary.html">functional interface</a> whose
 * functional method is {@link #accept()}.
 *
 * @see Consumer
 * @since 1.8
 */
@FunctionalInterface
public interface VoidConsumer {
	/**
	 * Performs this operation on the given argument.
	 */
	void accept();

	/**
	 * Returns a composed {@code VoidConsumer} that performs, in sequence, this
	 * operation followed by the {@code after} operation. If performing either
	 * operation throws an exception, it is relayed to the caller of the composed
	 * operation. If performing this operation throws an exception, the
	 * {@code after} operation will not be performed.
	 *
	 * @param after the operation to perform after this operation
	 * @return a composed {@code VoidConsumer} that performs in sequence this
	 *         operation followed by the {@code after} operation
	 * @throws NullPointerException if {@code after} is null
	 */
	default VoidConsumer andThen(VoidConsumer after) {
		Objects.requireNonNull(after);
		return () -> {
			accept();
			after.accept();
		};
	}
}
