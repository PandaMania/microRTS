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
public class HEIRHierarchicalNode extends HEIRNode{

    boolean hasMoreActions = true;

    public HEIRHierarchicalNode(HEIRUCTNode a_parent, float bound) throws Exception {
        uctParent = a_parent;
        type=a_parent.type;
        evaluation_bound = bound;
        hChildren = new ArrayList<HEIRHierarchicalNode>();
        uctChildren = new ArrayList<HEIRUCTNode>();
        actionIdx =0;
    }

    public HEIRHierarchicalNode(HEIRHierarchicalNode a_parent, float bound) throws Exception {
        hParent = a_parent;
        type=a_parent.type;
        evaluation_bound = bound;
        hChildren = new ArrayList<HEIRHierarchicalNode>();
        uctChildren = new ArrayList<HEIRUCTNode>();
        actionIdx = 0;
    }

    public double childValue(HEIRNode parent){
        double exploitation = ((double)this.accum_evaluation) / this.visit_count;
        double exploration = Math.sqrt(Math.log(parent.visit_count) / visit_count);
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
            HEIRHierarchicalNode child = hChildren.get(i);
            for(int j = 0;j<depth;j++) System.out.print("    ");
            //System.out.println("child explored " + child.visit_count + " Avg evaluation: " + (child.accum_evaluation/((double)child.visit_count)) + " : " + actions.get(i));
            if (depth<maxdepth) child.showNode(depth+1,maxdepth);
        }
    }
}