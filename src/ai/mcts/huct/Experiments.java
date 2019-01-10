package ai.mcts.huct;

import ai.RandomBiasedAI;
import ai.core.AI;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import gui.PhysicalGameStatePanel;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import ai.core.AI;
import ai.*;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.huct.HEIRUCT;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.scv.SCV;
import gui.PhysicalGameStatePanel;
import java.io.OutputStreamWriter;
import javax.swing.JFrame;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import util.XMLWriter;
import javax.swing.*;

/**
 * Created by jonty on 10/01/2019.
 */
public class Experiments {

 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


    /**
     * @author santi
     */

    public static void main(String args[]) throws Exception {
        for (int i = 10; i < 20; i++) {
            for (int j = 9; j < i; j++) {

                UnitTypeTable utt = new UnitTypeTable();
                PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/bases8x8.xml", utt);
//        PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();

                GameState gs = new GameState(pgs, utt);
                int MAXCYCLES = 3000;
                int PERIOD = 20;
                boolean gameover = false;

                AI ai1 = new HEIRUCT(300, 3000, 100, i, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), false, false, false);
                 AI ai2 = new HEIRUCT(300, 3000, 100, j, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), false, false, false);
               // AI ai1 = new RandomAI();
                JFrame w = PhysicalGameStatePanel.newVisualizer(gs, 640, 640, false, PhysicalGameStatePanel.COLORSCHEME_BLACK);
//        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_WHITE);

                long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
                do {
                    if (System.currentTimeMillis() >= nextTimeToUpdate) {
                        PlayerAction pa1 = ai1.getAction(0, gs);
                        PlayerAction pa2 = ai2.getAction(1, gs);
                        gs.issueSafe(pa1);
                        gs.issueSafe(pa2);

                        // simulate:
                        gameover = gs.cycle();
                        w.repaint();
                        nextTimeToUpdate += PERIOD;
                    } else {
                        try {
                            Thread.sleep(1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } while (!gameover && gs.getTime() < MAXCYCLES);
                ai1.gameOver(gs.winner());
                ai2.gameOver(gs.winner());

                System.out.println("Game Over");
                System.out.println("i= " + i + " j= " + j);
                if(gs.winner()==0){
                    System.out.println("Winner= " + i );
                }else
                    System.out.println("Winner= " + j);
                System.out.println();


            }
        }
    }
}