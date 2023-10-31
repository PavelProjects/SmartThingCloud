package ru.pobopo.smartthing.cloud.exception;

public class CommandNotAllowed extends Exception {
    public CommandNotAllowed() {
    }

    public CommandNotAllowed(String message) {
        super(message);
    }
}
