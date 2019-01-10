package rts;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.units.Unit;
import util.Pair;

/**
 * Enumerates the PlayerActions for a given game state
 * @author santi
 */
public class PlayerActionGenerator {
    static Random r = new Random();
    
    GameState gameState;
    PhysicalGameState physicalGameState;
    ResourceUsage base_ru;
    List<Pair<Unit,List<UnitAction>>> choices;
	List<Unit> enemyUnits;
    PlayerAction lastAction = null;
    long size = 1;  // this will be capped at Long.MAX_VALUE;
    long generated = 0;
    int choiceSizes[] = null;
    int currentChoice[] = null;
    boolean moreActions = true;
    
    
    /**
     * 
     * @return
     */
    public long getGenerated() {
        return generated;
    }
    
    public long getSize() {
        return size;
    }
    
    public PlayerAction getLastAction() {
        return lastAction;
    }
    
    public List<Pair<Unit,List<UnitAction>>> getChoices() {
        return choices;
    }
    
    public int[] getChoiceSizes() {
    	return choiceSizes;
    }
    
    public int[] getCurrentChoice() {
    	return currentChoice;
    }
        

    /**
     * Generating all possible actions for a player in a given state
     * @param a_gs
     * @param pID
     * @throws Exception
     */
    public PlayerActionGenerator(GameState a_gs, int pID) throws Exception {
        // Generate the reserved resources:
        base_ru = new ResourceUsage();
        gameState = a_gs;
        physicalGameState = gameState.getPhysicalGameState();
        
		for (Unit u : physicalGameState.getUnits()) {
			UnitActionAssignment uaa = gameState.unitActions.get(u);
			if (uaa != null) {
				ResourceUsage ru = uaa.action.resourceUsage(u, physicalGameState);
				base_ru.merge(ru);
			}
		}
		enemyUnits = new ArrayList<>();
        choices = new ArrayList<>();
        int enemyID = pID ==1?0:1;
		for (Unit u : physicalGameState.getUnits()) {
			if (u.getPlayer() == pID) {
				if (gameState.unitActions.get(u) == null) {
					List<UnitAction> l = u.getUnitActions(gameState);
					choices.add(new Pair<>(u, l));
					// make sure we don't overflow:
					long tmp = l.size();
					if (Long.MAX_VALUE / size <= tmp) {
						size = Long.MAX_VALUE;
					} else {
						size *= (long) l.size();
					}
					// System.out.println("size = " + size);
				}
			}
			else if( u.getPlayer() == enemyID )
				enemyUnits.add(u);
		}
		// System.out.println("---");

		if (choices.size() == 0) {
			System.err.println("Problematic game state:");
			System.err.println(a_gs);
			throw new Exception(
				"Move generator for player " + pID + " created with no units that can execute actions! (status: "
						+ a_gs.canExecuteAnyAction(0) + ", " + a_gs.canExecuteAnyAction(1) + ")"
			);
		}

        choiceSizes = new int[choices.size()];
        currentChoice = new int[choices.size()];
        int i = 0;
        for(Pair<Unit,List<UnitAction>> choice:choices) {
            choiceSizes[i] = choice.m_b.size();
            currentChoice[i] = 0;
            i++;
        }
    } 
    
    /**
     * Shuffles the list of choices
     */
    public void randomizeOrder() {
		for (Pair<Unit, List<UnitAction>> choice : choices) {
			List<UnitAction> tmp = new LinkedList<>();
			tmp.addAll(choice.m_b);
			choice.m_b.clear();
			while (!tmp.isEmpty())
				choice.m_b.add(tmp.remove(r.nextInt(tmp.size())));
		}
	}

