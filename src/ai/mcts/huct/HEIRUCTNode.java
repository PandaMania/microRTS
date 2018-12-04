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
public class HEIRUCTNode extends HEIRNode{
    public GameState gs;
    int depth = 0;  // the depth in the tree

    boolean hasMoreActions = true;
    PlayerActionGenerator moveGenerator = null;
    int[] squadIdxs = null;
    public List<PlayerAction> actions = null;
    int unitIdx = 0;


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
            uctChildren = new ArrayList<HEIRUCTNode>();
            hChildren = new ArrayList<HEIRHierarchicalNode>();

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
            uctChildren = new ArrayList<HEIRUCTNode>();
            hChildren = new ArrayList<HEIRHierarchicalNode>();

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
        
        unitIdx = 0;
        //We will use squadIdx later but it behaves now as there are no squads
        //We check whether we have HEIRHierarchicalNodes children
        if (squadIdxs.length>1) {
        	HEIRHierarchicalNode HNode;
        	//this function tries the actions one after an other for the unit indexed by unitIdx
        	//The actionIdx is the starting index and every time when the action is not valid
        	//we increase it and try again. At the end we returns the valid action index and
        	//a bool variable indicating whether we need to create a new child
        	childInfo info = moveGenerator.updateAction(unitIdx,actionIdx);
        	//We update the index of the action with the next action that we will try.
        	//This is faster then check the action index of the last child in the list
        	actionIdx = info.actionIdx;
        	//If we found an action that leads to a valid state
        	if (info.newChild) {
        		HNode = new HEIRHierarchicalNode(this, evaluation_bound);
                hChildren.add(HNode);
        	}
        	else {
        		//Pick the child with the best UCT value
        		HNode = (HEIRHierarchicalNode) getBestChild(this, true);
        	}
        	//We iterate through the tree levels containing HEIRHierarchicalNodes
            for (unitIdx = 1; unitIdx < squadIdxs.length-1; unitIdx++) {
            	info = moveGenerator.updateAction(unitIdx, HNode.actionIdx);
            	HNode.actionIdx = info.actionIdx;
            	if (info.newChild) {
            		HEIRHierarchicalNode newHNode = new HEIRHierarchicalNode(HNode, evaluation_bound);
                    HNode.hChildren.add(newHNode);
                    HNode = newHNode;
            	}
            	else {
            		HNode = (HEIRHierarchicalNode) getBestChild(HNode, true);
            	}
            }
            info = moveGenerator.updateAction(unitIdx, HNode.actionIdx);
            HNode.actionIdx = info.actionIdx;
            GameState gs2 = gs.cloneIssue(moveGenerator.getLastAction());
            HEIRUCTNode node;
            if (info.newChild) {
            	actions.add(moveGenerator.getLastAction());
                node = new HEIRUCTNode(maxplayer, minplayer, gs2.clone(), this, HNode, evaluation_bound);
                HNode.uctChildren.add(node);
                this.uctChildren.add(node);
            	return node;
            }
            else {
            	node = (HEIRUCTNode) getBestChild(HNode, false);
            	return node.UCTSelectLeaf(maxplayer, minplayer, cutOffTime, max_depth);
            }
        }
        else {
        	childInfo info = moveGenerator.updateAction(0, actionIdx);
        	actionIdx = info.actionIdx;
            GameState gs2 = gs.cloneIssue(moveGenerator.getLastAction());
            HEIRUCTNode node;
            if (info.newChild) {
            	actions.add(moveGenerator.getLastAction());
                node = new HEIRUCTNode(maxplayer, minplayer, gs2.clone(), this, null, evaluation_bound);
                this.uctChildren.add(node);
            	return node;
            }
            else {
            	node = (HEIRUCTNode) getBestChild(this, false);
            	return node.UCTSelectLeaf(maxplayer, minplayer, cutOffTime, max_depth);
            }
        }
    }
    
    public HEIRNode getBestChild(HEIRNode Node, boolean ishChild) {
    	double best_score = 0;
    	//If we iterate through hChildrens
    	if (ishChild) {
    		HEIRHierarchicalNode best = null;
    		//find the child with the highest UCT value
    		for (HEIRHierarchicalNode child : Node.hChildren) {
                double tmp = child.childValue(Node.visit_count);
                if (best==null || tmp>best_score) {
                    best = child;
                    best_score = tmp;
                }
            }
    		//Add the unit and its action to moveGenerator.lastAction.
    		//That variable stores the combined actions that will be played
    		moveGenerator.addToLastAction(unitIdx, Node.actionIdx);
            return best;
    	}
    	//If we iterate through uctChildrens
        else {
        	HEIRUCTNode best = null;
    		for (HEIRUCTNode child : Node.uctChildren) {
                double tmp = child.childValue(Node.visit_count);
                if (best==null || tmp>best_score) {
                    best = child;
                    best_score = tmp;
                }
            }
    		moveGenerator.addToLastAction(unitIdx, Node.actionIdx);
            return best;
    	}
    }
    
    public double childValue(double parent_visit_count) {
        double exploitation = ((double)accum_evaluation) / visit_count;
        double exploration = Math.sqrt(Math.log(parent_visit_count)/visit_count);
        //maximizer and minimizer UCT nodes 
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
            HEIRUCTNode child = uctChildren.get(i);
            for(int j = 0;j<depth;j++) System.out.print("    ");
            System.out.println("child explored " + child.visit_count + " Avg evaluation: " + (child.accum_evaluation/((double)child.visit_count)) + " : " + actions.get(i));
            if (depth<maxdepth) child.showNode(depth+1,maxdepth);
        }
    }
}