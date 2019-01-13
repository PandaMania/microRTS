package tests;

import ai.core.AI;
import ai.mcts.uct.UCT;
import gui.PhysicalGameStatePanel;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import util.Pair;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class RolloutStrategyTest {
    public static void main(String args[]) throws Exception {
        List<TestInfo> testInfoList = new ArrayList<>();
        int numberOfTestMatches = 10;

        for (int i = 0; i < numberOfTestMatches; i++) {
            System.out.println("---"+i+" match is executing---");
            TestInfo info = new TestInfo(new String[]{"UCT-Random","UCT-MAST"});
            DoAMatch(info);
            testInfoList.add(info);
        }

        for (int i = 0; i < numberOfTestMatches; i++) {
            System.out.println(testInfoList.toString());
        }
    }

    private static void DoAMatch(TestInfo info)throws Exception {
        UnitTypeTable utt = new UnitTypeTable();
        PhysicalGameState pgs = PhysicalGameState.load("maps/16x16/basesWorkers16x16.xml", utt);
//        PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();

        GameState gs = new GameState(pgs, utt);
        int MAXCYCLES = 5000;
        int PERIOD = 20;
        boolean gameover = false;

        AI ai1 = new UCT(utt,false);//new RandomBiasedAI(utt);// new WorkerRush(utt, new BFSPathFinding());
        AI ai2 = new UCT(utt,true);//new RandomBiasedAI();
        //JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_BLACK);
//        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_WHITE);

        long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
        do{
            if (System.currentTimeMillis()>=nextTimeToUpdate) {
                PlayerAction pa1 = ai1.getAction(0, gs);
                PlayerAction pa2 = ai2.getAction(1, gs);
                gs.issueSafe(pa1);
                gs.issueSafe(pa2);
                info.updateActionCount(pa1);
                info.updateActionCount(pa2);
                // simulate:
                gameover = gs.cycle();
                //w.repaint();
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
        info.recordResults(gs);
        //System.out.println("Game Over");
    }

    public static long testIdCounter=0;
    private static class TestInfo{

        public long testID;
        public String[] aiNames;// aiNames[0] vs aiNames[1]
        public int result; // 0: player0 win, 1:player1 win, -1: draw
        public int consumedTime;
        public long[] nWaitAction;
        public long[] nProduceAction;
        public long[] nAttackAction;
        public long[] nMoveAction;
        public long[] nHarvestAction;
        public long[] nReturnAction;

        public TestInfo(String[] aiNames){
            testID = testIdCounter++;
            this.aiNames = aiNames;
            this.nWaitAction = new long[this.aiNames.length];
            this.nProduceAction = new long[this.aiNames.length];
            this.nAttackAction = new long[this.aiNames.length];
            this.nMoveAction = new long[this.aiNames.length];
            this.nHarvestAction = new long[this.aiNames.length];
            this.nReturnAction = new long[this.aiNames.length];
        }

        public void recordResults(GameState finalState){
            result = finalState.winner();
            consumedTime = finalState.getTime();
        }
        public void updateActionCount(PlayerAction playerAction){
            List<Pair<Unit, UnitAction>> pairList= playerAction.getActions();

            for(Pair<Unit,UnitAction> pair : pairList){
                int player =pair.m_a.getPlayer();
                if( player>=0){
                    UnitAction ua = pair.m_b;
                    switch (ua.getType()){
                        case UnitAction.TYPE_NONE:
                            nWaitAction[player]++;
                            break;
                        case UnitAction.TYPE_ATTACK_LOCATION:
                            nAttackAction[player]++;
                            break;
                        case UnitAction.TYPE_MOVE:
                            nMoveAction[player]++;
                            break;
                        case UnitAction.TYPE_PRODUCE:
                            nProduceAction[player]++;
                            break;
                        case UnitAction.TYPE_HARVEST:
                            nHarvestAction[player]++;
                            break;
                        case UnitAction.TYPE_RETURN:
                            nReturnAction[player]++;
                            break;
                        default:
                            break;
                    }
                }

            }
        }
        public String toString(){
            String s="id: "+testID+"|consumedTime: "+consumedTime+"\n";
            s+="\t"+aiNames[0]+"-\t"+aiNames[1]+"\n";
            s+="RESULT:\t"+result;
            s+="NUM_WAIT:\t"+nWaitAction[0]+"-\t"+nWaitAction[1]+"\n";
            s+="NUM_PROD:\t"+nProduceAction[0]+"-\t"+nProduceAction[1]+"\n";
            s+="NUM_ATTA:\t"+nAttackAction[0]+"-\t"+nAttackAction[1]+"\n";
            s+="NUM_MOVE:\t"+nMoveAction[0]+"-\t"+nMoveAction[1]+"\n";
            s+="NUM_HARV:\t"+nHarvestAction[0]+"-\t"+nHarvestAction[1]+"\n";
            s+="NUM)RETU:\t"+nReturnAction[0]+"-\t"+nReturnAction[1]+"\n";

            return s;
        }
    }
}
