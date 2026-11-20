package com.intentmanagement.knowledge;

public record Triple(String head, String relation, String tail) {
    @Override public String toString() { return "(" + head + ", " + relation + ", " + tail + ")"; }
}
