import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author 
 */

public class RabbitsGrassSimulationModel extends SimModelImpl {
		private static final int GRIDSIZE = 20;
		private static final int NUMINITRABBITS = 10;
		private static final int NUMINITGRASS = 10;
		private static final int GRASSGROWTHRATE = 50;
		private static final int BIRTHTHRESHOLD = 300;
		private static final int ENERGYFACTOR = 1;
		private static final int LIFESPAN = 40;
				
		private int gridSize = GRIDSIZE;
		private int numInitRabbits = NUMINITRABBITS;
		private int numInitGrass = NUMINITGRASS;
		private int grassGrowthRate = GRASSGROWTHRATE;
		private int birthThreshold = BIRTHTHRESHOLD;
		private int energyFactor = ENERGYFACTOR;
		private int lifespan = LIFESPAN;
		
		private Schedule schedule;
		private RabbitsGrassSimulationSpace rgsSpace;
		private DisplaySurface displaySurf;
		private ArrayList<RabbitsGrassSimulationAgent> agentList;
		private OpenSequenceGraph amounts;

		class grassInSpace implements DataSource, Sequence {
			public Object execute() {
				return getSValue();
			}
			
			public double getSValue() {
				return (double) rgsSpace.getTotalGrass();
			}
		}
		
		class rabbitsInSpace implements DataSource, Sequence {
			public Object execute() {
				return getSValue();
			}
			
			public double getSValue() {
				return (double) agentList.size();
			}
		}
		
