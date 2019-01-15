package ai.mcts.uct;

import ai.RandomBiasedAI;
import ai.core.AI;
import jdk.internal.cmm.SystemResourcePressureImpl;
import rts.*;
import rts.units.Unit;
import util.Pair;
import util.Sampler;

import java.util.*;

public class MASTStrategy2 {
    public static double DEFAULT_Q_VALUE = 1;

    List<HashMap<Long,List<ActionProbability>>> actionProbabilityPerUnitMapList;
    AI randomAI = new RandomBiasedAI();

    public MASTStrategy2(){
        actionProbabilityPerUnitMapList =new ArrayList<>();
        //player 0
        actionProbabilityPerUnitMapList.add(new HashMap<>());
        //player 1
        actionProbabilityPerUnitMapList.add(new HashMap<>());

    }

    public void startNewComputation(){

    }
    public void simulate(GameState gs, int time){
        boolean gameover = false;

        do{
            //try {
            if (gs.isComplete()) {
                gameover = gs.cycle();

            } else {

                //UNABLE TO GET ACTION ???
                //GET PLAYER_ACTION REQUIRED <UNIT,UNIT_ACTION>
                PlayerAction player0Action = getAction(0, gs);
                PlayerAction player1Action = getAction(1, gs);

                gs.issue(player0Action);
                gs.issue(player1Action);
                System.out.println("Im cycling");
            }
            /*}catch(Exception e){
                e.printStackTrace();
                System.out.println("debugging");
            }*/
        }while(!gameover && gs.getTime()<time);
    }

