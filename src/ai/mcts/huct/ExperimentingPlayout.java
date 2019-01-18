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
import sun.util.calendar.BaseCalendar;
import util.XMLWriter;
import javax.swing.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class ExperimentingPlayout {


/**
 * Created by jonty on 10/01/2019.
 */

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
        data1= new ExperimentData(Calendar.getInstance().getTime().toString(),"HEIRUCT+Saving","HEIRUCT+depth10",1000);
        ExperimentData data2 = new ExperimentData(Calendar.getInstance().getTime().toString(),"HEIRUCT+Saving","HEIRUCT+depth15",1000);
        ExperimentData data3 = new ExperimentData(Calendar.getInstance().getTime().toString(),"HEIRUCT+Saving","HEIRUCT+depth20",1000);
int x;
        for(int j=1; j< 6; j++) {
            if(j==2){
                j++;
            }
             x=j*500;
            data1= new ExperimentData(Calendar.getInstance().getTime().toString(),"HEIRUCT-MastNorm","HEIRUCT+rollout" + x,1000);

            for (int i = 0; i < 200; i++) {

                //    for (int j = 9; j < i; j++) {
                UnitTypeTable utt = new UnitTypeTable();
                PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/bases8x8.xml", utt);
//        PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();

                GameState gs = new GameState(pgs, utt);
                int MAXCYCLES = 3000;
                int PERIOD = 1;
                boolean gameover = false;

                AI ai1 = new HEIRUCT(100, 1000, 1000, 20, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), false, false, true);
                AI ai2 = new HEIRUCT(100, x, 1000, 20, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), false, false, true);

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
                if(j==0) {
                    data1.results.add(gs.winner());
                    data1.times.add(gs.getTime());
                }else{
                    data2.results.add(gs.winner());
                    data2.times.add(gs.getTime());
                }

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