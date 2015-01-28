/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.montecarlo;

import ai.AI;
import ai.RandomBiasedAI;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleEvaluationFunction;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;

/**
 *
 * @author santi
 */
public class ContinuingMC extends AI {
    public static final int DEBUG = 0;
    EvaluationFunction ef = null;
    
    
    public class PlayerActionTableEntry {
        PlayerAction pa;
        float accum_evaluation = 0;
        int visit_count = 0;
    }
    
    
    Random r = new Random();
    AI randomAI = new RandomBiasedAI();
    long max_actions_so_far = 0;
    
    PlayerActionGenerator  moveGenerator = null;
    boolean allMovesGenerated = false;
    List<PlayerActionTableEntry> actions = null;
    GameState gs_to_start_from = null;
    int run = 0;
    
    // statistics:
    public long total_runs = 0;
    public long total_cycles_executed = 0;
    public long total_actions_issued = 0;
        
    int TIME_PER_CYCLE = -1;
    int PLAYOUTS_PER_CYCLE = 200;
    int MAXSIMULATIONTIME = 1024;
    
    public ContinuingMC(int available_time, int playouts_per_cycle, int lookahead, AI policy, EvaluationFunction a_ef) {
        MAXSIMULATIONTIME = lookahead;
        randomAI = policy;
        TIME_PER_CYCLE = available_time;
        PLAYOUTS_PER_CYCLE = playouts_per_cycle;
        ef = a_ef;
    }
    
    
    public void printStats() {
        if (total_cycles_executed>0 && total_actions_issued>0) {
            System.out.println("Average runs per cycle: " + ((double)total_runs)/total_cycles_executed);
            System.out.println("Average runs per action: " + ((double)total_runs)/total_actions_issued);
        }
    }
    
    public void reset() {
        moveGenerator = null;
        actions = null;
        gs_to_start_from = null;
        run = 0;
    }    
    
    public AI clone() {
        return new ContinuingMC(TIME_PER_CYCLE, PLAYOUTS_PER_CYCLE, MAXSIMULATIONTIME, randomAI, ef);
    }
    
    public PlayerAction getAction(int player, GameState gs) throws Exception{
        if (gs.winner()!=-1) return new PlayerAction();
        if (gs.canExecuteAnyAction(player)) {
            // continue or start a search:
            if (moveGenerator==null) {
                startNewSearch(player,gs);
            } else {
                if (!gs.getPhysicalGameState().equivalents(gs_to_start_from.getPhysicalGameState())) {
                    System.err.println("Game state used for search NOT equivalent to the actual one!!!");
                    System.err.println("gs:");
                    System.err.println(gs);
                    System.err.println("gs_to_start_from:");
                    System.err.println(gs_to_start_from);
                }
            }
            search(player);
            PlayerAction best = getBestAction();
            resetSearch();
            return best;
        } else {
            if (moveGenerator!=null) {
                // continue previous search:
                search(player);
            } else {
                // determine who will be the next player:
                GameState temporary_gameState = gs.clone();
                while(temporary_gameState.winner()==-1 && 
                      !temporary_gameState.gameover() &&  
                    !temporary_gameState.canExecuteAnyAction(0) && 
                    !temporary_gameState.canExecuteAnyAction(1)) temporary_gameState.cycle();
                if (temporary_gameState.canExecuteAnyAction(player)) {
                    // start a new search:
                    startNewSearch(player,temporary_gameState);
                    search(player);
                    return new PlayerAction();
                } else {
                    return new PlayerAction();
                }
            }
        }
        
        return new PlayerAction();
    }    
    
    public void startNewSearch(int player, GameState gs) throws Exception {
        if (DEBUG>=2) System.out.println("Starting a new search...");
        if (DEBUG>=2) System.out.println(gs);
        gs_to_start_from = gs;
        moveGenerator = new PlayerActionGenerator(gs,player);
        moveGenerator.randomizeOrder();
        allMovesGenerated = false;
        actions = new LinkedList<PlayerActionTableEntry>();
        run = 0;
    }    
    
    
    public void resetSearch() {
        if (DEBUG>=2) System.out.println("Resetting search...");
        gs_to_start_from = null;
        moveGenerator = null;
        actions = null;
        run = 0;
    }
    

    public void search(int player) throws Exception {
        if (DEBUG>=2) System.out.println("Search...");
        long start = System.currentTimeMillis();
        int nruns = 0;
        long cutOffTime = (TIME_PER_CYCLE>0 ? System.currentTimeMillis() + TIME_PER_CYCLE:0);
        if (TIME_PER_CYCLE<=0) cutOffTime = 0;
        while(true) {
            if (TIME_PER_CYCLE>0 && (System.currentTimeMillis() - start)<TIME_PER_CYCLE) break;
            if (PLAYOUTS_PER_CYCLE>0 && nruns>=PLAYOUTS_PER_CYCLE) break;
            
            if (!allMovesGenerated) {
                PlayerAction pa;
                pa = moveGenerator.getNextAction(cutOffTime);
                if (pa!=null) {
                    PlayerActionTableEntry pate = new PlayerActionTableEntry();
                    pate.pa = pa;
                    actions.add(pate);
                } else {
                    allMovesGenerated = true;
                    max_actions_so_far = Math.max(actions.size(),max_actions_so_far);
                    if (DEBUG>=1) System.out.println("MontCarloAI (complete generation plus random reduction) for player " + player + " chooses between " + actions.size() + " actions [maximum so far " + max_actions_so_far + "]");
                }
            }
            
            monteCarloRun(player, gs_to_start_from);
            nruns++;
        }
        
        total_cycles_executed++;
    }
    

    public void monteCarloRun(int player, GameState gs) throws Exception {
        int idx = run%actions.size();
//        System.out.println(idx);
        PlayerActionTableEntry pate = actions.get(idx);

        GameState gs2 = gs.cloneIssue(pate.pa);
        GameState gs3 = gs2.clone();
        simulate(gs3,gs3.getTime() + MAXSIMULATIONTIME);
        int time = gs3.getTime() - gs2.getTime();

        pate.accum_evaluation += ef.evaluate(player, 1-player, gs3)*Math.pow(0.99,time/10.0);    
        pate.visit_count++;
        run++;
        total_runs++;
    }
    
    
    public PlayerAction getBestAction() {
        // find the best:
        PlayerActionTableEntry best = null;
        for(PlayerActionTableEntry pate:actions) {
            if (best==null || (pate.accum_evaluation/pate.visit_count)>(best.accum_evaluation/best.visit_count)) {
                best = pate;
            }
        }
        if (best==null) best = best;
        
        if (DEBUG>=1) {
            System.out.println("Executed " + run + " runs");
            System.out.println("Selected action: " + best + " visited " + best.visit_count + " with average evaluation " + (best.accum_evaluation/best.visit_count));
        }      
        
        total_actions_issued++;
        
        return best.pa;        
    }
    
    
    public void simulate(GameState gs, int time) throws Exception {
        boolean gameover = false;

        do{
            if (gs.isComplete()) {
                gameover = gs.cycle();
            } else {
                gs.issue(randomAI.getAction(0, gs));
                gs.issue(randomAI.getAction(1, gs));
            }
        }while(!gameover && gs.getTime()<time);   
    }
    
    public String toString() {
        return "ContinuingMC(" + TIME_PER_CYCLE + "," +  PLAYOUTS_PER_CYCLE + "," + MAXSIMULATIONTIME + ")";
    }
    
}
