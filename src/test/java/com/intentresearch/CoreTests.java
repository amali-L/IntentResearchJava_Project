package com.intentmanagement.app;

/** Dependency-free smoke-test entry point. Run the richer suite with Main --self-test. */
public final class CoreTests {
    private CoreTests() {}
    public static void main(String[] args) {
        SelfTest.run();
    }
}
