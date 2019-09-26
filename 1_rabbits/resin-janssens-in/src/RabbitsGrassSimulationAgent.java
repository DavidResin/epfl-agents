import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {
	private int x;
	private int y;
	private int vX;
	private int vY;
	private int energy;
	private static int IDNumber = 0;
	private int ID;
	private RabbitsGrassSimulationSpace rgsSpace;
	
	public RabbitsGrassSimulationAgent(int lifespan) {
		x = -1;
		y = -1;
		energy = lifespan;
		setVxVy();
		IDNumber++;
		ID = IDNumber;
	}
	
	private void setVxVy() {
		vX = 0;
		vY = 0;
		
		while (!(vX == 0 ^ vY == 0)) {
			vX = (int) Math.floor(Math.random() * 3) - 1;
			vY = (int) Math.floor(Math.random() * 3) - 1;
		}
	}
	
	public void setXY(int newX, int newY) {
		x = newX;
		y = newY;
	}
	
	public void setRabbitsGrassSimulationSpace(RabbitsGrassSimulationSpace rgss) {
		rgsSpace = rgss;
	}

	public String getID() {
		return "A-" + ID;
	}
	
	public int getEnergy() {
		return energy;
	}

	public void setEnergy(int energy) {
		this.energy = energy;
	}
	
	public void report() {
		System.out.println(getID() + " at " + x + ", " + y + " has " + getEnergy() + " units of energy.");
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}
	
	public void draw(SimGraphics G) {
		if (energy > 10)
			G.drawFastRoundRect(Color.white);
		else
			G.drawFastRoundRect(Color.red);
	}
	
	public void step() {
		int newX = x + vX;
		int newY = y + vY;
		
		Object2DGrid grid = rgsSpace.getCurrentAgentSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();
		
		if (tryMove(newX, newY))
			energy += rgsSpace.takeGrassAt(x, y);
		else {
			RabbitsGrassSimulationAgent rgsa = rgsSpace.getAgentAt(newX, newY);
			setVxVy();
		}

		energy--;
	}
	
	private boolean tryMove(int newX, int newY) {
		return rgsSpace.moveAgentAt(x, y, newX, newY);
	}
	
	public void receiveEnergy(int amount) {
		energy += amount;
	}
}
