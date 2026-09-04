package demo.web;

/** A record returned from a controller becomes JSON, with no code to do it. */
public record Greeting(String message, int count) {}