		public static void main(String[] args) {
			
			System.out.println("Rabbit skeleton");

			SimInit init = new SimInit();
			RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
			// Do "not" modify the following lines of parsing arguments
			if (args.length == 0) // by default, you don't use parameter file nor batch mode 
				init.loadModel(model, "", false);
			else
				init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));
			
		}
		
		public void buildModel() {
			System.out.println("Running BuildModel");
			rgsSpace = new RabbitsGrassSimulationSpace(gridSize, energyFactor);
			rgsSpace.spreadGrass(numInitGrass);
			
			for (int i = 0; i < numInitRabbits; i++) {
				addNewAgent();
			}
			
			for (int i = 0; i < agentList.size(); i++) {
				RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) agentList.get(i);
				rgsa.report();
			}
		}
		
		public void buildSchedule() {
			System.out.println("Running BuildSchedule");
			
			class RabbitsGrassSimulationStep extends BasicAction {
				public void execute() {
					SimUtilities.shuffle(agentList);
					
					for (int i = 0; i < agentList.size(); i++) {
						RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) agentList.get(i);
						rgsa.step();
					}
					
					reapDeadAgents();
					displaySurf.updateDisplay();
				}
			}
			
			schedule.scheduleActionBeginning(0, new RabbitsGrassSimulationStep());
			
			class RabbitsGrassSimulationCountLiving extends BasicAction {
				public void execute() {
					countLivingAgents();
				}
			}
			
			schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationCountLiving());
			
			class RabbitsGrassSimulationUpdateGrassInSpace extends BasicAction {
				public void execute() {
					amounts.step();
				}
			}
			
			schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationUpdateGrassInSpace());
			
			class RabbitsGrassSimulationGrow extends BasicAction {
				public void execute() {
					rgsSpace.spreadGrass(grassGrowthRate);
				}
			}
			
			schedule.scheduleActionAtInterval(1, new RabbitsGrassSimulationGrow());
			
			class RabbitsGrassSimulationBirth extends BasicAction {
				public void execute() {
					for (int i = 0; i < agentList.size(); i++) {
						RabbitsGrassSimulationAgent rgsa = agentList.get(i);
						
						if (rgsa.getEnergy() > birthThreshold) {
							addNewAgent();
							rgsa.setEnergy(rgsa.getEnergy() / 2);
						}
					}
				}
			}
			
			schedule.scheduleActionAtInterval(1, new RabbitsGrassSimulationBirth());
		}
		
		public void buildDisplay() {
			System.out.println("Running BuildDisplay");
			
			ColorMap map = new ColorMap();
			
			for (int i = 1; i < 16; i++) {
				map.mapColor(i,  new Color((int) 0, Math.min(255, i * 8 + 127), 0));
			}
			
			map.mapColor(0, Color.black);
			
			Value2DDisplay displayGrass = new Value2DDisplay(rgsSpace.getCurrentRGSSpace(), map);
			Object2DDisplay displayAgents = new Object2DDisplay(rgsSpace.getCurrentAgentSpace());
					
			displayAgents.setObjectList(agentList);
			
			displaySurf.addDisplayableProbeable(displayGrass, "Grass");
			displaySurf.addDisplayableProbeable(displayAgents, "Agents");
			
			amounts.addSequence("Grass In Space",  new grassInSpace());
			amounts.addSequence("Rabbits in space", new rabbitsInSpace());
			
		}
		
		private void addNewAgent() {
			RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(lifespan);
			agentList.add(a);
			rgsSpace.addAgent(a);
		}
		
		private int reapDeadAgents() {
			int count = 0;
			
			for (int i = agentList.size() - 1; i >= 0; i--) {
				RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) agentList.get(i);
				
				if (rgsa.getEnergy() < 1) {
					rgsSpace.removeAgentAt(rgsa.getX(), rgsa.getY());
					agentList.remove(i);
					count++;
				}
			}
			
			return count;
		}
		
		private int countLivingAgents() {
			int livingAgents = 0;
			
			for (int i = 0; i < agentList.size(); i++) {
				RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) agentList.get(i);
				
				if (rgsa.getEnergy() > 0)
					livingAgents++;
			}
			
			System.out.println("Number of living agents is: " + livingAgents);
			
			return livingAgents;
		}

		public void begin() {
			buildModel();
			buildSchedule();
			buildDisplay();
			
			displaySurf.display();
			amounts.display();
		}

		public String[] getInitParam() {
			// Parameters to be set by users via the Repast UI slider bar
			// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
			String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "EnergyFactor", "Lifespan" };
			return params;
		}

		public int getGridSize() {
			return gridSize;
		}

		public void setGridSize(int gridSize) {
			this.gridSize = gridSize;
		}

		public int getNumInitRabbits() {
			return numInitRabbits;
		}

		public void setNumInitRabbits(int numInitRabbits) {
			this.numInitRabbits = numInitRabbits;
		}

		public int getNumInitGrass() {
			return numInitGrass;
		}

		public void setNumInitGrass(int numInitGrass) {
			this.numInitGrass = numInitGrass;
		}

		public int getGrassGrowthRate() {
			return grassGrowthRate;
		}

		public void setGrassGrowthRate(int grassGrowthRate) {
			this.grassGrowthRate = grassGrowthRate;
		}

		public int getBirthThreshold() {
			return birthThreshold;
		}

		public void setBirthThreshold(int birthThreshold) {
			this.birthThreshold = birthThreshold;
		}
		
		public int getLifespan() {
			return lifespan;
		}
		
		public void setLifespan(int lifespan) {
			this.lifespan = lifespan;
		}
		
		public int getEnergyFactor() {
			return energyFactor;
		}
		
		public void setEnergyFactor(int energyFactor) {
			this.energyFactor = energyFactor;
		}
		
		public String getName() {
			return "Rabbit Grass Simulator Model";
		}

		public Schedule getSchedule() {
			return schedule;
		}

		public void setup() {
			System.out.println("Running setup");
			rgsSpace = null;
			agentList = new ArrayList<RabbitsGrassSimulationAgent>();
			schedule = new Schedule(1);
			
			if (displaySurf != null) {
				displaySurf.dispose();
			}
			
			String windowName = "Rabbits Grass Simulation Model Window 1";
			displaySurf = null;
			displaySurf = new DisplaySurface(this, windowName);
			amounts = new OpenSequenceGraph("Amount of Grass and Rabbits In Space", this);
			
			registerDisplaySurface(windowName, displaySurf);
			this.registerMediaProducer("Plot", amounts);
		}
}
