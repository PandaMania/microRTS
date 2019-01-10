package ai.mcts.uct;

import rts.*;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import util.Pair;
import util.Sampler;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static rts.UnitAction.*;

public class MASTStrategy3 {
    private static final double DEFAULT_Q_VALUE = 1;
    public final double DISCOUNT_FACTOR = 0.1f;
    List<HashMap<Long,List<ActionQvalue>>> actionQvaluePerUnitMapList;
    public MASTStrategy3(){
        actionQvaluePerUnitMapList =new ArrayList<>();
        //player 0
        actionQvaluePerUnitMapList.add(new HashMap<>());
        //player 1
        actionQvaluePerUnitMapList.add(new HashMap<>());

        produceActionGSTimeList = new ArrayList<>();
    }
    private int simulationTimes = 0;
    public void simulate(GameState gs, int time){
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%simulationTimes ="+simulationTimes+" at time = "+time+"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        simulationTimes++;
        //produceActionGSTimeList.clear();

        List<GameState> gsList = new ArrayList<>();
        boolean gameover = false;
        do{
            if (gs.isComplete()) {
                gameover = gs.simulate();//cycle();

            } else {
                System.out.println("@@@@@@@@@@@@@@@@@gs time before getting Action: "+gs.getTime()+"@@@@@@@@");
                List<Unit> l = gs.getUnits();
                for(Unit u:l){
                    System.out.println(u.toString());
                }
                //UNABLE TO GET ACTION ???
                //GET PLAYER_ACTION REQUIRED <UNIT,UNIT_ACTION>
                PlayerAction player0Action = getAction(0, gs);
                PlayerAction player1Action = getAction(1, gs);
                System.out.println("@@@@@@@@@@@@@@@@@gs time after getting Action: "+gs.getTime()+"@@@@@@@@");
                l = gs.getUnits();
                for(Unit u:l){
                    System.out.println(u.toString());
                }
                gs.issue(player0Action);
                gs.issue(player1Action);
                gsList.add(gs);


                //System.out.println("Im cycling");
            }
        }while(!gameover && gs.getTime()<time);

        updateQvalues(gameover,gsList);

    }
    public void updateQvalues(boolean gameover,List<GameState> gsList){
        if(gameover){
            for(int i=gsList.size()-1;i>=0;i--){
                GameState gs = gsList.get(i);
                List<Unit> unitList= gs.getUnits();
                for(Unit u : unitList){
                    if(u.getPlayer() >= 0) {
                        HashMap<Long, List<ActionQvalue>> actionQvaluePerUnitMap = actionQvaluePerUnitMapList.get(u.getPlayer());
                        if (actionQvaluePerUnitMap.containsKey(u.getID())) {
                            List<ActionQvalue> actionQvalueList = actionQvaluePerUnitMap.get(u.getID());
                            UnitAction selectedUA = gs.getUnitAction(u);
                            for (ActionQvalue aqv : actionQvalueList) {
                                if (aqv.equalsUnitAction(selectedUA)) {
                                    boolean isWin = (gs.winner() == u.getPlayer());
                                    aqv.qvalue = aqv.qvalue * (1 - DISCOUNT_FACTOR) + (isWin ? 1 : 0) * DISCOUNT_FACTOR;
                                    break;
                                }
                            }

                        }
                    }
                }
            }
        }
    }
    public PlayerAction getAction(int player, GameState gs) {
        HashMap<Long, List<ActionQvalue>> actionQvaluePerUnitMap = actionQvaluePerUnitMapList.get(player);
        PlayerAction pa = new PlayerAction();
        PhysicalGameState pgs = gs.getPhysicalGameState();
        if (!gs.canExecuteAnyAction(player)) return pa;

        //System.out.println("Im in getAction of player" + player);
        // Generate the reserved resources:
        for (Unit u : pgs.getUnits()) {
            UnitActionAssignment uaa = gs.getActionAssignment(u);
            if (uaa != null) {
                ResourceUsage ru = uaa.action.resourceUsage(u, pgs);
                pa.getResourceUsage().merge(ru);
            }
        }
        checkingGameState = gs.clone();
        String s="\nplayer"+player+"\n";
        for(Unit u:pgs.getUnits()) {
            if (u.getPlayer() == player) {
                if (gs.getActionAssignment(u) == null) {
                    s+= "\nunitId:"+u.getID()+"\t|"+u.getX()+","+u.getY()+"\t|"+u.getType().name;
                    UnitAction none = new UnitAction(UnitAction.TYPE_NONE, 10);
                    List<UnitAction> l = u.getUnitActions(gs);
                    s+= "\n\tListActions:";
                    for(UnitAction ua:l) {
                        s += ua.toString() + "\t";
                    }
                    s+= "\n\tAvaiActions:";
                    /*for(UnitAction a:l) {
                        if (a.getType()==UnitAction.TYPE_NONE) {
                            none = a;
                            break;
                        }
                    }*/
                    List<ActionQvalue> actionQvalueList = null;
                    if(actionQvaluePerUnitMap.containsKey(u.getID())){
                        actionQvalueList = actionQvaluePerUnitMap.get(u.getID());
                        //add new found actions
                        for(UnitAction unitAction: l){
                            if(!IsActionUnitContainedInList(actionQvalueList,unitAction)
                            ) {
                                ActionQvalue aqv = new ActionQvalue(unitAction, DEFAULT_Q_VALUE);
                                actionQvalueList.add(aqv);
                            }
                        }
                    }
                    else{
                        //new unit
                        //try to add all possible actions which this unit can perform
                        actionQvalueList = new ArrayList<>();
                        for(UnitAction unitAction: l){
                            ActionQvalue aqv = new ActionQvalue(unitAction,DEFAULT_Q_VALUE);
                            actionQvalueList.add(aqv);
                        }
                        actionQvaluePerUnitMap.put(u.getID(),actionQvalueList);
                    }
                    int i=0;
                    List<ActionQvalue> tmpList= new ArrayList<>();
                    for(ActionQvalue ap: actionQvalueList){
                        if(l.contains(ap.ua)&&gs.isUnitActionAllowed(u,ap.ua)) {
                            if(checkPostGameState(u,ap.ua)) { // do not filter enough to get avai actions
                                i++; // allowed, so use available prob
                                tmpList.add(ap);
                            }
                        }
                    }

                    double[] distribution =new double[i];
                    i=0;
                    for(ActionQvalue aqv:tmpList){
                        distribution[i] = gibbsSampling(aqv,tmpList);
                        s+= aqv.ua.toString()+"\t";
                        i++;
                    }
                    s+="\n";
                    try{

                        //extract to player action
                        ActionQvalue selectedAp = null;
                        UnitAction ua = null;
                        if(tmpList.isEmpty()){
                            ua= none;
                        }
                        else {
                            selectedAp = tmpList.get(Sampler.weighted(distribution));
                            ua= selectedAp.ua;
                        }
                        if (ua.resourceUsage(u, pgs).consistentWith(pa.getResourceUsage(), gs)) {
                            ResourceUsage ru = ua.resourceUsage(u, pgs);
                            pa.getResourceUsage().merge(ru);
                            pa.addUnitAction(u, ua);
                            if(ua.getType() == TYPE_PRODUCE ){
                                boolean found = false;
                                ProduceSituation newps = new ProduceSituation(u,ua,gs.getTime());
                                for(ProduceSituation ps: produceActionGSTimeList){
                                    if(ps.x == newps.x && ps.y == newps.y){
                                        found  = true;
                                        break;
                                    }
                                }
                                if(!found)
                                    produceActionGSTimeList.add(newps);
                            }
                        } else {
                            pa.addUnitAction(u, none);
                        }

                        //ua.execute(u,checkingGameState);
                        System.out.println("checking game state informationnnnnnnnnnnnnnn");
                        checkingGameState= gs.clone(); // <= error | you have to find a function can clone gamestate or a unittypetable
                        ua.execute(checkingGameState.getUnit(u.getID()),checkingGameState); // <- it affects original game state
                        //ua.execute();
                        List<Unit> temppppp = checkingGameState.getUnits();
                        for(Unit uee:temppppp){
                            System.out.println(uee.toString());
                        }
                        s +="\t|chosenAction:"+ua.toString()+"\t";
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.out.println("unitId"+u.getID()+" performs null move");
                        pa.addUnitAction(u, none);
                    }
                }
            }
        }
        System.out.println(s);
        return pa;
    }

