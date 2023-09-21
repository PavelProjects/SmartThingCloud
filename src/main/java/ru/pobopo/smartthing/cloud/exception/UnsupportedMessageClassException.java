package ru.pobopo.smartthing.cloud.exception;

public class UnsupportedMessageClassException extends Exception {

    public UnsupportedMessageClassException(Class<?> cl) {
        super("Class " + cl.getName() + " can't be sent");
    }
}
