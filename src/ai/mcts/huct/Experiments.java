package ai.mcts.huct;

import ai.RandomBiasedAI;
import ai.core.AI;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.uct.MASTStrategy5;
import ai.mcts.uct.UCT;
import ai.montecarlo.lsi.LSI;
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
import sun.util.calendar.BaseCalendar;
import util.XMLWriter;
import javax.swing.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;


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
        String path= "test-"+"HEIRUCTwMAST"+"-"+"HEIRUCTwRAN"+"-1000.csv";

        ExperimentData data1;

        int x;
        for(int j=4; j< 6; j++) {
            x=500*j;


                data1= new ExperimentData(Calendar.getInstance().getTime().toString(),"HUCT_depth1000","HUCT_depth"+x,1000);

            for (int i = 0; i < 200; i++) {

            //    for (int j = 9; j < i; j++) {
            UnitTypeTable utt = new UnitTypeTable();
            PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/bases8x8.xml", utt);
//        PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();

            GameState gs = new GameState(pgs, utt);
            int MAXCYCLES = 3000;
            int PERIOD = 1;
            boolean gameover = false;

            AI ai1 = new HEIRUCT(100, -1, 1000, 20, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), false, false, true, new MASTStrategy5());

            AI ai2 = new HEIRUCT(100, -1, x, 20, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), false, false, true, new MASTStrategy5());


            // AI ai1 = new RandomAI();
            // JFrame w = PhysicalGameStatePanel.newVisualizer(gs, 640, 640, false, PhysicalGameStatePanel.COLORSCHEME_BLACK);
//       JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_WHITE);

            long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
            do {
                if (System.currentTimeMillis() >= nextTimeToUpdate) {
                    PlayerAction pa1 = ai1.getAction(0, gs);
                    PlayerAction pa2 = ai2.getAction(1, gs);
                    gs.issueSafe(pa1);
                    gs.issueSafe(pa2);

                    // simulate:
                    gameover = gs.cycle();
                    //w.repaint();
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

          /*System.out.println("Game Over");
          System.out.println("i= " + "true" + " j= " + "false");
          if (gs.winner() == 0) {
              System.out.println("Winner= " + 10);
          } else
              System.out.println("Winner= " + 9);*/

                data1.results.add(gs.winner());
                data1.times.add(gs.getTime());


            //   }
        }

            System.out.println("Finished - j="+j);
            System.out.println(data1.toString());
            String savedPath= data1.toFile();
            System.out.println("saved at "+savedPath);

    }



        /*System.out.println("SAVED AT PATH:"+path3);
        System.out.println("After:");

        ExperimentData data3 = ExperimentData.FromFile(data.path);

        System.out.println( data1.toString());*/

    }
}