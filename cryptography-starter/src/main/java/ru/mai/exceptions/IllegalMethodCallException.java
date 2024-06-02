package ru.mai.exceptions;

public class IllegalMethodCallException extends RuntimeException {
    /**
     * Constructs an {@code IllegalMethodCallException} with no
     * detail message.
     */
    public IllegalMethodCallException() {
        super();
    }

    /**
     * Constructs an {@code IllegalMethodCallException} with the
     * specified detail message.
     *
     * @param s the detail message.
     */
    public IllegalMethodCallException(String s) {
        super(s);
    }

}
