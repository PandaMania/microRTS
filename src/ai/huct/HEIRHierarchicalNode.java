/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.mcts.huct;
import java.util.ArrayList;
import java.util.Random;
import rts.GameState;

/**
 *
 * @author santi
 */
public class HEIRHierarchicalNode {
    static Random r = new Random();
    public static float C = 0.05f;   // this is the constant that regulates exploration vs exploitation, it must be tuned for each domain
//    public static float C = 1;   // this is the constant that regulates exploration vs exploitation, it must be tuned for each domain

    public int type;    // 0 : max, 1 : min, -1: Game-over
    HEIRUCTNode uctParent = null;
    public HEIRHierarchicalNode hParent = null;
    public GameState gs;

    boolean hasMoreActions = true;
    public int actionIndex;
    public HEIRHierarchicalNode[] hChildren = null;
    public ArrayList<HEIRUCTNode> uctChildrenSorted = null;
    public ArrayList<HEIRHierarchicalNode> hChildrenSorted = null;
    public HEIRUCTNode[] uctChildren = null;
    float evaluation_bound = 0;
    float accum_evaluation = 0;
    int visit_count = 0;


    public HEIRHierarchicalNode(int actNum, HEIRUCTNode a_parent, float bound) throws Exception {
        uctParent = a_parent;
        evaluation_bound = bound;
        hChildren = new HEIRHierarchicalNode[actNum];
        hChildrenSorted = new ArrayList<HEIRHierarchicalNode>();
        uctChildrenSorted = new ArrayList<HEIRUCTNode>();
        uctChildren = new HEIRUCTNode[actNum];
    }

    public HEIRHierarchicalNode(int actNum, HEIRHierarchicalNode a_parent, float bound) throws Exception {
        hParent = a_parent;
        evaluation_bound = bound;
        hChildren = new HEIRHierarchicalNode[actNum];
        hChildrenSorted = new ArrayList<HEIRHierarchicalNode>();
        uctChildrenSorted = new ArrayList<HEIRUCTNode>();
        uctChildren = new HEIRUCTNode[actNum];
    }

    public double childValue(){
        double exploitation = ((double)this.accum_evaluation) / this.visit_count;
        double exploration = Math.sqrt(Math.log((double)visit_count)/this.visit_count);
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
        for(int i = 0;i<hChildrenSorted.size();i++) {
            HEIRHierarchicalNode child = hChildrenSorted.get(i);
            for(int j = 0;j<depth;j++) System.out.print("    ");
            //System.out.println("child explored " + child.visit_count + " Avg evaluation: " + (child.accum_evaluation/((double)child.visit_count)) + " : " + actions.get(i));
            if (depth<maxdepth) child.showNode(depth+1,maxdepth);
        }
    }
}