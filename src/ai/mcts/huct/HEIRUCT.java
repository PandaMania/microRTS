/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.mcts.huct;

import ai.core.AI;
import ai.RandomBiasedAI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ai.mcts.uct.MAST;
import ai.mcts.uct.MASTStrategy5;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import ai.core.InterruptibleAI;

/**
 *
 * @author santi
 */
public class HEIRUCT extends AIWithComputationBudget implements InterruptibleAI {
    public static int DEBUG = 0;
    EvaluationFunction ef = null;

    Random r = new Random();
    AI randomAI = new RandomBiasedAI();
    long max_actions_so_far = 0;

    GameState gs_to_start_from = null;
    public HEIRUCTNode tree = null;
    //public MASTStrategy5 mastStrategy5= new MASTStrategy5();
    public MAST mast;

    // statistics:
    public long total_runs = 0;
    public long total_cycles_executed = 0;
    public long total_actions_issued = 0;

    public int savedTrees=0;
    public int deadTrees=0;
    long total_runs_this_move = 0;

    int MAXSIMULATIONTIME = 1024;
    int MAX_TREE_DEPTH = 12;
    boolean SAVING;
    boolean PLAYOUT;
    boolean ORDERED;


    int playerForThisComputation;


    public HEIRUCT(UnitTypeTable utt) {
        this(100,-1,100,10,
                new RandomBiasedAI(),
                new SimpleSqrtEvaluationFunction3());
    }


    public HEIRUCT(int available_time, int max_playouts, int lookahead, int max_depth, AI policy, EvaluationFunction a_ef) {
        super(available_time, max_playouts);
        MAXSIMULATIONTIME = lookahead;
        randomAI = policy;
        MAX_TREE_DEPTH = max_depth;
        ef = a_ef;
    }

    public HEIRUCT(int available_time, int max_playouts, int lookahead, int max_depth, AI policy, EvaluationFunction a_ef, boolean ordered, boolean saving, boolean playout) {
        super(available_time, max_playouts);
        MAXSIMULATIONTIME = lookahead;
        MAX_TREE_DEPTH = max_depth;
        ef = a_ef;
        PLAYOUT=playout;
        SAVING=saving;
        ORDERED=ordered;
        randomAI = policy;

    }
    public HEIRUCT(int available_time, int max_playouts, int lookahead, int max_depth, AI policy, EvaluationFunction a_ef, boolean ordered, boolean saving, boolean playout,MAST mast) {
        super(available_time, max_playouts);
        MAXSIMULATIONTIME = lookahead;
        MAX_TREE_DEPTH = max_depth;
        ef = a_ef;
        PLAYOUT=playout;
        SAVING=saving;
        ORDERED=ordered;
        randomAI = policy;
        if(PLAYOUT)
            this.mast = mast;

    }


    public String statisticsString() {
        return "Average runs per cycle: " + ((double)total_runs)/total_cycles_executed +
                ", Average runs per action: " + ((double)total_runs)/total_actions_issued;

    }

    public void printStats() {
        if (total_cycles_executed>0 && total_actions_issued>0) {
            System.out.println("Average runs per cycle: " + ((double)total_runs)/total_cycles_executed);
            System.out.println("Average runs per action: " + ((double)total_runs)/total_actions_issued);
        }
    }


    public void reset() {
        gs_to_start_from = null;
        tree = null;
        total_runs_this_move = 0;
    }


    public AI clone() {
        return new HEIRUCT(TIME_BUDGET, ITERATIONS_BUDGET, MAXSIMULATIONTIME, MAX_TREE_DEPTH, randomAI, ef);
    }


    public PlayerAction getAction(int player, GameState gs) throws Exception
    {


        if (gs.canExecuteAnyAction(player)) {
           // if(tree==null){
                startNewComputation(player,gs.clone());
                computeDuringOneGameFrame();
                return getBestActionSoFar();
           /*} else{
                startNewComputation(gs.clone(),tree);
                computeDuringOneGameFrame();
                return getBestActionSoFar();
                }

*/        }else {
            return new PlayerAction();
        }

    }


    public void startNewComputation(int a_player, GameState gs) throws Exception {
        //System.out.println("starting computation");

        float evaluation_bound = ef.upperBound(gs);
        playerForThisComputation = a_player;
     //  HEIRUCTNode newRoot= WWF(tree, gs);
        //if(newRoot==null){
            tree = new HEIRUCTNode(playerForThisComputation, 1 - playerForThisComputation, gs.clone(), null, null, evaluation_bound);
            gs_to_start_from = gs;
            total_runs_this_move = 0;
        if(PLAYOUT)
            mast.myPlayer = playerForThisComputation;

        /*}else
        tree =  copyTree(newRoot, gs);
            gs_to_start_from = gs;
            total_runs_this_move = tree.visit_count;

//        System.out.println(evaluation_bound);*/
    }


