package ai.mcts.huct;

import ai.core.AI;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.uct.MASTStrategy5;
import ai.mcts.uct.UCT;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

import java.util.Calendar;

public class ExperimentingPlayout {

    public static void main(String args[]) throws Exception {
        System.out.println("testing");

        ExperimentData data1 = new ExperimentData(Calendar.getInstance().getTime().toString(), "HUCT", "UCT", 1000);

        for (int j = 0; j < 1; j++) {

            for (int i = 0; i < 200; i++) {

                //    for (int j = 9; j < i; j++) {
                UnitTypeTable utt = new UnitTypeTable();
                PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/bases8x8.xml", utt);

                GameState gs = new GameState(pgs, utt);
                int MAXCYCLES = 3000;
                int PERIOD = 1;
                boolean gameover = false;

                AI ai1 = null;
                AI ai2 = null;

                //"HEIRUCT+MyMAST","HEIRUCT+MASTO"
                ai1 = new HEIRUCT(100, -1, 1000, 20, null, new SimpleSqrtEvaluationFunction3(), false, false, true, new MASTStrategy5());
               // ai1 = new NaiveMCTS(utt);
                ai2=new UCT(utt);



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

                if (j == 0) {
                    data1.results.add(gs.winner());
                    data1.times.add(gs.getTime());

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

                    System.out.println("Finished - j=" + j);
                    if (j == 0) {
                        System.out.println(data1.toString());
                        String savedPath = data1.toFile();
                        System.out.println("saved at " + savedPath);

                    }



        /*System.out.println("SAVED AT PATH:"+path3);
        System.out.println("After:");

        ExperimentData data3 = ExperimentData.FromFile(data.path);

        System.out.println( data1.toString());*/

                }


            }
        }
    }
    }