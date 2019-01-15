package ai.mcts.uct;

import ai.core.AI;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.*;
import rts.units.Unit;
import util.Sampler;

import java.util.*;

import static rts.UnitAction.*;

public class MASTStrategy4 extends AI {
    private static final double BIAS_Q_VALUE = 1f;
    private static final double REGULAR_Q_VALUE = 0.2f;
    public static final double DECAY_FACTOR = 0.01f;
    public static final double EPSILON = 0.1f;
    //DEBUG CONTROLER >= 1 : ON , 0 : OFF
    private static int DEBUG = 0;
    private List<HashMap<Long, List<ActionQvalue>>> actionQvaluePerUnitMapList;
    private EvaluationFunction ef;

    public MASTStrategy4(){
        actionQvaluePerUnitMapList =new ArrayList<>();
        //player 0
        actionQvaluePerUnitMapList.add(new HashMap<>());
        //player 1
        actionQvaluePerUnitMapList.add(new HashMap<>());

        ef =new SimpleSqrtEvaluationFunction3();
    }

    public void simulate(GameState gs, int time){
        List<GameState> gsList = new ArrayList<>();
        boolean gameover = false;

        for(Unit u : gs.getUnits()){
            if(u.getPlayer() >=0) {
                HashMap<Long, List<ActionQvalue>> actionQvaluePerUnitMap = actionQvaluePerUnitMapList.get(u.getPlayer());
                if (actionQvaluePerUnitMap.isEmpty())
                    break;
                if (!actionQvaluePerUnitMap.containsKey(u.getID())) {
                    actionQvaluePerUnitMap.remove(u.getID());

                }
            }
        }

        do{

            if (gs.isComplete()) {
                gameover = gs.cycle();

            } else {
                if(DEBUG>=2){
                    System.out.println("******************SIMULATION AT TIME "+gs.getTime()+"******************************");
                    List<Integer> positionsUsed =gs.getResourceUsage().getPositionsUsed();
                    System.out.println("Before -Resource usage ");
                    for(Integer pos: positionsUsed){
                        System.out.println(pos% gs.getPhysicalGameState().getWidth()+","+ pos/ gs.getPhysicalGameState().getWidth() );
                    }
                    List<Unit> units = gs.getUnits();
                    for(Unit test:units){
                        System.out.println(test.toString());
                    }
                }
                PlayerAction player0Action = getAction(0, gs);
                //System.out.println("Player 0 -"+player0Action.toString());
                PlayerAction player1Action = getAction(1, gs);
                //System.out.println("Player 1 -"+player1Action.toString());

                gs.issue(player0Action);
                gs.issue(player1Action);
                gsList.add(gs.clone());

                if(DEBUG>=2) {
                    List<Integer> positionsUsed = gs.getResourceUsage().getPositionsUsed();
                    System.out.println("After -Resource usage ");
                    for (Integer pos : positionsUsed) {
                        System.out.println(pos % gs.getPhysicalGameState().getWidth() + "," + pos / gs.getPhysicalGameState().getWidth());
                    }
                    System.out.println("**********************************END**********************************************");
                }
            }
        }while(!gameover && gs.getTime()<time);



        updateQvalues(gameover,gsList,gs);
    }
    public void updateQvalues(boolean gameover,List<GameState> gsList,GameState lastGameState){
        int winner = lastGameState.winner();
        float result =1;
        if(!gameover){
            result =  ef.evaluate(0,1,lastGameState);
            if(result == 0.5f)
                winner = 1;
            else{
                winner = (result >= 0f? 0:1);
            }
        }else{
            System.out.println("checking");
        }
        double times=1f;
        for(GameState gs : gsList){
            List<Unit> unitList= gs.getUnits();
            for(Unit u : unitList){
                if(u.getPlayer() >= 0) {
                    HashMap<Long, List<ActionQvalue>> actionQvaluePerUnitMap = actionQvaluePerUnitMapList.get(u.getPlayer());
                    if (actionQvaluePerUnitMap.containsKey(u.getID())) {
                        List<ActionQvalue> actionQvalueList = actionQvaluePerUnitMap.get(u.getID());
                        UnitAction selectedUA = gs.getUnitAction(u);
                        if(selectedUA != null) {
                            for (ActionQvalue aqv : actionQvalueList) {
                                if (aqv.equalsUnitAction(selectedUA)) {
                                    boolean isWin = (winner == u.getPlayer());
                                    double effectRate = 1/times;
                                    aqv.qvalue = aqv.qvalue * (1 - DECAY_FACTOR) + (isWin ? effectRate : 0) * DECAY_FACTOR;
                                    //aqv.qvalue = aqv.qvalue * (1 - DECAY_FACTOR) + (isWin ? 1.0f : 0) * DECAY_FACTOR;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            times+=0.1f;
        }
        if(DEBUG>=2) {
            for (int i = 0; i < actionQvaluePerUnitMapList.size(); i++) {
                HashMap<Long, List<ActionQvalue>> actionQvaluePerUnitMap = actionQvaluePerUnitMapList.get(i);
                Set<Map.Entry<Long, List<ActionQvalue>>> set = actionQvaluePerUnitMap.entrySet();
                String debug = "";
                for (Map.Entry<Long, List<ActionQvalue>> entry : set) {
                    List<ActionQvalue> temp = entry.getValue();
                    debug = "Unit" + entry.getKey() + ": ";
                    for (ActionQvalue aqv : temp) {
                        debug += aqv.ua.toString() + ":" + aqv.qvalue + "|\t";
                    }
                    System.out.println(debug);
                }
            }
        }
    }



    @Override
    public void reset() {

    }

    public PlayerAction getAction(int player, GameState gameState){
        if (!gameState.canExecuteAnyAction(player) ) {
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
        HashMap<Long,List<ActionQvalue>> unitActionQvalueTable = actionQvaluePerUnitMapList.get(player);
        for (Unit u : gameState.getUnits()) {
            if (u.getPlayer()==player) {
                if (gameState.getActionAssignment(u) == null) {
                    List<UnitAction> choiceUAList = u.getUnitActions(gameState);
                    List<ActionQvalue> actionQvalueList = null;

                    List<ActionQvalue> avaiActionList= new ArrayList<>();
                    if(unitActionQvalueTable.containsKey(u.getID())){
                        actionQvalueList = unitActionQvalueTable.get(u.getID());
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
                        unitActionQvalueTable.put(u.getID(),actionQvalueList);

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
                            e.printStackTrace();
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

                    playerAction.addUnitAction(u, selectedUa);
                }
            }
        }
        return playerAction;
    }

    private ActionQvalue generateActionQvalue(UnitAction validAction) {
        double initQvalue = 0;
        if(validAction.getType() == TYPE_HARVEST
                ||validAction.getType() == TYPE_RETURN
                ||validAction.getType() == TYPE_ATTACK_LOCATION)
            initQvalue = BIAS_Q_VALUE;
        else
            initQvalue = REGULAR_Q_VALUE;
        ActionQvalue newOne = new ActionQvalue(validAction, initQvalue);

        return  newOne;
    }

    public boolean isUnitActionAllowed(GameState gs,Unit u, UnitAction ua) {
        PlayerAction empty = new PlayerAction();
        PhysicalGameState pgs = gs.getPhysicalGameState();
        if (ua.getType()==UnitAction.TYPE_MOVE || ua.getType() == UnitAction.TYPE_PRODUCE) {
            int x2 = u.getX() + UnitAction.DIRECTION_OFFSET_X[ua.getDirection()];
            int y2 = u.getY() + UnitAction.DIRECTION_OFFSET_Y[ua.getDirection()];
            if (x2<0 || y2<0 ||
                    x2>=pgs.getWidth() ||
                    y2>=pgs.getHeight() ||
                    pgs.getTerrain(x2, y2) == PhysicalGameState.TERRAIN_WALL ||
                    pgs.getUnitAt(x2, y2) != null) return false;
        }

        // Generate the reserved resources:
        for(Unit u2:pgs.getUnits()) {
            UnitActionAssignment uaa = gs.getActionAssignment(u2);
            if (uaa!=null) {
                ResourceUsage ru = uaa.action.resourceUsage(u2, pgs);
                uaa.action.clearResourceUSageCache();
                empty.getResourceUsage().merge(ru);
            }
        }

        //System.out.print("RCurrent GameState vs "+u.toString()+","+ua.toString()+"\n");
        List<Integer> positionsUsed =empty.getResourceUsage().getPositionsUsed();
        for(Integer pos:positionsUsed){
            int posx= pos%pgs.getWidth();
            int posy= pos/pgs.getHeight();
            //System.out.print("GS("+posx+","+posy+") |");
            ua.clearResourceUSageCache();
            List<Integer> uaPositionUsed= ua.resourceUsage(u,pgs).clone().getPositionsUsed();

            if(uaPositionUsed.isEmpty()){
                int uaPosx= u.getX();
                int uaPosy= u.getY();
                //System.out.println("U,UA("+uaPosx+","+uaPosy+")");
                if(posx == uaPosx && posy == uaPosy){
                    return false;
                }
            }else{
                //System.out.print("U,UA:");
                /*for(Integer temp: uaPositionUsed){
                    int tempx= temp%pgs.getWidth();
                    int tempy= temp/pgs.getHeight();
                    System.out.print("("+tempx+","+tempy+")");
                }*/
                //System.out.println();

                if(uaPositionUsed.contains(pos))
                    return false;
            }
        }
        if(ua.getType() == UnitAction.TYPE_PRODUCE){
            Player p = gs.getPlayer(u.getPlayer());
            if(p.getResources() - ua.getUnitType().cost <0)
                return false;
        }
        if (ua.resourceUsage(u, pgs).consistentWith(empty.getResourceUsage(), gs))
            return true;

        return false;
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


        public ActionQvalue(UnitAction ua, double qvalue){
            this.ua = new UnitAction(ua);
            this.qvalue= qvalue;
            this.isFirstTime = true;
        }
        public ActionQvalue(ActionQvalue aqv){
            this.ua = new UnitAction(aqv.ua);
            this.qvalue= aqv.qvalue;
            this.isFirstTime = aqv.isFirstTime;
        }
        public boolean equalsUnitAction(UnitAction checkedUa){
            return ua.equals(checkedUa);
        }
    }
}