	public void DangerBasedOrder() {
    	int i = 0;
    	List<Integer> indeces = new ArrayList<>();

		List<Pair<Unit, List<UnitAction>>> tmp1 = new ArrayList<>();
		List<Pair<Unit, List<UnitAction>>> tmp2 = new ArrayList<>();
		List<Pair<Unit, List<UnitAction>>> tmp3 = new ArrayList<>();

		boolean isRes;

		for (Pair<Unit, List<UnitAction>> choice : choices) {
			isRes = false;

			if(choice.m_a.getResources() == 1)
			{
				isRes = true;
			}

			int x = choice.m_a.getX();
			int y = choice.m_a.getY();
			for (Unit enemyUnit : enemyUnits) {
				if (checkNeighbour(enemyUnit.getX(), enemyUnit.getY(), x, y)){
					if(isRes){
						tmp1.add(choice);
						indeces.add(i);
						isRes = false;
					}else {
					tmp2.add(choice);
						indeces.add(i);
						isRes = false;
					}
					break;
				}
			}
			if(isRes){
				tmp3.add(choice);
				indeces.add(i);
			}
			i++;
		}

		int correction = 0;
		for(int index : indeces){
			choices.remove(index - correction);
			correction++;
		}
		tmp1.addAll(tmp2);
		tmp1.addAll(tmp3);
		tmp1.addAll(choices);
		choices = tmp1;
	}

	boolean checkNeighbour(int x, int y, int otherX, int otherY){
    	if(x-1 == otherX && y-1 == otherY)
    		return true;
		if(x-1 == otherX && y == otherY)
			return true;
		if(x == otherX && y-1 == otherY)
			return true;
		if(x == otherX && y == otherY)
			return true;

		return false;
	}

    
    /**
     * Increases the index that tracks the next action to be returned
     * by {@link #getNextAction(long)}
     * @param startPosition
     */
    public void incrementCurrentChoice(int startPosition) {
		for (int i = 0; i < startPosition; i++)
			currentChoice[i] = 0;
		
		currentChoice[startPosition]++;
		if (currentChoice[startPosition] >= choiceSizes[startPosition]) {
			if (startPosition < currentChoice.length - 1) {
				incrementCurrentChoice(startPosition + 1);
			} else {
				moreActions = false;
			}
		}
    }

    /**
     * Returns the next PlayerAction for the state stored in this object
     * @param cutOffTime time to stop generationg the action
     * @return
     * @throws Exception
     */
    public PlayerAction getNextAction(long cutOffTime) throws Exception {
        int count = 0;
        while(moreActions) {
            boolean consistent = true;
            PlayerAction pa = new PlayerAction();
            pa.setResourceUsage(base_ru.clone());
            int i = choices.size();
			if (i == 0)
				throw new Exception("Move generator created with no units that can execute actions!");
			
			while (i > 0) {
				i--;
				Pair<Unit, List<UnitAction>> unitChoices = choices.get(i);
				int choice = currentChoice[i];
				Unit u = unitChoices.m_a;
				UnitAction ua = unitChoices.m_b.get(choice);

				ResourceUsage r2 = ua.resourceUsage(u, physicalGameState);

				if (pa.getResourceUsage().consistentWith(r2, gameState)) {
					pa.getResourceUsage().merge(r2);
					pa.addUnitAction(u, ua);
				} else {
					consistent = false;
					break;
				}
			}

			incrementCurrentChoice(i);
			if (consistent) {
				lastAction = pa;
				generated++;
				return pa;
			}
            
            // check if we are over time (only check once every 1000 actions, since currenttimeMillis is a slow call):
			if (cutOffTime > 0 && (count % 1000 == 0) && System.currentTimeMillis() > cutOffTime) {
				lastAction = null;
				return null;
			}
			count++;
        }
        lastAction = null;
        return null;
    }
    public class childInfo {
    	public int actionIdx;
    	public boolean newChild;
    	public childInfo(boolean newChild, int actionIdx) {
    		this.newChild = newChild;
    		this.actionIdx = actionIdx;
    	}
    }
    //search for a new valid action for a unit. If there is no available a null move is rendered.
    //returns with a bool variable indicating whether we need to add a new child
    public childInfo updateAction(int unitIdx, int actionIdx) {
    	boolean consistent = false;
    	//If this is the first unit action to be added, we initialize lastAction with the game state of root
    	if ( unitIdx == 0) {
    		lastAction = new PlayerAction();
    		lastAction.setResourceUsage(base_ru.clone());
    	}
    	boolean first = actionIdx == 0;
    	Unit u = choices.get(unitIdx).m_a;
    	while (choiceSizes[unitIdx]>actionIdx) {
    		Pair<Unit, List<UnitAction>> unitChoices = choices.get(unitIdx);
			int choice = actionIdx;
			u = unitChoices.m_a;
			UnitAction ua = unitChoices.m_b.get(choice);

			ResourceUsage r2 = ua.resourceUsage(u, physicalGameState);
			actionIdx++;
			if (lastAction.getResourceUsage().consistentWith(r2, gameState)) {
				lastAction.getResourceUsage().merge(r2);
				lastAction.addUnitAction(u, ua);
				consistent = true;
				break;
			}
    	}
    	if (!consistent)
    	{
    		if (first) {
    			actionIdx++;
        		lastAction.addUnitAction(u, new UnitAction(UnitAction.TYPE_NONE, 1));
        		//We create node with null move only if there is no valid action for the unit
        		return new childInfo(true, actionIdx);
    		}
    		return new childInfo(false, actionIdx);
    	}
    	return new childInfo(true, actionIdx);
    }
    
