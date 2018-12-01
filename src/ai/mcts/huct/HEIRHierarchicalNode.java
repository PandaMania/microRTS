/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.mcts.huct;
import java.util.ArrayList;
import java.util.Random;

import javafx.util.Pair;
import rts.GameState;

/**
 *
 * @author santi
 */
public class HEIRHierarchicalNode {
    static Random r = new Random();
    public static float C = 1f;   // this is the constant that regulates exploration vs exploitation, it must be tuned for each domain
//    public static float C = 1;   // this is the constant that regulates exploration vs exploitation, it must be tuned for each domain

    public int type;    // 0 : max, 1 : min, -1: Game-over
    HEIRUCTNode uctParent = null;
    public HEIRHierarchicalNode hParent = null;
    public GameState gs;
    public int actionIdx;

    boolean hasMoreActions = true;
    public int actionIndex;
    public ArrayList<Pair<Integer, HEIRHierarchicalNode>> hChildren = null;
    public ArrayList<Pair<Integer, HEIRUCTNode>> uctChildren = null;
    float evaluation_bound = 0;
    float accum_evaluation = 0;
    int visit_count = 0;


    public HEIRHierarchicalNode(HEIRUCTNode a_parent, float bound) throws Exception {
        uctParent = a_parent;
        type=a_parent.type;
        evaluation_bound = bound;
        hChildren = new ArrayList<Pair<Integer, HEIRHierarchicalNode>>();
        uctChildren = new ArrayList<Pair<Integer, HEIRUCTNode>>();
        actionIdx =0;
    }

    public HEIRHierarchicalNode(HEIRHierarchicalNode a_parent, float bound) throws Exception {
        hParent = a_parent;
        type=a_parent.type;
        evaluation_bound = bound;
        hChildren = new ArrayList<Pair<Integer, HEIRHierarchicalNode>>();
        uctChildren = new ArrayList<Pair<Integer, HEIRUCTNode>>();
        actionIdx = 0;
    }

    public double childValue(double parent_visit_count){
        double exploitation = ((double)this.accum_evaluation) / this.visit_count;
        double exploration = Math.sqrt(Math.log(parent_visit_count) / visit_count);
        if (hParent == null)
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
        for(int i = 0;i<hChildren.size();i++) {
            HEIRHierarchicalNode child = hChildren.get(i).getValue();
            for(int j = 0;j<depth;j++) System.out.print("    ");
            //System.out.println("child explored " + child.visit_count + " Avg evaluation: " + (child.accum_evaluation/((double)child.visit_count)) + " : " + actions.get(i));
            if (depth<maxdepth) child.showNode(depth+1,maxdepth);
        }
    }
}