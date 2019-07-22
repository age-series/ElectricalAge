package mods.eln.fsm;

import mods.eln.debug.DP;
import mods.eln.debug.DPType;
import mods.eln.sim.core.IProcess;

public class StateMachine implements IProcess {

    public void setInitialState(State initialState) {
        this.initialState = initialState;
    }

    public void reset() {
        state = initialState;
        if (state != null) state.enter();
    }

    protected void stop() {
        state = null;
    }

    public void setDebug(boolean enabled) {
        debug = enabled;
    }

    @Override
    public void process(double time) {
        if (state == null) {
            if (debug) DP.println(DPType.FSM, "INVALID STATE!!");
            return;
        }

        State nextState = state.state(time);

        if (nextState != null && nextState != state) {
            if (debug)
                System.out.print(getClass().toString() + ": " + state.getClass().toString() + " -> " + nextState.getClass().toString());
            state.leave();
            state = nextState;
            state.enter();
        }
    }

    private State initialState = null;
    private State state = null;
    private boolean debug = false;
}
