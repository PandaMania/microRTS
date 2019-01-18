package ai.mcts.huct;

import ai.RandomBiasedAI;
import ai.core.AI;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.uct.MASTStrategy5;
import ai.mcts.uct.MASTStrategyO;
import ai.mcts.uct.MASTStrategyO1;
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

    public static void main(String args[]) throws Exception {


        ExperimentData data1 = new ExperimentData(Calendar.getInstance().getTime().toString(),"HEIRUCT+MyMAST","HEIRUCT+MASTO",1000);
        ExperimentData data2 = new ExperimentData(Calendar.getInstance().getTime().toString(),"HEIRUCT+MyMAST","HEIRUCT+MASTO1",1000);
        ExperimentData data3 = new ExperimentData(Calendar.getInstance().getTime().toString(),"HEIRUCT+MASTO","HEIRUCT+MASTO1",1000);

        for(int j=0; j< 3; j++) {

            for (int i = 0; i < 1; i++) {

                //    for (int j = 9; j < i; j++) {
                UnitTypeTable utt = new UnitTypeTable();
                PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/bases8x8.xml", utt);

                GameState gs = new GameState(pgs, utt);
                int MAXCYCLES = 3000;
                int PERIOD = 1;
                boolean gameover = false;

                AI ai1 = null; AI ai2 = null;
                if(j==0) {
                    //"HEIRUCT+MyMAST","HEIRUCT+MASTO"
                    ai1= new HEIRUCT(100, -1, 1000, 10, null, new SimpleSqrtEvaluationFunction3(), false, false, true,new MASTStrategy5());
                    ai2 = new HEIRUCT(100, -1, 1000, 10, null, new SimpleSqrtEvaluationFunction3(), false, false, true,new MASTStrategyO());


                }else if(j==1){
                    //"HEIRUCT+MyMAST","HEIRUCT+MASTO1"
                    ai1= new HEIRUCT(100, -1, 1000, 10, null, new SimpleSqrtEvaluationFunction3(), false, false, true,new MASTStrategy5());
                    ai2 = new HEIRUCT(100, -1, 1000, 10, null, new SimpleSqrtEvaluationFunction3(), false, false, true,new MASTStrategyO1());

                }
                else{
                    //"HEIRUCT+MASTO","HEIRUCT+MASTO1"
                    ai1= new HEIRUCT(100, -1, 1000, 10, null, new SimpleSqrtEvaluationFunction3(), false, false, true,new MASTStrategyO());
                    ai2 = new HEIRUCT(100, -1, 1000, 10,null, new SimpleSqrtEvaluationFunction3(), false, false, true,new MASTStrategyO1());

                }


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

                if(j==0) {
                    data1.results.add(gs.winner());
                    data1.times.add(gs.getTime());
                }else if(j==1){
                    data2.results.add(gs.winner());
                    data2.times.add(gs.getTime());
                }
                else{
                    data3.results.add(gs.winner());
                    data3.times.add(gs.getTime());
                }

            }
            /*if(j==10) {
                System.out.println("Finished - j="+j);
                System.out.println(data1.toString());
                String savedPath= data1.toFile();
                System.out.println("saved at "+savedPath);
            }
            else if(j==15) {
                System.out.println("Finished - j="+j);
                System.out.println(data2.toString());
                String savedPath= data2.toFile();
                System.out.println("saved at "+savedPath);
            }else{
                System.out.println("Finished - j="+j);
                System.out.println(data3.toString());
                String savedPath= data3.toFile();
                System.out.println("saved at "+savedPath);
            }*/

            System.out.println("Finished - j="+j);
            if(j==0) {
                System.out.println(data1.toString());
                String savedPath= data1.toFile();
                System.out.println("saved at "+savedPath);
            }else if(j==1){
                System.out.println(data2.toString());
                String savedPath= data2.toFile();
                System.out.println("saved at "+savedPath);
            }
            else{
                System.out.println(data3.toString());
                String savedPath= data3.toFile();
                System.out.println("saved at "+savedPath);
            }
        }



        /*System.out.println("SAVED AT PATH:"+path3);
        System.out.println("After:");

        ExperimentData data3 = ExperimentData.FromFile(data.path);

        System.out.println( data1.toString());*/

    }


}