    public void resetSearch() {
        if (DEBUG>=2) System.out.println("Resetting search...");
        tree = null;
        gs_to_start_from = null;
        total_runs_this_move = 0;
    }


    public void computeDuringOneGameFrame() throws Exception {
        if (DEBUG>=2) System.out.println("Search...");
        long start = System.currentTimeMillis();
        int nPlayouts = 0;
        long cutOffTime = start + TIME_BUDGET;
        if (TIME_BUDGET<=0) cutOffTime = 0;

//        System.out.println(start + " + " + available_time + " = " + cutOffTime);

        while(true) {
            if (cutOffTime>0 && System.currentTimeMillis() > cutOffTime) break;
            if (ITERATIONS_BUDGET>0 && nPlayouts>ITERATIONS_BUDGET) break;
            monteCarloRun(playerForThisComputation, cutOffTime);
            nPlayouts++;
        }

        total_cycles_executed++;
    }


    public double monteCarloRun(int player, long cutOffTime) throws Exception {
        HEIRUCTNode leaf = tree.UCTSelectLeaf(player, 1-player, cutOffTime, MAX_TREE_DEPTH);

        if (leaf!=null) {
            GameState gs2 = leaf.gs.clone();
           if(PLAYOUT) {
               mast.simulate(gs2, gs2.getTime() + MAXSIMULATIONTIME);
           }else
               simulate(gs2,gs2.getTime() + MAXSIMULATIONTIME);
            int time = gs2.getTime() - gs_to_start_from.getTime();
            double evaluation = ef.evaluate(player, 1-player, gs2)*Math.pow(0.99,time/10.0);

//                System.out.println(evaluation_bound + " -> " + evaluation + " -> " + (evaluation+evaluation_bound)/(evaluation_bound*2));

            while(leaf!=null) {
                leaf.accum_evaluation += evaluation;
                leaf.visit_count++;
                HEIRHierarchicalNode hNode = leaf.hParent;
                while(hNode != null){
                    hNode.accum_evaluation += evaluation;
                    hNode.visit_count++;
                    hNode=hNode.hParent;
                }
                leaf = leaf.uctParent;
            }
            total_runs++;
            total_runs_this_move++;
            return evaluation;
        } else {
            // no actions to choose from 🙂
            System.err.println(this.getClass().getSimpleName() + ": claims there are no more leafs to explore...");
            return 0;
        }
    }


    public PlayerAction getBestActionSoFar() {
        total_actions_issued++;

        if (tree.uctChildren==null) {
            if (DEBUG>=1) System.out.println(this.getClass().getSimpleName() + " no children selected. Returning an empty asction");
            return new PlayerAction();
        }

        int mostVisitedIdx = -1;
        HEIRUCTNode mostVisited = null;
        for(int i = 0;i<tree.uctChildren.size();i++) {
            HEIRUCTNode child = tree.uctChildren.get(i);
            if (mostVisited == null || child.visit_count>mostVisited.visit_count ||
                    (child.visit_count==mostVisited.visit_count &&
                            child.accum_evaluation > mostVisited.accum_evaluation)) {
                mostVisited = child;
                mostVisitedIdx = i;
            }
        }

//        tree.showNode(0,0);
        if (DEBUG>=2) tree.showNode(0,1);
        if (DEBUG>=1) System.out.println(this.getClass().getSimpleName() + " performed " + total_runs_this_move + " playouts.");
        if (DEBUG>=1) System.out.println(this.getClass().getSimpleName() + " selected children " + tree.actions.get(mostVisitedIdx) + " explored " + mostVisited.visit_count + " Avg evaluation: " + (mostVisited.accum_evaluation/((double)mostVisited.visit_count)));

//        printStats();

        if (mostVisitedIdx==-1) return new PlayerAction();

        return tree.actions.get(mostVisitedIdx);
    }


    // gets the best action, evaluates it for 'N' times using a simulation, and returns the average obtained value:
    public float getBestActionEvaluation(GameState gs, int player, int N) throws Exception {
        PlayerAction pa = getBestActionSoFar();

        if (pa==null) return 0;

        float accum = 0;
        for(int i = 0;i<N;i++) {
            GameState gs2 = gs.cloneIssue(pa);
            GameState gs3 = gs2.clone();
            if(PLAYOUT) {
                mast.simulate(gs3, gs3.getTime() + MAXSIMULATIONTIME);
            }else
                simulate(gs3,gs3.getTime() + MAXSIMULATIONTIME);

            int time = gs3.getTime() - gs2.getTime();
            // Discount factor:
            accum += (float)(ef.evaluate(player, 1-player, gs3)*Math.pow(0.99,time/10.0));
        }

        return accum/N;
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


    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + TIME_BUDGET + ", " + ITERATIONS_BUDGET + ", " + MAXSIMULATIONTIME + ", " + MAX_TREE_DEPTH + ", " + randomAI + ", " + ef + ")";
    }


    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();

