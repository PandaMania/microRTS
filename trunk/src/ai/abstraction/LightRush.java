/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.abstraction;

import ai.AI;
import ai.abstraction.AbstractionLayerAI;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.*;
import util.Pair;

/**
 *
 * @author santi
 */
public class LightRush extends AbstractionLayerAI {
    Random r = new Random();

    // If we have any "light": send it to attack to the nearest enemy unit
    // If we have a base: train worker until we have 1 workers
    // If we have a barracks: train light
    // If we have a worker: do this if needed: build base, build barracks, harvest resources
    
    public void reset() {
    }
    
    public AI clone() {
        return new LightRush();
    }
    
    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        PlayerAction pa = new PlayerAction();
//        System.out.println("LightRushAI for player " + player + " (cycle " + gs.getTime() + ")");
                
        // behavior of bases:
        for(Unit u:pgs.getUnits()) {
            if ((u instanceof Base) && 
                u.getPlayer() == player && 
                gs.getActionAssignment(u)==null) {
                baseBehavior(u,p,pgs);
            }
        }
        
        // behavior of barracks:
        for(Unit u:pgs.getUnits()) {
            if ((u instanceof Barracks) && 
                u.getPlayer() == player && 
                gs.getActionAssignment(u)==null) {
                barracksBehavior(u,p,pgs);
            }
        }
        
        // behavior of melee units:
        for(Unit u:pgs.getUnits()) {
            if (((u instanceof Light) || (u instanceof Heavy)) && 
                u.getPlayer() == player && 
                gs.getActionAssignment(u)==null) {
                meleeUnitBehavior(u,p,pgs);
            }        
        }
        
        // behavior of workers:
        List<Unit> workers = new LinkedList<Unit>();
        for(Unit u:pgs.getUnits()) {
            if ((u instanceof Worker) && 
                u.getPlayer() == player) {
                workers.add(u);
            }        
        }
        workersBehavior(workers,p,pgs);
        
                
        return translateActions(player,gs);
    }
    
    
    public void baseBehavior(Unit u,Player p, PhysicalGameState pgs) {
        int nworkers = 0;
        for(Unit u2:pgs.getUnits()) {
            if ((u2 instanceof Worker) && 
                u2.getPlayer() == p.getID()) nworkers++;
        }
        if (nworkers<1 && p.getResources()>Worker.WORKER_COST) train(u, Unit.WORKER);
    }

    public void barracksBehavior(Unit u,Player p, PhysicalGameState pgs) {
        if (p.getResources()>Light.LIGHT_COST) train(u, Unit.LIGHT);
    }
    
    public void meleeUnitBehavior(Unit u,Player p, PhysicalGameState pgs) {
        Unit closestEnemy = null;
        int closestDistance = 0;
        for(Unit u2:pgs.getUnits()) {
            if (u2.getPlayer()>=0 && u2.getPlayer()!=p.getID()) { 
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy==null || d<closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy!=null) {
//            System.out.println("LightRushAI.meleeUnitBehavior: " + u + " attacks " + closestEnemy);
            attack(u,closestEnemy);
        }
    }
    
    public void workersBehavior(List<Unit> workers,Player p, PhysicalGameState pgs) {
        int nbases = 0;
        int nbarracks = 0;
        
        int resourcesUsed = 0;
        List<Unit> freeWorkers = new LinkedList<Unit>();
        freeWorkers.addAll(workers);
        
        if (workers.isEmpty()) return;
        
        for(Unit u2:pgs.getUnits()) {
            if ((u2 instanceof Base) && 
                u2.getPlayer() == p.getID()) nbases++;
            if ((u2 instanceof Barracks) && 
                u2.getPlayer() == p.getID()) nbarracks++;
        }
        
        List<Integer> reservedPositions = new LinkedList<Integer>();
        if (nbases==0 && !freeWorkers.isEmpty()) {
            // build a base:
            if (p.getResources()>Base.BASE_COST + resourcesUsed) {
                Unit u = freeWorkers.remove(0);
                int pos = findBuildingPosition(reservedPositions,u,p,pgs);
                build(u, Unit.BASE, pos%pgs.getWidth(), pos/pgs.getWidth());
                resourcesUsed+=Base.BASE_COST;
                reservedPositions.add(pos);
            }
        }
        
        if (nbarracks==0) {
            // build a barracks:
            if (p.getResources()>Barracks.BARRACKS_COST + resourcesUsed) {
                Unit u = freeWorkers.remove(0);
                int pos = findBuildingPosition(reservedPositions,u,p,pgs);
                build(u, Unit.BARRACKS, pos%pgs.getWidth(), pos/pgs.getWidth());
                resourcesUsed+=Base.BASE_COST;
            }
        }
        
        
        // harvest with all the free workers:
        for(Unit u:freeWorkers) {
            Unit closestBase = null;
            Unit closestResource = null;
            int closestDistance = 0;
            for(Unit u2:pgs.getUnits()) {
                if (u2 instanceof Resource) { 
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestResource==null || d<closestDistance) {
                        closestResource = u2;
                        closestDistance = d;
                    }
                }
            }
            closestDistance = 0;
            for(Unit u2:pgs.getUnits()) {
                if (u2 instanceof Base) { 
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestBase==null || d<closestDistance) {
                        closestBase = u2;
                        closestDistance = d;
                    }
                }
            }
            if (closestResource!=null && closestBase!=null) harvest(u, closestResource, closestBase);
        }
    }
    
    public int findBuildingPosition(List<Integer> reserved, Unit u, Player p, PhysicalGameState pgs) {
        int bestPos = -1;
        int bestScore = 0;
        
        for(int x = 0;x<pgs.getWidth();x++) {
            for(int y = 0;y<pgs.getHeight();y++) {
                int pos = x+y*pgs.getWidth();
                if (!reserved.contains(pos) && pgs.getUnitAt(x, y)==null) {
                    int score = 0;
                    
                    score = - (Math.abs(u.getX()-x) + Math.abs(u.getY()-y));
                    
                    if (bestPos==-1 || score>bestScore) {
                        bestPos = pos;
                        bestScore = score;
                    }
                }
            }
        }
        
        return bestPos;
    }
    

}