    public void updateHistoryQValues(int player, UCTNode leaf){
        HashMap<Long,List<ActionProbability>> actionProbabilityPerUnitMap= actionProbabilityPerUnitMapList.get(player);
        if(actionProbabilityPerUnitMap.isEmpty())
            return;
        try {
            while (leaf != null) {
                for (PlayerAction playerAction : leaf.actions) {
                    List<Pair<Unit, UnitAction>> actions = playerAction.getActions();
                    double exploitationValue = leaf.getExploitationValue(leaf);
                    for (Pair<Unit, UnitAction> pair : actions) {
                        Unit u = pair.m_a;
                        UnitAction ua = pair.m_b;
                        if (!actionProbabilityPerUnitMap.containsKey(u.getID())) {
                            //throw new Exception("action proba per unit map does not contain unit!");
                            List<ActionProbability> actionProbabilityList = new ArrayList<>();
                            actionProbabilityList.add(new ActionProbability(ua,DEFAULT_Q_VALUE));
                            actionProbabilityPerUnitMap.put(u.getID(),actionProbabilityList);
                        }
                        else {

                            List<ActionProbability> actionProbabilityList = actionProbabilityPerUnitMap.get(u.getID());
                            for (ActionProbability ap : actionProbabilityList) {
                                if (ap.ua.equals(ua)) {
                                    if(ap.isFirstTime) {
                                        ap.isFirstTime = false;
                                        ap.probability = 0d;
                                    }

                                    ap.probability += exploitationValue;
                                    ap.probability = Math.max(0, Math.min(DEFAULT_Q_VALUE, ap.probability));
                                }
                            }
                        }
                    }
                }
                leaf = leaf.parent;
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
    public PlayerAction getAction(int player, GameState gs) {
        HashMap<Long,List<ActionProbability>> actionProbabilityPerUnitMap= actionProbabilityPerUnitMapList.get(player);
        PlayerAction pa = new PlayerAction();
        PhysicalGameState pgs = gs.getPhysicalGameState();
        if (!gs.canExecuteAnyAction(player)) return pa;

        System.out.println("Im in getAction of player"+player);
        // Generate the reserved resources:
        for(Unit u:pgs.getUnits()) {
            UnitActionAssignment uaa = gs.getActionAssignment(u);
            if (uaa!=null) {
                ResourceUsage ru = uaa.action.resourceUsage(u, pgs);
                pa.getResourceUsage().merge(ru);
            }
        }

        String s="\nplayer"+player+"\n";
        for(Unit u:pgs.getUnits()) {
            if (u.getPlayer()==player) {
                if (gs.getActionAssignment(u)==null) {
                    s+= "unitId:"+u.getID()+"\t|"+u.getX()+","+u.getY()+"\t|"+u.getType().name;
                    UnitAction none = new UnitAction(UnitAction.TYPE_NONE, 10);
                    List<UnitAction> l = u.getUnitActions(gs);

                    s+= "\n\tListActions:";
                    for(UnitAction ua:l) {
                        s += ua.toString() + "\t";
                    }
                    s+= "\n\tAvaiActions:";

                    for(UnitAction a:l) {
                        if (a.getType()==UnitAction.TYPE_NONE) {
                            none = a;
                            break;
                        }
                    }
                    List<ActionProbability> actionProbList = null;
                    if(actionProbabilityPerUnitMap.containsKey(u.getID())){
                        actionProbList = actionProbabilityPerUnitMap.get(u.getID());
                        //add new found actions
                        for(UnitAction unitAction: l){
                            if(!IsActionUnitContainedInList(actionProbList,unitAction)
                            ) {
                                ActionProbability ap = new ActionProbability(unitAction, DEFAULT_Q_VALUE);
                                actionProbList.add(ap);
                            }
                        }
                    }
                    else{
                        //new unit
                        //try to add all possible actions which this unit can perform
                        actionProbList = new ArrayList<>();
                        for(UnitAction unitAction: l){
                            ActionProbability ap = new ActionProbability(unitAction,DEFAULT_Q_VALUE);
                            actionProbList.add(ap);
                        }
                        actionProbabilityPerUnitMap.put(u.getID(),actionProbList);
                    }
                    //try to get action by probs
                    int i=0;

                    /*double[] distribution = new double[actionProbList.size()];
                    for(ActionProbability ap: actionProbList){
                        if(l.contains(ap.ua))
                            distribution[i] = ap.probability; // allowed, so use available prob
                        else
                            distribution[i] = 0; // impossible action in this game state
                        i++;
                    }*/
                    List<ActionProbability> tmpList= new ArrayList<>();
                    for(ActionProbability ap: actionProbList){
                        if(l.contains(ap.ua)&&gs.isUnitActionAllowed(u,ap.ua)) {
                            if(checkPostGameState(pa,u,ap.ua,gs)) {
                                i++; // allowed, so use available prob
                                tmpList.add(ap);
                            }
                        }
                    }
                    double[] distribution =new double[i];
                    i=0;
                    for(ActionProbability ap:tmpList){
                        distribution[i] = ap.probability;
                        s+= ap.ua.toString()+"\t";
                        i++;
                    }
                    s+="\n";

                    try{
                        //extract to player action
                        ActionProbability selectedAp = null;
                        if(distribution.length == 1 && distribution[0] ==0.0){
                            selectedAp = new ActionProbability(none,1);
                        }else {
                            //selectedAp = actionProbList.get(Sampler.weighted(distribution));
                            selectedAp =  tmpList.get(Sampler.weighted(distribution));
                        }
                        UnitAction ua = selectedAp.ua;
                        if (ua.resourceUsage(u, pgs).consistentWith(pa.getResourceUsage(), gs)) {
                            ResourceUsage ru = ua.resourceUsage(u, pgs);
                            pa.getResourceUsage().merge(ru);
                            pa.addUnitAction(u, ua);
                        } else {
                            pa.addUnitAction(u, none);
                        }
                        s +="\t|chosenAction:"+ua.toString()+"\t|highestProb:"+selectedAp.probability+"\n";

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.out.println("unitId"+u.getID()+" performs null move");
                        pa.addUnitAction(u, none);
                    }


                }
            }
        }
        System.out.println(s);
        System.out.println("finish action");
        return  pa;
    }
    private boolean checkPostGameState(PlayerAction pa,Unit u,UnitAction ua,GameState gs){
        PlayerAction pa2 = pa.clone();
        GameState checkingGameState = gs.clone();
        checkingGameState.issue(pa2);
        checkingGameState.cycle();
        System.out.println("PA: "+pa2.toString());
        boolean  ret = false;
        PhysicalGameState pgs = checkingGameState.getPhysicalGameState();

        System.out.println("Position of all units ");
        for(Unit check:    checkingGameState.getUnits())
        {
            System.out.println(check.toString());
        }
        ret = checkingGameState.isUnitActionAllowed(u,ua);
        System.out.println("Result of checking unit"+u.getID()+" do action "+ua.toString()+": "+ret);
        return ret;
    }

    private boolean IsActionUnitContainedInList(List<ActionProbability> actionProbList,UnitAction ua){
        if(ua.getType() == UnitAction.TYPE_PRODUCE)
            return  true;
        for(ActionProbability ap:actionProbList){
            if(ap.equalsUnitAction(ua) )
                return  true;
        }
        return  false;
    }
    //only ref unit action and probability of it per a unit
    private class ActionProbability{
        public UnitAction ua;
        public double probability;
        public boolean isFirstTime;

        public ActionProbability(UnitAction ua, double probability){
            this.ua = ua;
            this.probability= probability;
            isFirstTime = true;
        }
        public boolean equalsUnitAction(UnitAction checkedUa){
            return ua.equals(checkedUa);
        }
    }
}
