package ai.mcts.uct;

import ai.core.AI;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.*;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import util.Sampler;

import java.util.*;

/**
 * @author Huy
 * MAST original version with UnitType
 */
public class MASTStrategyO1 extends MAST {
    private static final double BIAS_Q_VALUE = 1f;
    private static final double REGULAR_Q_VALUE = 0.2f;
    public static final double DECAY_FACTOR = 0.01f;
    public static final double EPSILON = 0.1f;
    //DEBUG CONTROLER >= 1 : ON , 0 : OFF
    private static int DEBUG = 0;
    private List<HashMap<UnitType, List<ActionQvalue>>> actionQvaluePerUnitTypeMapList;
    private EvaluationFunction ef;
    private UnitType baseType;
    private UnitType workerType;
    private UnitType barracksType;
    private UnitType lightType;
    private UnitType heavyType;
    private UnitType rangeType;
    //public int myPlayer;

    public MASTStrategyO1(){
        actionQvaluePerUnitTypeMapList =new ArrayList<>();
        //player 0
        actionQvaluePerUnitTypeMapList.add(new HashMap<>());
        //player 1
        actionQvaluePerUnitTypeMapList.add(new HashMap<>());

        ef =new SimpleSqrtEvaluationFunction3();

    }

    @Override
    public void simulate(GameState gs, int time){
        //List<PlayerAction> paList = new LinkedList<>();
        List<GameState> gsList = new LinkedList<>();
        boolean gameover = false;
        UnitTypeTable utt = gs.getUnitTypeTable();
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");

        do {

            if (gs.isComplete()) {
                //System.out.println("in cycle");
                gameover = gs.cycle();

            } else {
                //System.out.println("in getAction" + gs.getTime());
                if (DEBUG >= 2) {
                    System.out.println("******************SIMULATION AT TIME " + gs.getTime() + "******************************");
                    List<Integer> positionsUsed = gs.getResourceUsage().getPositionsUsed();
                    System.out.println("Before -Resource usage ");
                    for (Integer pos : positionsUsed) {
                        System.out.println(pos % gs.getPhysicalGameState().getWidth() + "," + pos / gs.getPhysicalGameState().getWidth());
                    }
                    List<Unit> units = gs.getUnits();
                    for (Unit test : units) {
                        System.out.println(test.toString());
                    }
                }

                PlayerAction player0Action = getAction(0, gs);
                //System.out.println("Player 0 -"+player0Action.toString());
                PlayerAction player1Action = getAction(1, gs);
                //System.out.println("Player 1 -"+player1Action.toString());

                gs.issue(player0Action);
                gs.issue(player1Action);
                gsList.add(gs);
                if (DEBUG >= 2) {
                    List<Integer> positionsUsed = gs.getResourceUsage().getPositionsUsed();
                    System.out.println("After -Resource usage ");
                    for (Integer pos : positionsUsed) {
                        System.out.println(pos % gs.getPhysicalGameState().getWidth() + "," + pos / gs.getPhysicalGameState().getWidth());
                    }
                    System.out.println("**********************************END**********************************************");
                }
            }
        } while (!gameover && gs.getTime() < time);

        updateQvalues(gameover,gsList,gs);
    }