    public void addToLastAction(int unitIdx, int actionIdx) {
    	Pair<Unit, List<UnitAction>> unitChoices = choices.get(unitIdx);
    	Unit u = unitChoices.m_a;
    	UnitAction ua;
    	//If the actionIdx is bigger than the last action idx by one, it is a null action
    	if (actionIdx == choiceSizes[unitIdx])
    		ua = new UnitAction(UnitAction.TYPE_NONE, 1);
    	else
    		ua = unitChoices.m_b.get(actionIdx);
    	lastAction.addUnitAction(u, ua);
    }
    
    
    /**
     * Returns a random player action for the game state in this object
     * @return
     */
    public PlayerAction getRandom() {
		Random r = new Random();
		PlayerAction pa = new PlayerAction();
		pa.setResourceUsage(base_ru.clone());
		for (Pair<Unit, List<UnitAction>> unitChoices : choices) {
			List<UnitAction> l = new LinkedList<UnitAction>();
			l.addAll(unitChoices.m_b);
			Unit u = unitChoices.m_a;

			boolean consistent = false;
			do {
				UnitAction ua = l.remove(r.nextInt(l.size()));
				ResourceUsage r2 = ua.resourceUsage(u, physicalGameState);

				if (pa.getResourceUsage().consistentWith(r2, gameState)) {
					pa.getResourceUsage().merge(r2);
					pa.addUnitAction(u, ua);
					consistent = true;
				}
			} while (!consistent);
		}
		return pa;
    }
    
    /**
     * Finds the index of a given PlayerAction within the list of PlayerActions
     * @param a
     * @return
     */
	public long getActionIndex(PlayerAction a) {
		int choice[] = new int[choices.size()];
		for (Pair<Unit, UnitAction> ua : a.actions) {
			int idx = 0;
			Pair<Unit, List<UnitAction>> ua_choice = null;
			for (Pair<Unit, List<UnitAction>> c : choices) {
				if (ua.m_a == c.m_a) {
					ua_choice = c;
					break;
				}
				idx++;
			}
			if (ua_choice == null)
				return -1;
			choice[idx] = ua_choice.m_b.indexOf(ua.m_b);

		}
		long index = 0;
		long multiplier = 1;
		for (int i = 0; i < choice.length; i++) {
			index += choice[i] * multiplier;
			multiplier *= choiceSizes[i];
		}
		return index;
	}
    
    
    public String toString() {
        String ret = "PlayerActionGenerator:\n";
        for(Pair<Unit,List<UnitAction>> choice:choices) {
            ret = ret + "  (" + choice.m_a + "," + choice.m_b.size() + ")\n";
        }
        ret += "currentChoice: ";
        for(int i = 0;i<currentChoice.length;i++) {
            ret += currentChoice[i] + " ";
        }
        ret += "\nactions generated so far: " + generated;
        return ret;
    }
    
}
