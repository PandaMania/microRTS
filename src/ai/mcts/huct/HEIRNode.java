package ai.mcts.huct;

import java.util.ArrayList;
import java.util.Random;

import javafx.util.Pair;

//Superclass of HEIRHierarchicalNode and HEIRUCTNode
public class HEIRNode {
	static Random r = new Random();
    public static float C = 1f;   // this is the constant that regulates exploration vs exploitation, it must be tuned for each domain
//    public static float C = 1;   // this is the constant that regulates exploration vs exploitation, it must be tuned for each domain

	
	public ArrayList<HEIRHierarchicalNode> hChildren = null;
    public ArrayList<HEIRUCTNode> uctChildren = null;
    
    float evaluation_bound = 0;
    float accum_evaluation = 0;
    int visit_count = 0;
    //This value is updated for every node to know which child of action was added previously
    public int actionIdx;
    
    public int type;    // 0 : max, 1 : min, -1: Game-over
    HEIRUCTNode uctParent = null;
    public HEIRHierarchicalNode hParent = null;
}
