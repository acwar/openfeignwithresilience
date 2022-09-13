package org.aag.testing.feigncircuittest;

public class IgnorableException  extends Exception{

    /**
     * This exception SHOULDN'T trigger the Fallback
     *
     * @param message
     */
    public IgnorableException(String message) {
        super(message);
    }
}
