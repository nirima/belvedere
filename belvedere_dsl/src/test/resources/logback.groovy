package com.nirima
// LOGBACK LOGGING CONFIGURATION

import ch.qos.logback.classic.encoder.PatternLayoutEncoder

import static ch.qos.logback.classic.Level.DEBUG


def consolePattern = "TEST: %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

scan("30 seconds")



appender("STDERR", ConsoleAppender) {
    target = System.err
    encoder(PatternLayoutEncoder) {
        pattern = consolePattern;
    }
}



root(DEBUG, ["STDERR",])