        parameters.add(new ParameterSpecification("TimeBudget",int.class,100));
        parameters.add(new ParameterSpecification("IterationsBudget",int.class,-1));
        parameters.add(new ParameterSpecification("PlayoutLookahead",int.class,100));
        parameters.add(new ParameterSpecification("MaxTreeDepth",int.class,10));

        parameters.add(new ParameterSpecification("DefaultPolicy",AI.class, randomAI));
        parameters.add(new ParameterSpecification("EvaluationFunction", EvaluationFunction.class, new SimpleSqrtEvaluationFunction3()));

        return parameters;
    }


    public int getPlayoutLookahead() {
        return MAXSIMULATIONTIME;
    }


    public void setPlayoutLookahead(int a_pola) {
        MAXSIMULATIONTIME = a_pola;
    }


    public int getMaxTreeDepth() {
        return MAX_TREE_DEPTH;
    }


    public void setMaxTreeDepth(int a_mtd) {
        MAX_TREE_DEPTH = a_mtd;
    }


    public AI getDefaultPolicy() {
        return randomAI;
    }


    public void setDefaultPolicy(AI a_dp) {
        randomAI = a_dp;
    }


    public EvaluationFunction getEvaluationFunction() {
        return ef;
    }


    public void setEvaluationFunction(EvaluationFunction a_ef) {
        ef = a_ef;
    }




    public HEIRUCTNode WWF(HEIRUCTNode oldRoot, GameState current){
        HEIRUCTNode toReturn;
       // System.out.println("------------------------------------------------------");
        //System.out.println("current" + current.getPhysicalGameState());

        if(oldRoot!=null) {
            for (int j = 0; j < oldRoot.uctChildren.get(0).uctChildren.size(); j++) {
               // System.out.println(oldRoot.uctChildren.get(0).uctChildren.get(j).gs.getPhysicalGameState());
                if (compare2GameStates(oldRoot.uctChildren.get(0).uctChildren.get(j).gs,current)) {
                    toReturn = copyTree(oldRoot.uctChildren.get(0).uctChildren.get(j), current);
                    savedTrees++;
                     System.out.println("we saved " + (double) savedTrees/(savedTrees+deadTrees) + "% trees");

                    return  toReturn;
                }
            }
        }
        deadTrees++;

        System.out.println("we saved " +(double) savedTrees/(savedTrees+deadTrees) + "% trees");
        return null;


    }

    public HEIRUCTNode copyTree( HEIRUCTNode newRoot, GameState current){
        newRoot.uctParent=null;
        newRoot.hParent=null;
       // newRoot.gs=current.clone();
        return newRoot;
    }
    public void startNewComputation( GameState gs, HEIRUCTNode oldRoot) throws Exception {
       HEIRUCTNode newRoot= WWF(oldRoot, gs);
        if(newRoot==null){
            startNewComputation(0,gs);
        }else
        tree =  copyTree(newRoot, gs);
        gs_to_start_from = gs;
        total_runs_this_move = 0; // QUESTIONMARK
//        System.out.println(evaluation_bound);
    }




    public boolean compare2GameStates(GameState one, GameState two){
      boolean same=false;

       // System.out.println("number of children = " + two.getUnits().size());
        for(int i=0; i< one.getUnits().size();i++){
            same=false;

            for(int j=0; j<two.getUnits().size();j++){
                if(one.getUnits().get(i).getX()==two.getUnits().get(j).getX()){
                    if(one.getUnits().get(i).getY()==two.getUnits().get(j).getY()){
                        if(one.getUnits().get(i).getPlayer()==two.getUnits().get(j).getPlayer()){
                            if(one.getUnits().get(i).getResources()==two.getUnits().get(j).getResources()){
                                if(one.getUnits().get(i).getType()==two.getUnits().get(j).getType()){
                                    if(one.getUnits().get(i).getHitPoints()==two.getUnits().get(j).getHitPoints()){
                                        if(one.getUnits().get(i).getMoveTime()==two.getUnits().get(j).getMoveTime()){
                                            if(one.getUnits().get(i).getAttackTime()==two.getUnits().get(j).getAttackTime()){
                                                if(one.getUnits().get(i).getHarvestAmount()==two.getUnits().get(j).getHarvestAmount()){
                                                    if(one.getUnits().get(i).getCost()==two.getUnits().get(j).getCost()){
                                                        if(one.getTime()==two.getTime()){



                                                                same=true;


                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
            if(!same){
               // System.out.println(1);
                return false;
            }
          }
          //System.out.println("fuck");
    return true;
    }

}