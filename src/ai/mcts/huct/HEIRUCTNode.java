/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.mcts.huct;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.util.Pair;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.PlayerActionGenerator.childInfo;


/**
 *
 * @author santi
 */
public class HEIRUCTNode {
    static Random r = new Random();
    public static float C = 1f;   // this is the constant that regulates exploration vs exploitation, it must be tuned for each domain
//    public static float C = 1;   // this is the constant that regulates exploration vs exploitation, it must be tuned for each domain

    public int type;    // 0 : max, 1 : min, -1: Game-over
    HEIRUCTNode uctParent = null;
    public HEIRHierarchicalNode hParent = null;
    public GameState gs;
    int depth = 0;  // the depth in the tree

    boolean hasMoreActions = true;
    PlayerActionGenerator moveGenerator = null;
    int[] squadIdxs = null;
    public int actionIdx;
    public List<PlayerAction> actions = null;
    public ArrayList<Pair<Integer, HEIRUCTNode>> uctChildren = null;
    //This list is for ordering based on the HEIRUCT values
    public ArrayList<Pair<Integer, HEIRHierarchicalNode>> hChildren = null;
    float evaluation_bound = 0;
    float accum_evaluation = 0;
    int visit_count = 0;


    public HEIRUCTNode(int maxplayer, int minplayer, GameState a_gs, HEIRUCTNode a_parent, HEIRHierarchicalNode h_parent, float bound) throws Exception {
        uctParent = a_parent;
        hParent = h_parent;
        gs = a_gs;
        actionIdx = 0;
        if (uctParent==null) depth = 0;
        else depth = uctParent.depth+1;
        evaluation_bound = bound;

        while(gs.winner()==-1 &&
                !gs.gameover() &&
                !gs.canExecuteAnyAction(maxplayer) &&
                !gs.canExecuteAnyAction(minplayer)) gs.cycle();
        if (gs.winner()!=-1 || gs.gameover()) {
            type = -1;
        } else if (gs.canExecuteAnyAction(maxplayer)) {
            type = 0;
//            actions = gs.getPlayerActions(maxplayer);

            moveGenerator = new PlayerActionGenerator(a_gs, maxplayer);
            moveGenerator.randomizeOrder();
            actions = new ArrayList<>();
            uctChildren = new ArrayList<Pair<Integer, HEIRUCTNode>>();
            hChildren = new ArrayList<Pair<Integer, HEIRHierarchicalNode>>();

            //This is the array of starting indeces for squads. Here we just suppose that there are
            //no squads
            squadIdxs= new int[moveGenerator.getChoiceSizes().length];
            for (int i = 0; i < squadIdxs.length; i++) {
                squadIdxs[i] = i;
            }
        } else if (gs.canExecuteAnyAction(minplayer)) {
            type = 1;
//            actions = gs.getPlayerActions(minplayer);
            moveGenerator = new PlayerActionGenerator(a_gs, minplayer);
            moveGenerator.randomizeOrder();
            actions = new ArrayList<>();
            uctChildren = new ArrayList<Pair<Integer, HEIRUCTNode>>();
            hChildren = new ArrayList<Pair<Integer, HEIRHierarchicalNode>>();

            //This is the array of starting indeces for squads. Here we just suppose that there are
            //no squads
            squadIdxs= new int[moveGenerator.getChoiceSizes().length];
            for (int i = 0; i < squadIdxs.length; i++) {
                squadIdxs[i] = i;
            }
        } else {
            type = -1;
            System.err.println("RTMCTSNode: This should not have happened...");
        }
    }

