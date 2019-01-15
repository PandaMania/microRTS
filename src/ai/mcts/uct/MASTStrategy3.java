package ai.mcts.uct;

import rts.*;
import rts.units.Unit;
import util.Sampler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static rts.UnitAction.*;

public class MASTStrategy3 {
    private static final double DEFAULT_Q_VALUE = 1;
    public static final double DISCOUNT_FACTOR = 0.1f;
    public static final double EPSILON = 0.1f;
    private GameState lastGameState;
    //GameState checkingGameState =null;
    private int simulationTimes = 0;
    private PlayerActionGenerator generator;
    HashMap<Integer,List<UnitAction>> produceActionMap;
    private List<HashMap<Long,List<ActionQvalue>>> actionQvaluePerUnitMapList;
    public MASTStrategy3(){
        actionQvaluePerUnitMapList =new ArrayList<>();
        //player 0
        actionQvaluePerUnitMapList.add(new HashMap<>());
        //player 1
        actionQvaluePerUnitMapList.add(new HashMap<>());

    }


    public void simulate(GameState gs, int time){
        //System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%simulationTimes ="+simulationTimes+" at time = "+time+"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        simulationTimes++;
        List<GameState> gsList = new ArrayList<>();
        boolean gameover = false;



        do{
            if (gs.isComplete()) {
                gameover = gs.cycle();

            } else {
                PlayerAction player0Action = getAction2(0, gs);
                PlayerAction player1Action = getAction2(1, gs);

                gs.issue(player0Action);
                gs.issue(player1Action);
                gsList.add(gs);


                //System.out.println("Im cycling");
            }
        }while(!gameover && gs.getTime()<10000);

        updateQvalues(gameover,gsList);

    }
    public void updateQvalues(boolean gameover,List<GameState> gsList){
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
                            for (ActionQvalue aqv : actionQvalueList) {
                                if (aqv.equalsUnitAction(selectedUA)) {
                                    boolean isWin = (gs.winner() == u.getPlayer());
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

    public GameState getLastGameState() {
        return lastGameState;
    }

    PlayerAction pa2;
    public PlayerAction getAction2(int player, GameState gs){
        // attack, harvest and return have 5 times the probability of other actions
        PhysicalGameState pgs = gs.getPhysicalGameState();
        PlayerAction pa = new PlayerAction();

        if (!gs.canExecuteAnyAction(player)) return pa;
        lastGameState = gs.clone();
        pa2 = new PlayerAction();
        HashMap<Long, List<ActionQvalue>> actionQvaluePerUnitMap = actionQvaluePerUnitMapList.get(player);
        // Generate the reserved resources:
        for(Unit u:pgs.getUnits()) {
            UnitActionAssignment uaa = gs.getActionAssignment(u);
            if (uaa!=null) {
                ResourceUsage ru = uaa.action.resourceUsage(u, pgs);
                pa.getResourceUsage().merge(ru);
            }
        }

        for(Unit u:pgs.getUnits()) {
            if (u.getPlayer()==player) {
                if (gs.getActionAssignment(u)==null) {
                    List<UnitAction> l = u.getUnitActions(gs);
                    UnitAction none = new UnitAction(TYPE_NONE,10);
                    int nActions = l.size();
                    double []distribution = new double[nActions];
                    List<ActionQvalue> actionQvalueList = null;
                    if(actionQvaluePerUnitMap.containsKey(u.getID())){
                        actionQvalueList = actionQvaluePerUnitMap.get(u.getID());
                        //add new found actions
                        for(UnitAction unitAction: l){
                            if(!IsActionUnitContainedInList(actionQvalueList,unitAction)
                            ) {
                                ActionQvalue aqv = new ActionQvalue(unitAction, DEFAULT_Q_VALUE);
                                actionQvalueList.add(aqv);
                                actionQvaluePerUnitMap.put(u.getID(),actionQvalueList);
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
                    UnitAction selectedUa = none;
                    if(!actionQvalueList.isEmpty()){
                        List<ActionQvalue> tmpList= new ArrayList<>();
                        for(ActionQvalue ap: actionQvalueList){
                            if(gs.isUnitActionAllowed(u,ap.ua))
                            {
                                tmpList.add(ap);
                            }
                        }
                        List<Double> dist =new ArrayList<>();
                        for(ActionQvalue ap: tmpList){
                            dist.add(ap.qvalue);
                        }
                        // Select the best combination that results in a valid playeraction by epsilon-greedy sampling:
                        ResourceUsage base_ru = new ResourceUsage();
                        for(Unit unit:gs.getUnits()) {
                            UnitAction ua = gs.getUnitAction(unit);
                            if (ua!=null) {
                                ResourceUsage ru = ua.resourceUsage(unit, gs.getPhysicalGameState());
                                base_ru.merge(ru);
                            }
                        }

                        pa2 = new PlayerAction();
                        pa2.setResourceUsage(base_ru.clone());
                        try {
                            UnitAction ua;
                            ResourceUsage r2;
                            int selectedIndex = Sampler.eGreedy(dist,EPSILON);
                            ua = tmpList.get(selectedIndex).ua;
                            r2 = ua.resourceUsage(u, gs.getPhysicalGameState());
                            if (!pa2.getResourceUsage().consistentWith(r2, gs)) {
                                do{
                                    tmpList.remove(selectedIndex);
                                    dist.remove(selectedIndex);
                                    selectedIndex = Sampler.eGreedy(dist,EPSILON);
                                    ua = tmpList.get(selectedIndex).ua;
                                    r2 = ua.resourceUsage(u, gs.getPhysicalGameState());
                                }while(!pa2.getResourceUsage().consistentWith(r2, gs));
                            }
                            pa2.getResourceUsage().merge(r2);
                            pa2.addUnitAction(u, ua);


                        } catch(Exception e) {
                            e.printStackTrace();
                        }

                    }


                    try {
                        UnitAction ua =selectedUa;
                        if (ua.resourceUsage(u, pgs).consistentWith(pa.getResourceUsage(), gs)) {
                            ResourceUsage ru = ua.resourceUsage(u, pgs);
                            pa.getResourceUsage().merge(ru);
                            pa.addUnitAction(u, ua);
                        } else {
                            pa.addUnitAction(u, none);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        pa.addUnitAction(u, none);
                    }
                }
            }
        }

        return pa;
    }

    private double gibbsSampling(ActionQvalue aqv, List<ActionQvalue> aqvList){
        double probability = 0;

        double expTotal = 0;
        for(ActionQvalue aq : aqvList){
            expTotal +=  Math.exp(aq.qvalue);
        }

        probability = Math.exp(aqv.qvalue) / expTotal;
        return  probability;
    }
    private boolean checkPostGameState(Unit unit,UnitAction unitAction,GameState gs){
        return  false;
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
