package com.example.forkworld;

/**
 * Everything the player's client did on one game tick. Applied in the same
 * phase of the tick it was recorded in.
 *
 * x/y/z is the player position at the END of that tick; the replayer uses it
 * to detect when the real world has diverged from the simulation.
 */
public record InputFrame(
        float forward, float strafe,
        boolean jump, boolean sneak, boolean sprint,
        float yaw, float pitch,
        int slot,
        boolean attack, boolean use,
        double x, double y, double z) {
}
