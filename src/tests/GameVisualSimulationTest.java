 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import ai.core.AI;
import ai.*;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.huct.HEIRUCT;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.mcts.uct.MASTStrategy5;
import ai.mcts.uct.MASTStrategyO;
import ai.mcts.uct.MASTStrategyO1;
import ai.mcts.uct.UCT;
import ai.montecarlo.lsi.LSI;
import ai.scv.SCV;
import gui.PhysicalGameStatePanel;
import java.io.OutputStreamWriter;
import javax.swing.JFrame;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import util.XMLWriter;

/**
 *
 * @author santi
 */
public class GameVisualSimulationTest {
    public static void main(String args[]) throws Exception {
        UnitTypeTable utt = new UnitTypeTable();
        PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml",utt);//PhysicalGameState.load("maps/16x16/basesWorkers16x16.xml", utt);//PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml",utt);//PhysicalGameState.load("maps/16x16/basesWorkers16x16.xml", utt);//
//        PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();

        GameState gs = new GameState(pgs, utt);
        int MAXCYCLES = 5000;
        int PERIOD = 1;
        boolean gameover = false;
        
        AI ai1 = new UCT(100,-1,1000,10,new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(),true, new MASTStrategy5());//new HEIRUCT(100,-1,1000,14,new RandomBiasedAI(),new SimpleSqrtEvaluationFunction3(),false,false,true,new MASTStrategy5());
        AI ai2 = new NaiveMCTS(100,-1,1000,10,0.3f, 0.0f, 0.4f,new RandomBiasedAI(),new SimpleSqrtEvaluationFunction3(),true,true,new MASTStrategy5());//new HEIRUCT(100,-1,1000,14,new RandomBiasedAI(),new SimpleSqrtEvaluationFunction3(),false,false,true,new MASTStrategy5());



        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_BLACK);
//        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_WHITE);

        long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
        do{
            if (System.currentTimeMillis()>=nextTimeToUpdate) {
                PlayerAction pa1 = ai1.getAction(0, gs);
                PlayerAction pa2 = ai2.getAction(1, gs);
                gs.issueSafe(pa1);
                gs.issueSafe(pa2);

                // simulate:
                gameover = gs.cycle();
                w.repaint();
                nextTimeToUpdate+=PERIOD;
            } else {
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }while(!gameover && gs.getTime()<MAXCYCLES);
        ai1.gameOver(gs.winner());
        ai2.gameOver(gs.winner());
        w.repaint();
        System.out.println("Game Over");
    }    
}