    public HEIRUCTNode UCTSelectLeaf(int maxplayer, int minplayer, long cutOffTime, int max_depth) throws Exception {

        // Cut the tree policy at a predefined depth
        if (depth>=max_depth) return this;

        // if non visited children, visit:
        if (type == 0)
        	moveGenerator = new PlayerActionGenerator(gs, maxplayer);
        else if (type == 1)
        	moveGenerator = new PlayerActionGenerator(gs, minplayer);
        if (moveGenerator==null) {
//                System.out.println("No more leafs because moveGenerator = null!");
            return this;
        }
        
        if (squadIdxs.length>1) {
        	HEIRHierarchicalNode HNode;
        	childInfo info = moveGenerator.updateAction(0,actionIdx);
        	actionIdx = info.actionIdx;
        	if (info.newChild) {
        		HNode = new HEIRHierarchicalNode(this, evaluation_bound);
                hChildren.add(new Pair<>(actionIdx-1,HNode));
        	}
        	else {
        		Pair<Integer,HEIRHierarchicalNode> best = null;
            	double best_score = 0;
                for (Pair<Integer,HEIRHierarchicalNode> child : this.hChildren) {
                	HEIRHierarchicalNode childNode = child.getValue();
                    double tmp = childNode.childValue(visit_count);
                    if (best==null || tmp>best_score) {
                        best = child;
                        best_score = tmp;
                    }
                }
                moveGenerator.addToLastAction(0, best.getKey());
                HNode = best.getValue();
        	}
        	int i;
            for (i = 1; i < squadIdxs.length-1; i++) {
            	info = moveGenerator.updateAction(squadIdxs[i], HNode.actionIdx);
            	HNode.actionIdx = info.actionIdx;
            	if (info.newChild) {
            		HEIRHierarchicalNode newHNode = new HEIRHierarchicalNode(HNode, evaluation_bound);
                    HNode.hChildren.add(new Pair<>(HNode.actionIdx-1,newHNode));
                    HNode = newHNode;
            	}
            	else {
            		Pair<Integer,HEIRHierarchicalNode> best = null;
                	double best_score = 0;
                    for (Pair<Integer,HEIRHierarchicalNode> child : HNode.hChildren) {
                    	HEIRHierarchicalNode childNode = child.getValue();
                        double tmp = childNode.childValue(HNode.visit_count);
                        if (best==null || tmp>best_score) {
                            best = child;
                            best_score = tmp;
                        }
                    }
                    moveGenerator.addToLastAction(squadIdxs[i], best.getKey());
                    HNode = best.getValue();
            	}
            }
            info = moveGenerator.updateAction(squadIdxs[i], HNode.actionIdx);
            HNode.actionIdx = info.actionIdx;
            GameState gs2 = gs.cloneIssue(moveGenerator.getLastAction());
            HEIRUCTNode node;
            if (info.newChild) {
            	actions.add(moveGenerator.getLastAction());
                node = new HEIRUCTNode(maxplayer, minplayer, gs2.clone(), this, HNode, evaluation_bound);
                HNode.uctChildren.add(new Pair<>(HNode.actionIdx-1,node));
                this.uctChildren.add(new Pair<>(HNode.actionIdx-1,node));
            	return node;
            }
            else {
            	Pair<Integer,HEIRUCTNode> best = null;
            	double best_score = 0;
                for (Pair<Integer,HEIRUCTNode> child : HNode.uctChildren) {
                	HEIRUCTNode childNode = child.getValue();
                    double tmp = childNode.childValue(HNode.visit_count);
                    if (best==null || tmp>best_score) {
                        best = child;
                        best_score = tmp;
                    }
                }
                moveGenerator.addToLastAction(squadIdxs[i], best.getKey());
                node = best.getValue();
            	return node.UCTSelectLeaf(maxplayer, minplayer, cutOffTime, max_depth);
            }
        }
        else {
        	childInfo info = moveGenerator.updateAction(squadIdxs[squadIdxs.length-1], actionIdx);
        	actionIdx = info.actionIdx;
            GameState gs2 = gs.cloneIssue(moveGenerator.getLastAction());
            HEIRUCTNode node;
            if (info.newChild) {
            	actions.add(moveGenerator.getLastAction());
                node = new HEIRUCTNode(maxplayer, minplayer, gs2.clone(), this, null, evaluation_bound);
                this.uctChildren.add(new Pair<>(actionIdx-1,node));
            	return node;
            }
            else {
            	Pair<Integer,HEIRUCTNode> best = null;
            	double best_score = 0;
                for (Pair<Integer,HEIRUCTNode> child : this.uctChildren) {
                	HEIRUCTNode childNode = child.getValue();
                    double tmp = childNode.childValue(visit_count);
                    if (best==null || tmp>best_score) {
                        best = child;
                        best_score = tmp;
                    }
                }
                moveGenerator.addToLastAction(squadIdxs[squadIdxs.length-1], best.getKey());
                node = best.getValue();
            	return node.UCTSelectLeaf(maxplayer, minplayer, cutOffTime, max_depth);
            }
        }
    }

    public double childValue(double parent_visit_count) {
        double exploitation = ((double)accum_evaluation) / visit_count;
        double exploration = Math.sqrt(Math.log(parent_visit_count)/visit_count);
        //We use the parent's type
        if (uctParent.type==0) {
            // max node:
            exploitation = (uctParent.evaluation_bound + exploitation)/(2*uctParent.evaluation_bound);
        } else {
            exploitation = (uctParent.evaluation_bound - exploitation)/(2*uctParent.evaluation_bound);
        }
//            System.out.println(exploitation + " + " + exploration);

        double tmp = C*exploitation + exploration;
        return tmp;
    }

    public void showNode(int depth, int maxdepth) {
        int mostVisitedIdx = -1;
        HEIRUCTNode mostVisited = null;
        for(int i = 0;i<uctChildren.size();i++) {
            HEIRUCTNode child = uctChildren.get(i).getValue();
            for(int j = 0;j<depth;j++) System.out.print("    ");
            System.out.println("child explored " + child.visit_count + " Avg evaluation: " + (child.accum_evaluation/((double)child.visit_count)) + " : " + actions.get(i));
            if (depth<maxdepth) child.showNode(depth+1,maxdepth);
        }
    }
}