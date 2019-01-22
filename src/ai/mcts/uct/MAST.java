package ai.mcts.uct;

import ai.core.AI;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PlayerAction;

import java.util.List;

public class MAST extends AI {

    public int myPlayer;
    @Override
    public void reset() {

    }
    public void simulate(GameState gs, int time) {

    }

    public void simulate(List<GameState> lastGss ,GameState gs, int time) {

    }
    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        return null;
    }

    @Override
    public AI clone() {
        return null;
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return null;
    }
}
