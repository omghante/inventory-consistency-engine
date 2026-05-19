package com.cascade.exceptions;

/**
 * CascadeException — Base exception for all CASCADE errors.
 */
public class CascadeException extends RuntimeException {
    public CascadeException(String message) { super(message); }
    public CascadeException(String message, Throwable cause) { super(message, cause); }
}