    public void updateQvalues(boolean gameover,List<GameState> gsList,GameState lastGameState){
        int winner = lastGameState.winner();

        float result =  ef.evaluate(this.myPlayer,1-this.myPlayer,lastGameState);
        if(!gameover){
            if(result == 0.5f)
                winner = 1-this.myPlayer;
            else{
                winner = (result >= 0f? this.myPlayer:1-this.myPlayer);
            }
        }
        for(GameState gs:gsList){
            List<Unit> units = gs.getUnits();
            for(Unit u : units){
                if(u.getPlayer() >=0){
                    HashMap<UnitType, List<ActionQvalue>> actionQvaluePerUnitTypeMap = actionQvaluePerUnitTypeMapList.get(u.getPlayer());
                    if (actionQvaluePerUnitTypeMap.containsKey(u.getType())) {
                        List<ActionQvalue> actionQvalueList = actionQvaluePerUnitTypeMap.get(u.getType());
                        UnitAction selectedUA = gs.getUnitAction(u);
                        if(selectedUA != null) {
                            for (ActionQvalue aqv : actionQvalueList) {
                                if (aqv.equalsUnitAction(selectedUA)) {
                                    if(this.myPlayer != u.getPlayer()){
                                        if(result != 0.5f)
                                            result = -result;
                                    }
                                    boolean isWin = (winner == u.getPlayer());

                                    aqv.visit_count++;
                                    if(isWin){
                                        aqv.qvalue =  (aqv.qvalue + aqv.visit_count_per_match)/aqv.visit_count;
                                    }
                                    else{
                                        aqv.qvalue =  aqv.qvalue /aqv.visit_count;
                                    }
                                    aqv.resetVisitCountPerMatch();
                                    //aqv.qvalue = Math.min(0,Math.max(1,aqv.qvalue));

                                    //aqv.qvalue = aqv.qvalue * (1 - DECAY_FACTOR) + (isWin ? 1.0f : 0) * DECAY_FACTOR;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

    }



    @Override
    public void reset() {

    }

    public PlayerAction getAction(int player, GameState gameState){
        if (!gameState.canExecuteAnyAction(player)) {
            return new PlayerAction();
        }
        //prepareUnitActionTable(gameState, player);
        ResourceUsage base_ru = new ResourceUsage();
        PhysicalGameState pgs = gameState.getPhysicalGameState();
        for(Unit u:pgs.getUnits()) {
            UnitActionAssignment uaa = gameState.getUnitActions().get(u);
            if (uaa!=null) {
                UnitAction unitAction = new UnitAction(uaa.action);
                ResourceUsage ru =unitAction.resourceUsage(u, pgs);
                base_ru.merge(ru);
            }
        }

        PlayerAction playerAction = new PlayerAction();
        playerAction.setResourceUsage(base_ru.clone());

        //System.out.println("Player"+player);
        HashMap<UnitType,List<ActionQvalue>> unitActionQvalueTable = actionQvaluePerUnitTypeMapList.get(player);

        List<Unit> bases = new ArrayList<>();
        List<Unit> barrack = new ArrayList<>();
        List<Unit> workers = new ArrayList<>();
        List<Unit> others = new ArrayList<>();

        for (Unit u : gameState.getUnits()) {
            if (u.getPlayer()==player) {
                if (gameState.getActionAssignment(u) == null) {
                    UnitType unitType = u.getType();
                    if(unitType == baseType){
                        bases.add(u);
                    }
                    else if(unitType == workerType){
                        workers.add(u);
                    }
                    else if(unitType == barracksType){
                        barrack.add(u);
                    }
                    else {
                        others.add(u);
                    }
                }
            }
        }
        for(Unit u: barrack){
            addUnitActionIntoPlayerAction(playerAction,u,gameState,unitActionQvalueTable);
        }
        for(Unit u: bases){
            addUnitActionIntoPlayerAction(playerAction,u,gameState,unitActionQvalueTable);
        }
        for(Unit u: workers){
            addUnitActionIntoPlayerAction(playerAction,u,gameState,unitActionQvalueTable);
        }
        for(Unit u: others){
            addUnitActionIntoPlayerAction(playerAction,u,gameState,unitActionQvalueTable);
        }
        return playerAction;
    }

    void addUnitActionIntoPlayerAction(PlayerAction playerAction,Unit u,GameState gameState,HashMap<UnitType,List<ActionQvalue>> unitActionQvalueTable){

        PhysicalGameState pgs = gameState.getPhysicalGameState();
        List<UnitAction> choiceUAList = u.getUnitActions(gameState);
        List<ActionQvalue> actionQvalueList = null;

        List<ActionQvalue> avaiActionList= new ArrayList<>();
        if(unitActionQvalueTable.containsKey(u.getType())){
            actionQvalueList = unitActionQvalueTable.get(u.getType());
            for(UnitAction validAction: choiceUAList){
                boolean isExisted = false;
                for (ActionQvalue aqv : actionQvalueList) {
                    if (aqv.equalsUnitAction(validAction)) {
                        isExisted = true;
                        avaiActionList.add(new ActionQvalue(aqv));
                        break;
                    }
                }
                if (!isExisted) {
                    ActionQvalue newAqv = generateActionQvalue(validAction);
                    actionQvalueList.add(newAqv);

                    avaiActionList.add(new ActionQvalue(newAqv));
                }

            }

        }
        else{
            //new unit
            //try to add all possible actions which this unit can perform
            actionQvalueList = new ArrayList<>();
            for(UnitAction unitAction: choiceUAList){;
                ActionQvalue newAqv = generateActionQvalue(unitAction);
                actionQvalueList.add(newAqv);

                avaiActionList.add(new ActionQvalue(newAqv));
            }
            unitActionQvalueTable.put(u.getType(),actionQvalueList);

        }

        /*for(ActionQvalue aqv : actionQvalueList){
            if(isUnitActionAllowed(gameState,u,aqv.ua)) {
                aqv.ua.clearResourceUSageCache();
                avaiActionList.add(aqv);
            }
        }*/
        UnitAction none = new UnitAction(UnitAction.TYPE_NONE,10);
        UnitAction selectedUa = null;
        List<Double> dist =new ArrayList<>();
        for(ActionQvalue ap: avaiActionList){
            dist.add(ap.qvalue);
        }
        boolean test = true;
        if(!dist.isEmpty()) {
            try {
                int selectedIndex = Sampler.eGreedy(dist, EPSILON);
                selectedUa = avaiActionList.get(selectedIndex).ua;
                selectedUa.clearResourceUSageCache();
                ResourceUsage r2 = selectedUa.resourceUsage(u, pgs);
                if (!playerAction.getResourceUsage().consistentWith(r2, gameState)) {
                    // sample at random, eliminating the ones that have not worked so far:
                    List<Double> dist_l = new ArrayList<>(dist);
                    List<ActionQvalue> aqv_l = new ArrayList<>(avaiActionList);
                    if(dist_l.isEmpty()) {
                        if(DEBUG>=1)
                            System.out.println(u.toString()+": remove all of valid actions");
                    }
                    do {
                        dist_l.remove(selectedIndex);
                        aqv_l.remove(selectedIndex);
                        selectedIndex = Sampler.eGreedy(dist_l, EPSILON);
                        selectedUa = aqv_l.get(selectedIndex).ua;
                        selectedUa.clearResourceUSageCache();
                        r2 = selectedUa.resourceUsage(u, pgs);
                    } while (!playerAction.getResourceUsage().consistentWith(r2, gameState));
                }
                playerAction.getResourceUsage().merge(r2);

            } catch (Exception e) {
                //e.printStackTrace();
                test=false;
            }
        }else{
            if(DEBUG >=1)
                System.out.println(u.toString()+": cannnot find any valid unit_action");
        }
        if(!test){
            selectedUa = none;
            playerAction.getResourceUsage().merge(selectedUa.resourceUsage(u, pgs));
        }

        for(ActionQvalue updatedAqv: avaiActionList ){
            if(updatedAqv.equalsUnitAction(selectedUa)) {
                updatedAqv.visit_count_per_match ++;
                break;
            }
        }

        playerAction.addUnitAction(u, selectedUa);

    }

    private ActionQvalue generateActionQvalue(UnitAction validAction) {
        double initQvalue = 0;
        /*if(validAction.getType() == TYPE_HARVEST
                ||validAction.getType() == TYPE_RETURN
                ||validAction.getType() == TYPE_ATTACK_LOCATION)
            initQvalue = BIAS_Q_VALUE;
        else*/
        initQvalue = REGULAR_Q_VALUE;
        ActionQvalue newOne = new ActionQvalue(validAction, initQvalue);

        return  newOne;
    }

    @Override
    public AI clone() {
        return new MASTStrategy4();
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }

    private class ActionQvalue{
        public UnitAction ua;
        public double qvalue;
        public boolean isFirstTime;
        public int visit_count_per_match;
        public int visit_count;


        public ActionQvalue(UnitAction ua, double qvalue){
            this.ua = new UnitAction(ua);
            this.qvalue= qvalue;
            this.isFirstTime = true;
            this.visit_count_per_match = 0;
            this.visit_count = 0;
        }
        public ActionQvalue(ActionQvalue aqv){
            this.ua = new UnitAction(aqv.ua);
            this.qvalue= aqv.qvalue;
            this.isFirstTime = aqv.isFirstTime;
            this.visit_count_per_match = aqv.visit_count_per_match;
            this.visit_count = aqv.visit_count;
        }
        public boolean equalsUnitAction(UnitAction checkedUa){
            return ua.equals(checkedUa);
        }

        public void resetVisitCountPerMatch(){
            this.visit_count_per_match = 0;
        }
    }

}