    List<ProduceSituation> produceActionGSTimeList;
    HashMap<Integer,List<UnitAction>> produceActionMap;

    private double gibbsSampling(ActionQvalue aqv, List<ActionQvalue> aqvList){
        double probability = 0;

        double expTotal = 0;
        for(ActionQvalue aq : aqvList){
            expTotal +=  Math.exp(aq.qvalue);
        }

        probability = Math.exp(aqv.qvalue) / expTotal;
        return  probability;
    }
    GameState checkingGameState =null;
    private boolean checkPostGameState(Unit u,UnitAction ua){
        //PlayerAction pa2 = pa.clone();
        //checkingGameState.issue(pa2);
        //checkingGameState.cycle();
        //System.out.println("PA: "+pa2.toString());

        boolean  ret = checkingGameState.isUnitActionAllowed(u,ua);
        if(ret){
            if(!produceActionGSTimeList.isEmpty()) {
                int currTime = checkingGameState.getTime();
                List<ProduceSituation> removedList = new ArrayList<>();
                for (ProduceSituation ps : produceActionGSTimeList) {
                    /*int oldGsTime = ps.gsTime;
                    if(currTime - oldGsTime > ps.ua.getUnitType().produceTime){
                        removedList.add(ps);
                    }
                    else*/
                    System.out.println("Produce situation: ("+ps.x+","+ps.y+")");
                    {

                        //available produce

                        GameState gs2 =checkingGameState.clone();
                        Unit finalCheckingUnit = gs2.getUnit(u.getID());
                        boolean temp = finalCheckingUnit.canExecuteAction(ua,gs2);
                        if(!temp)
                            return false;
                        else {
                            ua.execute(finalCheckingUnit, gs2);
                            if (ps.x == finalCheckingUnit.getX() && ps.y == finalCheckingUnit.getY()) {
                                return false;
                            }
                        }

                    }
                }
                /*if(!removedList.isEmpty()){
                    for(ProduceSituation removedPs : removedList){
                        this.produceActionGSTimeList.remove(removedPs);
                    }
                }*/
            }


        }
        return ret;
    }
    private boolean IsActionUnitContainedInList(List<ActionQvalue> actionQvalueList,UnitAction ua){

        for(ActionQvalue aqv:actionQvalueList){
            if(aqv.equalsUnitAction(ua) )
                return  true;
        }
        return  false;
    }
    //only ref unit action and probability of it per a unit
    private class ActionQvalue{
        public UnitAction ua;
        public double qvalue;
        public boolean isFirstTime;


        public ActionQvalue(UnitAction ua, double qvalue){
            this.ua = ua;
            this.qvalue= qvalue;
            isFirstTime = true;
        }
        public boolean equalsUnitAction(UnitAction checkedUa){
            return ua.equals(checkedUa);
        }
    }

    private class ProduceSituation{
        public int x;
        public int y;
        public Unit u;
        public UnitAction ua;
        public int gsTime;

        public  ProduceSituation(Unit u,UnitAction ua,int gsTime){
            this.gsTime = gsTime;
            this.ua = ua;
            this.u = u;
            int targetx = u.getX();
            int targety = u.getY();
            int parameter = ua.getDirection();
            switch(parameter) {
                case DIRECTION_UP:      targety--; break;
                case DIRECTION_RIGHT:   targetx++; break;
                case DIRECTION_DOWN:    targety++; break;
                case DIRECTION_LEFT:    targetx--; break;
            }
            x = targetx;
            y = targety;

        }
    }
}
