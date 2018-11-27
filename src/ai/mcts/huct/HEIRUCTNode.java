/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.mcts.huct;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;


/**
 *
 * @author santi
 */
public class HEIRUCTNode {
    static Random r = new Random();
    public static float C = 0.05f;   // this is the constant that regulates exploration vs exploitation, it must be tuned for each domain
//    public static float C = 1;   // this is the constant that regulates exploration vs exploitation, it must be tuned for each domain

    public int type;    // 0 : max, 1 : min, -1: Game-over
    HEIRUCTNode uctParent = null;
    public HEIRHierarchicalNode hParent = null;
    public GameState gs;
    int depth = 0;  // the depth in the tree

    boolean hasMoreActions = true;
    PlayerActionGenerator moveGenerator = null;
    int[] squadIdxs = null;
    public List<PlayerAction> actions = null;
    public ArrayList<HEIRUCTNode> uctChildrenSorted = null;
    public HEIRHierarchicalNode[] hChildren = null;
    //This list is for ordering based on the HEIRUCT values
    public ArrayList<HEIRHierarchicalNode> hChildrenSorted = null;
    float evaluation_bound = 0;
    float accum_evaluation = 0;
    int visit_count = 0;


    public HEIRUCTNode(int maxplayer, int minplayer, GameState a_gs, HEIRUCTNode a_parent, HEIRHierarchicalNode h_parent, float bound) throws Exception {
        uctParent = a_parent;
        hParent = h_parent;
        if(h_parent != null)
            h_parent.uctChildrenSorted.add(this);
        if(a_parent != null)
            a_parent.uctChildrenSorted.add(this);
        gs = a_gs;
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
            uctChildrenSorted = new ArrayList<HEIRUCTNode>();
            hChildrenSorted = new ArrayList<HEIRHierarchicalNode>();
            hChildren = new HEIRHierarchicalNode[moveGenerator.getChoiceSizes()[0]];

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
            uctChildrenSorted = new ArrayList<HEIRUCTNode>();
            hChildrenSorted = new ArrayList<HEIRHierarchicalNode>();
            hChildren = new HEIRHierarchicalNode[moveGenerator.getChoiceSizes()[0]];

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
        if (hasMoreActions) {
            if (moveGenerator==null) {
//                System.out.println("No more leafs because moveGenerator = null!");
                return this;
            }
            PlayerAction a = moveGenerator.getNextAction(cutOffTime);
            if (a!=null) {
                HEIRUCTNode node = null;
                int[] choices = moveGenerator.getCurrentChoice();
                int[] choicesizes = moveGenerator.getChoiceSizes();
                //getNextAction set the action index to invalid
                for (int i = 0; i < choices.length; i++) {
                    if (choices[i] >= choicesizes[i])
                        choices[i]--;
                }
                if (squadIdxs.length>1) {
                    if (hChildren[choices[0]] == null ) {
                        hChildren[choices[0]] = new HEIRHierarchicalNode(choicesizes[squadIdxs[1]], this, evaluation_bound);
                        hChildrenSorted.add(hChildren[choices[0]]);
                    }

                    HEIRHierarchicalNode HNode = hChildren[choices[0]];
                    int i;
                    for (i = 1; i< choices.length-1; i++)
                    {
                        if (HNode.hChildren[choices[squadIdxs[i]]] == null) {
                            HNode.hChildren[choices[squadIdxs[i]]] = new HEIRHierarchicalNode(choicesizes[squadIdxs[i+1]], this, evaluation_bound);
                            HNode.hChildrenSorted.add(HNode.hChildren[choices[squadIdxs[i]]]);
                            HNode = HNode.hChildren[choices[squadIdxs[i]]];
                            i++;
                            break;
                        }
                        HNode = HNode.hChildren[choices[squadIdxs[i]]];
                    }
                    int cc=0;
                    if (HNode.hChildren.length==choices[squadIdxs[i]])
                        cc+=1;
                    //After finding a non-visited sub-action we do not have to check for null pointer
                    for (; i< choices.length-1; i++)
                    {
                        HNode.hChildren[choices[squadIdxs[i]]] = new HEIRHierarchicalNode(choicesizes[squadIdxs[i+1]], this, evaluation_bound);
                        HNode.hChildrenSorted.add(HNode.hChildren[choices[squadIdxs[i]]]);
                        HNode = HNode.hChildren[choices[squadIdxs[i]]];
                    }

                    actions.add(a);
                    GameState gs2 = gs.cloneIssue(a);
                    node = new HEIRUCTNode(maxplayer, minplayer, gs2.clone(), this, HNode, evaluation_bound);
                    HNode.uctChildren[choices[squadIdxs[i]]] = node;
                }
                else {
                    actions.add(a);
                    GameState gs2 = gs.cloneIssue(a);
                    node = new HEIRUCTNode(maxplayer, minplayer, gs2.clone(), this, null, evaluation_bound);
                }

                return node;
            } else {
                hasMoreActions = false;
            }
        }

        // Bandit policy:
        double best_score = 0;

        HEIRHierarchicalNode hBest = null;
        ArrayList<HEIRHierarchicalNode> hChildren = this.hChildrenSorted;
        if (hChildren.size() != 0) {
            for (int i = 0; i< squadIdxs.length-1; i++) {
                hBest = null;
                for (HEIRHierarchicalNode child : hChildren) {
                    double tmp = child.childValue();
                    if (hBest==null || tmp>best_score) {
                        hBest = child;
                        best_score = tmp;
                    }
                }
                hChildren = hBest.hChildrenSorted;
            }
        }

        ArrayList<HEIRUCTNode> uctChildren;
        //If there is at least one level of hierarchy
        if (hBest != null)
            uctChildren = hBest.uctChildrenSorted;
        else
            uctChildren = this.uctChildrenSorted;
        HEIRUCTNode best = null;
        for (HEIRUCTNode child : uctChildren) {
            double tmp = childValue(child);
            if (best==null || tmp>best_score) {
                best = child;
                best_score = tmp;
            }
        }

        if (best==null) {
//            System.out.println("No more leafs because this node has no children!");
//            return null;
            return this;
        }
        return best.UCTSelectLeaf(maxplayer, minplayer, cutOffTime, max_depth);
//        return best;
    }

    public double childValue(HEIRUCTNode child) {
        double exploitation = ((double)child.accum_evaluation) / child.visit_count;
        double exploration = Math.sqrt(Math.log((double)visit_count)/child.visit_count);
        if (type==0) {
            // max node:
            exploitation = (evaluation_bound + exploitation)/(2*evaluation_bound);
        } else {
            exploitation = (evaluation_bound - exploitation)/(2*evaluation_bound);
        }
//            System.out.println(exploitation + " + " + exploration);

        double tmp = C*exploitation + exploration;
        return tmp;
    }


    public void showNode(int depth, int maxdepth) {
        int mostVisitedIdx = -1;
        HEIRUCTNode mostVisited = null;
        for(int i = 0;i<uctChildrenSorted.size();i++) {
            HEIRUCTNode child = uctChildrenSorted.get(i);
            for(int j = 0;j<depth;j++) System.out.print("    ");
            System.out.println("child explored " + child.visit_count + " Avg evaluation: " + (child.accum_evaluation/((double)child.visit_count)) + " : " + actions.get(i));
            if (depth<maxdepth) child.showNode(depth+1,maxdepth);
        }
    }
}