import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {
	private Object2DGrid rgsSpace;
	private Object2DGrid agentSpace;
	private int energyFactor;
	
	public RabbitsGrassSimulationSpace(int gridSize, int energyFactor) {
		rgsSpace = new Object2DGrid(gridSize, gridSize);
		agentSpace = new Object2DGrid(gridSize, gridSize);
		this.energyFactor = energyFactor;
		
		for (int i = 0; i < gridSize; i++) {
			for (int j = 0; j < gridSize; j++) {
				rgsSpace.putObjectAt(i, j, 0);
			}
		}
	}
	
	public void spreadGrass(int grass) {
		for (int i = 0; i < grass; i++) {
			int x = (int) (Math.random() * rgsSpace.getSizeX());
			int y = (int) (Math.random() * rgsSpace.getSizeY());
			
			int currentValue = getGrassAt(x, y);
			rgsSpace.putObjectAt(x, y, currentValue + 1);
		}
	}
	
	public int getGrassAt(int x, int y) {
		int i;
		
		if (rgsSpace.getObjectAt(x, y) != null)
			i = ((Integer) rgsSpace.getObjectAt(x, y)).intValue();
		else
			i = 0;
		
		return i * energyFactor;
	}
	
	public RabbitsGrassSimulationAgent getAgentAt(int x, int y) {
		RabbitsGrassSimulationAgent retVal = null;
		
		if (agentSpace.getObjectAt(x, y) != null)
			retVal = (RabbitsGrassSimulationAgent) agentSpace.getObjectAt(x, y);
		
		return retVal;
	}
	
	public Object2DGrid getCurrentRGSSpace() {
		return rgsSpace;
	}
	
	public Object2DGrid getCurrentAgentSpace() {
		return agentSpace;
	}
	
	public boolean isCellOccupied(int x, int y) {
		boolean retVal = false;
	
		if (agentSpace.getObjectAt(x, y) != null)
			retVal = true;
		
		return retVal;
	}
	
	public boolean addAgent(RabbitsGrassSimulationAgent agent) {
		boolean retVal = false;
		int count = 0;
		int countLimit = 10 * agentSpace.getSizeX() * agentSpace.getSizeY();
		
		while ((retVal == false) && (count < countLimit)) {
			int x = (int) (Math.random() * agentSpace.getSizeX());
			int y = (int) (Math.random() * agentSpace.getSizeY());
			
			if (isCellOccupied(x, y) == false) {
				agentSpace.putObjectAt(x, y, agent);
				agent.setXY(x, y);
				agent.setRabbitsGrassSimulationSpace(this);
				retVal = true;
			}
			
			count++;
		}
		
		return retVal;
	}
	
	public void removeAgentAt(int x, int y) {
		agentSpace.putObjectAt(x, y, null);
	}
	
	public int takeGrassAt(int x, int y) {
		int grass = getGrassAt(x, y);
		rgsSpace.putObjectAt(x, y, 0);
		return grass;
	}
	
	public boolean moveAgentAt(int x, int y, int newX, int newY) {
		boolean retVal = false;
		
		if (!isCellOccupied(newX, newY)) {
			RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) agentSpace.getObjectAt(x, y);
			removeAgentAt(x, y);
			rgsa.setXY(newX,  newY);
			agentSpace.putObjectAt(newX, newY, rgsa);
			retVal = true;
		}
		
		return retVal;
	}
	
	public int getTotalGrass() {
		int totalGrass = 0;
		
		for (int i = 0; i < agentSpace.getSizeX(); i++) {
			for (int j = 0; j < agentSpace.getSizeY(); j++) {
				totalGrass += getGrassAt(i, j);
			}
		}
		
		return totalGrass;
	}
}
