package ai.mcts.uct;

import ai.core.AI;
import ai.core.ParameterSpecification;
import ai.montecarlo.lsi.Sampling;
import rts.*;
import rts.units.Unit;
import util.Pair;
import util.Sampler;

import javax.swing.*;
import java.util.*;

import static rts.UnitAction.*;

public class MASTStrategy4 extends AI {

    private static final double DEFAULT_Q_VALUE = 1;
    public static final double DISCOUNT_FACTOR = 0.1f;
    public static final double EPSILON = 0.1f;

    private int actionCount;
    private List<HashMap<Long, List<ActionQvalue>>> actionQvaluePerUnitMapList;
    private List<ProduceSituation> produceSituationList;
    public MASTStrategy4(){
        actionQvaluePerUnitMapList =new ArrayList<>();
        //player 0
        actionQvaluePerUnitMapList.add(new HashMap<>());
        //player 1
        actionQvaluePerUnitMapList.add(new HashMap<>());

        produceSituationList = new ArrayList<>();
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
                /*System.out.println("******************SIMULATION AT TIME "+gs.getTime()+"******************************");

                List<Integer> positionsUsed =gs.getResourceUsage().getPositionsUsed();
                System.out.println("Before -Resource usage ");
                for(Integer pos: positionsUsed){
                    System.out.println(pos% gs.getPhysicalGameState().getWidth()+","+ pos/ gs.getPhysicalGameState().getWidth() );
                }
                List<Unit> units = gs.getUnits();
                for(Unit test:units){
                    System.out.println(test.toString());
                }*/

                PlayerAction player0Action = getAction(0, gs);
                System.out.println("Player 0 -"+player0Action.toString());
                PlayerAction player1Action = getAction(1, gs);
                System.out.println("Player 1 -"+player1Action.toString());

                gs.issue(player0Action);
                gs.issue(player1Action);
                gsList.add(gs.clone());

                /*positionsUsed =gs.getResourceUsage().getPositionsUsed();
                System.out.println("After -Resource usage ");
                for(Integer pos: positionsUsed){
                    System.out.println(pos% gs.getPhysicalGameState().getWidth()+","+ pos/ gs.getPhysicalGameState().getWidth() );
                }
                System.out.println("**********************************END**********************************************");*/
            }
        }while(!gameover && gs.getTime()<2000);



        updateQvalues(gameover,gs.winner(),gsList);
        //updateQvalues(gameover,gs);
    }
    public void updateQvalues(boolean gameover,GameState gameState){
        if(gameover){
            int winner = gameState.winner();
            for(int i=0;i<2;i++){
                List<PlayerAction> paList =gameState.getPlayerActions(i);
                HashMap<Long, List<ActionQvalue>> actionQvaluePerUnitMap = actionQvaluePerUnitMapList.get(i);
                for(PlayerAction pa: paList){
                    List<Pair<Unit,UnitAction>> pairs = pa.getActions();
                    for(Pair<Unit,UnitAction> pair:pairs){
                        Unit u = pair.m_a;
                        UnitAction selectedUA = pair.m_b;
                        if (actionQvaluePerUnitMap.containsKey(u.getID())) {
                            List<ActionQvalue> actionQvalueList = actionQvaluePerUnitMap.get(u.getID());
                            if(selectedUA != null) {
                                for (ActionQvalue aqv : actionQvalueList) {
                                    if (aqv.equalsUnitAction(selectedUA)) {
                                        boolean isWin = (winner == u.getPlayer());

                                        aqv.qvalue = aqv.qvalue * (1 - DISCOUNT_FACTOR) + (isWin ? 1.0f : 0) * DISCOUNT_FACTOR;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    public void updateQvalues(boolean gameover,int winner,List<GameState> gsList){
        if(gameover){
            for(GameState gs : gsList){
                //for(int i=gsList.size()-1;i>=0;i--){
                //GameState gs = gsList.get(i);
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

                                        aqv.qvalue = aqv.qvalue * (1 - DISCOUNT_FACTOR) + (isWin ? 1.0f : 0) * DISCOUNT_FACTOR;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            /*for(int i=0;i<actionQvaluePerUnitMapList.size();i++){
                HashMap<Long, List<ActionQvalue>> actionQvaluePerUnitMap = actionQvaluePerUnitMapList.get(i);
                Set<Map.Entry<Long,List<ActionQvalue>>> set = actionQvaluePerUnitMap.entrySet();
                String debug = "";
                for(Map.Entry<Long,List<ActionQvalue>> entry:set){
                    List<ActionQvalue> temp = entry.getValue();
                    debug ="Unit"+entry.getKey()+": ";
                    for(ActionQvalue aqv: temp){
                        debug+= aqv.ua.toString()+":"+aqv.qvalue+"|\t";
                    }
                    System.out.println(debug);
                }
            }*/
        }
    }
    private class ActionQvalue{
        public UnitAction ua;
        public double qvalue;


        public ActionQvalue(UnitAction ua, double qvalue){
            this.ua = ua;
            this.qvalue= qvalue;
        }
        public boolean equalsUnitAction(UnitAction checkedUa){
            return ua.equals(checkedUa);
        }
    }


    @Override
    public void reset() {

    }

    public PlayerAction getAction(int player, GameState gameState){
        if (!gameState.canExecuteAnyAction(player) || gameState.winner()!= -1) {
            return new PlayerAction();
        }
        //prepareUnitActionTable(gameState, player);
        ResourceUsage base_ru = new ResourceUsage();
        PhysicalGameState pgs = gameState.getPhysicalGameState();
        for(Unit u:pgs.getUnits()) {
            UnitActionAssignment uaa = gameState.getUnitActions().get(u);
            if (uaa!=null) {
                ResourceUsage ru = uaa.action.resourceUsage(u, pgs);
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
                                    break;
                                }
                            }
                            if (!isExisted) {
                                ActionQvalue newOne = new ActionQvalue(validAction, DEFAULT_Q_VALUE);
                                actionQvalueList.add(newOne);
                            }
                        }

                    }
                    else{
                        //new unit
                        //try to add all possible actions which this unit can perform
                        actionQvalueList = new ArrayList<>();
                        for(UnitAction unitAction: choiceUAList){
                            ActionQvalue aqv = new ActionQvalue(unitAction,DEFAULT_Q_VALUE);
                            actionQvalueList.add(aqv);
                        }
                        unitActionQvalueTable.put(u.getID(),actionQvalueList);

                    }

                    List<Integer> positionsUsed =base_ru.getPositionsUsed();
                    for(ActionQvalue aqv : actionQvalueList){

                        if(isUnitActionAllowed(gameState,u,aqv.ua)) {// gameState.isUnitActionAllowed(u,aqv.ua)){
                            /*if (TryToCheckByOtherWay(base_ru, gameState, u, aqv.ua))
                            {
                                avaiActionList.add(aqv);
                            }*/


                            if(!positionsUsed.isEmpty()) {
                                boolean consistent = true;
                                for (Integer pos : positionsUsed) {
                                    int targetx = pos % pgs.getWidth();
                                    int targety = pos / pgs.getWidth();
                                    GameState checkGs = gameState.clone();
                                    Unit postUnit = checkGs.getUnit(u.getID());
                                    try {
                                        if(aqv.ua.getType() == TYPE_PRODUCE){
                                            Player p = checkGs.getPlayer(player);
                                            if (p.getResources() - aqv.ua.getUnitType().cost<0) {
                                                consistent=  false;
                                            }
                                        }
                                        if(consistent) {
                                            aqv.ua.execute(postUnit, checkGs);

                                            if (targetx == postUnit.getX() && targety == postUnit.getY()) {
                                                consistent = false;
                                                break;
                                            }
                                        }


                                    } catch (Exception ex) {
                                        //System.out.println("Good! i caught you");
                                        consistent = false;
                                    }

                                }
                                if(consistent)
                                {
                                    avaiActionList.add(aqv);
                                }
                            }else{
                                avaiActionList.add(aqv);
                            }

                        }
                    }

                    UnitAction selectedUa = new UnitAction(UnitAction.TYPE_NONE,10);
                    List<Double> dist =new ArrayList<>();
                    for(ActionQvalue ap: avaiActionList){
                        dist.add(ap.qvalue);
                    }
                    if(!dist.isEmpty()) {
                        try {
                            int selectedIndex = Sampler.eGreedy(dist, EPSILON);
                            selectedUa = avaiActionList.get(selectedIndex).ua;
                            ResourceUsage r2 = selectedUa.resourceUsage(u, pgs);
                            if (!playerAction.getResourceUsage().consistentWith(r2, gameState)) {
                                // sample at random, eliminating the ones that have not worked so far:
                                List<Double> dist_l = new ArrayList<>(dist);
                                List<ActionQvalue> aqv_l = new ArrayList<>(avaiActionList);
                                if(dist_l.isEmpty()) {
                                    System.out.println("BUG");
                                    continue;
                                }
                                do {
                                    dist_l.remove(selectedIndex);
                                    aqv_l.remove(selectedIndex);
                                    selectedIndex = Sampler.eGreedy(dist_l, EPSILON);
                                    selectedUa = aqv_l.get(selectedIndex).ua;
                                    r2 = selectedUa.resourceUsage(u, pgs);
                                } while (!playerAction.getResourceUsage().consistentWith(r2, gameState));
                            }
                            playerAction.getResourceUsage().merge(r2);

                        } catch (Exception e) {
                            //e.printStackTrace();
                        }
                    }else{
                        System.out.println("Im empty");
                    }
                    playerAction.addUnitAction(u, selectedUa);

                }
            }
        }
        return playerAction;
    }


    private boolean TryToCheckByOtherWay(ResourceUsage base_ru, GameState gameState, Unit unit, UnitAction ua) {

        Unit checkUnit = unit;
        if (checkUnit==null) {
            return false;
        }

        if (!checkUnit.canExecuteAction(ua, gameState)) {
            return  false;
        }
        PhysicalGameState pgs = gameState.getPhysicalGameState();
        // get the unit that corresponds to that action (since the state might have been cloned):
        List<Unit> unitList= pgs.getUnits();
        if (unitList.indexOf(checkUnit)==-1) {
            boolean found = false;
            for(Unit u:unitList) {
                if (u.getClass()==checkUnit.getClass() &&
//                        u.getID() == p.m_a.getID()) {
                        u.getX()==checkUnit.getX() &&
                        u.getY()==checkUnit.getY()) {
                    checkUnit = u;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }


            // check to see if the action is legal!
            ResourceUsage ru = ua.resourceUsage(checkUnit, pgs);
            for(int position:ru.getPositionsUsed()) {
                int y = position/pgs.getWidth();
                int x = position%pgs.getWidth();
                if (pgs.getTerrain(x, y) != PhysicalGameState.TERRAIN_NONE ||
                        pgs.getUnitAt(x, y) != null) {
                    return false;
                }
            }

            Player p = gameState.getPlayer(unit.getPlayer());
        if ( ua.getType() == UnitAction.TYPE_PRODUCE && p.getResources() < ua.getUnitType().cost) {
            return false;
        }
        PlayerAction pa = new PlayerAction();
        pa.getResourceUsage().merge(base_ru.clone());
        //pa.getResourceUsage().merge(ru);
        //pa.addUnitAction(checkUnit,ua);
        //TODO: DO NOT SAVE GAMESTATE, CAN USE GAMESTATE.GETPLAYERACTIONS() IN UPDATE_Q_VALUES
        //gameState.getPlayerActions()
        boolean ret = pa.consistentWith(ru,gameState);
        return ret;
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
                empty.getResourceUsage().merge(ru);
            }
        }

        if (ua.resourceUsage(u, pgs).consistentWith(empty.getResourceUsage(), gs)) return true;

        return false;
    }
    private void prepareUnitActionTable(GameState gameState, int player) throws Exception {
        HashMap<Long, List<ActionQvalue>> unitActionTable = actionQvaluePerUnitMapList.get(player);


        actionCount = 0;
        PlayerActionGenerator moveGenerator = new PlayerActionGenerator(gameState, player);
        int idx = 0;
        for (Pair<Unit, List<UnitAction>> choice : moveGenerator.getChoices()) {
            if(!unitActionTable.containsKey(choice.m_a.getID())) {
                List<ActionQvalue> actionQvalueList = new ArrayList<>();
                for (UnitAction ua : choice.m_b) {
                    actionQvalueList.add(new ActionQvalue(ua, DEFAULT_Q_VALUE));
                }
                unitActionTable.put(choice.m_a.getID(), actionQvalueList);
            }else{
                List<ActionQvalue> actionQvalueList = unitActionTable.get(choice.m_a.getID());
                if(actionQvalueList.size() != choice.m_b.size()){
                    if(actionQvalueList.size() > choice.m_b.size()){
                        System.out.println("so strange");
                    }
                    for (UnitAction ua : choice.m_b) {
                        boolean existed = false;
                        for(ActionQvalue aqv: actionQvalueList){
                            if(aqv.equalsUnitAction(ua)){
                                existed = true;
                                break;
                            }
                        }
                        if(!existed){
                            actionQvalueList.add(new ActionQvalue(ua,DEFAULT_Q_VALUE));
                        }
                    }
                }
            }
            actionCount += choice.m_b.size();
        }

    }
    private class ProduceSituation{
        public int startingTime;
        public UnitActionAssignment uaa;

        public ProduceSituation(int startingTime,Unit u, UnitAction unitAction){
            this.startingTime = startingTime;
            this.uaa = new UnitActionAssignment(u,unitAction,u.getType().produceTime);
        }
        public boolean isOutOfDate(int time,int delayTime){
            return startingTime+uaa.time < time+delayTime;
        }
        public boolean isConflict(GameState currentGameState,Unit checkingUnit, UnitAction checkingUA){

            GameState gs = currentGameState.clone();
            Unit postUnit = gs.getUnit(checkingUnit.getID());

            checkingUA.execute(postUnit,gs);
            int targetx = uaa.unit.getX();
            int targety = uaa.unit.getY();
            switch(uaa.action.getDirection()) {
                case DIRECTION_UP:      targety--; break;
                case DIRECTION_RIGHT:   targetx++; break;
                case DIRECTION_DOWN:    targety++; break;
                case DIRECTION_LEFT:    targetx--; break;
            }
            if(postUnit.getX() == targetx && postUnit.getY() == targety){
                if(checkingUA.ETA(checkingUnit) + currentGameState.getTime() < startingTime+uaa.time){
                    return  true;
                }
            }

            return false;
        }

    }
    @Override
    public AI clone() {
        return new MASTStrategy4();
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }

}
