package ru.mai.exceptions;

import org.slf4j.Logger;

public class IllegalArgumentExceptionWithLog extends IllegalArgumentException {
    public IllegalArgumentExceptionWithLog(String errorString,
                                           Logger log) {
        super(errorString);
        log.error(errorString);
    }


}
