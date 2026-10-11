package com.example.forkworld;

/**
 * The part that actually runs the private simulation. A backend takes a
 * ForkSnapshot, builds a sandboxed server-side world from it, lets the player
 * play in it, and hands back the Recording of what they did.
 *
 * NOT IMPLEMENTED YET - see README.md ("What is still missing").
 */
public interface SandboxBackend {
    void start(ForkSnapshot snapshot);

    boolean isActive();

    /** Rewind the sandbox to the snapshot, keeping the player in it. */
    void rewind();

    /** Leave the sandbox and return the inputs recorded inside it. */
    Recording finish();

    /** Leave the sandbox and throw the recording away. */
    void discard();
}
