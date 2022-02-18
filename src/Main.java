import scheduler.CScheduler;
import scheduler.CScheduler.CoverageType;
import scheduler.CoverageCalculator;
import scheduler.FIFO_Scheduler;
import scheduler.MCTS_Scheduler;
import scheduler.MCTS_Scheduler.VisionType;
import scheduler.Match;
import scheduler.Match.AllianceType;
import scheduler.Random_Scheduler;
import scheduler.Round_Robin_Scheduler;
import scheduler.Scheduler;
import scheduler.State;
import xml2bdi.XMLReader;
import uno.gpt.generators.*;
import util.Log;

import java.util.ArrayList;
import java.util.Random;

import goalplantree.GoalNode;

public class Main
{
    public static void main(String[] args) throws Exception
    {
    	boolean run_main_experiment = true;
    	boolean run_extra_alpha_experiment = false; // Change to true to run extra alpha experiments (see end of page 7 in the AAMAS paper).
    	
    	Log.log_to_file = false;
    	
    	// If xml_file == null, a random forest will be generated.
    	String xml_file = null; // "forest.xml";
    	
    	int experiment_repetitions = 1000;
    	
    	// MCTS settings
        int mcts_alpha = 100;
        int mcts_beta = 10;
        double mcts_c = 2.0 * Math.sqrt(2.0);
    	
        // Schedulers
        // Note: The 'assumed_politeness_of_other_agent' parameter of the MCTS agents is initially set to 1.0, meaning that
        // they expect the other agent to behave as an ally. However, this setting is overridden for the neutral and adversarial
        // experiments below.
		ArrayList<String> scheduler_names = new ArrayList<String>();
		ArrayList<Scheduler> schedulers = new ArrayList<Scheduler>();
		ArrayList<Scheduler> scheduler_clones = new ArrayList<Scheduler>(); // For mirror matches
		
		// For extra alpha experiments.
		Scheduler extra_alpha_IA = new MCTS_Scheduler(VisionType.FULL, 500, mcts_beta, mcts_c, 1.0, -1.0);
		Scheduler normal_alpha_IA = new MCTS_Scheduler(VisionType.FULL, 100, mcts_beta, mcts_c, 1.0, -1.0);
		
		scheduler_names.add("MCTS_fully_aware");
		schedulers.add(new MCTS_Scheduler(VisionType.FULL, mcts_alpha, mcts_beta, mcts_c, 1.0, 1.0));
		scheduler_clones.add(new MCTS_Scheduler(VisionType.FULL, mcts_alpha, mcts_beta, mcts_c, 1.0, 1.0));

		scheduler_names.add("MCTS_partially_aware");
		schedulers.add(new MCTS_Scheduler(VisionType.PARTIALLY_AWARE, mcts_alpha, mcts_beta, mcts_c, 1.0, 1.0));
		scheduler_clones.add(new MCTS_Scheduler(VisionType.PARTIALLY_AWARE, mcts_alpha, mcts_beta, mcts_c, 1.0, 1.0));

		scheduler_names.add("MCTS_unaware");
		schedulers.add(new MCTS_Scheduler(VisionType.UNAWARE, mcts_alpha, mcts_beta, mcts_c, 1.0, 1.0));
		scheduler_clones.add(new MCTS_Scheduler(VisionType.UNAWARE, mcts_alpha, mcts_beta, mcts_c, 1.0, 1.0));
		
		scheduler_names.add("FIFO");
		schedulers.add(new FIFO_Scheduler());
		scheduler_clones.add(new FIFO_Scheduler());
		
		scheduler_names.add("C0");
		schedulers.add(new CScheduler(CoverageType.C0));
		scheduler_clones.add(new CScheduler(CoverageType.C0));
		
		scheduler_names.add("C1");
		schedulers.add(new CScheduler(CoverageType.C1));
		scheduler_clones.add(new CScheduler(CoverageType.C1));
		
		scheduler_names.add("Random");
		schedulers.add(new Random_Scheduler());
		scheduler_clones.add(new Random_Scheduler());
		
		scheduler_names.add("Round_Robin");
		schedulers.add(new Round_Robin_Scheduler());
		scheduler_clones.add(new Round_Robin_Scheduler());
		
		// Generator settings
    	// Note: The below settings only apply if useRandomXML == true.
    	int depth, numEnvironmentVariables, numGoalPlanTrees, subgoalsPerPlan, plansPerGoal, actionsPerPlan;
    	double propGuaranteedCPrecond, prGoalHasSinglePlan;
    	
		depth = 5;
    	numEnvironmentVariables = 20;
    	numGoalPlanTrees = 12;
    	subgoalsPerPlan = 1; // "Goal branching factor"
    	plansPerGoal = 2; // "Branching factor"
    	actionsPerPlan = 5;
    	propGuaranteedCPrecond = 0.5;
    	prGoalHasSinglePlan = 0.5;	

    	XMLReader reader;
        Random rm = new Random();
    	
        for (int experiment_num = 0; experiment_num < experiment_repetitions; experiment_num++)
        {
        	String generated_filename = "random_" + experiment_num + ".xml";
        	
	    	if (xml_file == null)
	    	{
		    	int randomSeed = rm.nextInt();
		    	
		    	String generatedXmlFilename = Log.getLogDir() + "/" + generated_filename;
		    	
		    	String[] generatorArgs = new String[21];
		    	generatorArgs[0] = "synth"; // Options are "synth", "miconic", "block", "logi"
		    	generatorArgs[1] = "-f";
		    	generatorArgs[2] = generatedXmlFilename;
		    	generatorArgs[3] = "-s";
		    	generatorArgs[4] = Integer.toString(randomSeed);
		    	generatorArgs[5] = "-d";
		    	generatorArgs[6] = Integer.toString(depth);
		    	generatorArgs[7] = "-t";
		    	generatorArgs[8] = Integer.toString(numGoalPlanTrees);
		    	generatorArgs[9] = "-v";
		    	generatorArgs[10] = Integer.toString(numEnvironmentVariables);
		    	generatorArgs[11] = "-g";
		    	generatorArgs[12] = Integer.toString(subgoalsPerPlan);
		    	generatorArgs[13] = "-p";
		    	generatorArgs[14] = Integer.toString(plansPerGoal);
		    	generatorArgs[15] = "-a";
		    	generatorArgs[16] = Integer.toString(actionsPerPlan);
		    	generatorArgs[17] = "-y";
		    	generatorArgs[18] = Double.toString(propGuaranteedCPrecond);
		    	generatorArgs[19] = "-z";
		    	generatorArgs[20] = Double.toString(prGoalHasSinglePlan);

	
		    	XMLGenerator.generate(generatorArgs);
		    	
		    	reader = new XMLReader(generatedXmlFilename);
	    	}
	    	else
	    	{
	            reader = new XMLReader(xml_file);
	    	}
	    	
	    	String forest_name = (xml_file == null)? generated_filename : xml_file;
	    	
	        // Read the initial state from the XML file
	    	State currentState = new State(forest_name, reader.getBeliefs(), reader.getIntentions(), 0);
	        
	        // Calculate the coverage (for C0 and C1)
	        CoverageCalculator cc = new CoverageCalculator();
	        for (int i = 0; i < currentState.intentions.size(); i++)
	        {
	            cc.calG((GoalNode)currentState.intentions.get(i));
	        }
	        
	        if (run_extra_alpha_experiment)
	        {
		        new Match("adversarial_extra_alpha_IA_vs_normal_alpha_IA",
		            	numGoalPlanTrees,
		            	AllianceType.ADVERSARIAL,
		            	currentState.clone(),
		            	new Scheduler[] {extra_alpha_IA, normal_alpha_IA},
		            	new String[] {"extra_alpha_IA", "normal_alpha_IA"},
		            	-1.0).run_two_sided_series(true, true);
	        }
	        
	        if (run_main_experiment)
	        {
	        	// Allied and Neutral experiments.
	        	// As mentioned in the paper (page 5), we try both these experiments with assumed_politeness = 0 and assumed_politeness = 1 to see if it makes much difference (it doesn't).
		        for (int assumed_politeness = 0; assumed_politeness <= 1; assumed_politeness++)
		        {
		        	// Override assumed politeness
			        for (Scheduler sched : schedulers)
			        {
			        	if (sched instanceof MCTS_Scheduler)
			        	{
			        		((MCTS_Scheduler)sched).assumed_politeness_of_other_agent = assumed_politeness;
			        	}
			        }
			        for (Scheduler sched : scheduler_clones)
			        {
			        	if (sched instanceof MCTS_Scheduler)
			        	{
			        		((MCTS_Scheduler)sched).assumed_politeness_of_other_agent = assumed_politeness;
			        	}
			        }
			        
			        // Allied experiments
			        for (int agent_1 = 0; agent_1 < schedulers.size(); agent_1++)
			        {
			        	for (int agent_2 = agent_1; agent_2 < schedulers.size(); agent_2++)
			        	{
			        		// The different assumed politeness settings only need to be tried for I_A schedulers that have non-zero vision of the
			        		// other agent's intentions, since 'MCTS_unaware' doesn't actually model the other agent at all.
			        		if ((assumed_politeness == 0)
			        				|| scheduler_names.get(agent_1).equals("MCTS_fully_aware")
			        				|| scheduler_names.get(agent_1).equals("MCTS_partially_aware")
			        				|| scheduler_names.get(agent_2).equals("MCTS_fully_aware")
			        				|| scheduler_names.get(agent_2).equals("MCTS_partially_aware"))
			        		{
					        	if (agent_2 == agent_1)
					        	{
					        		// Mirror match
							        new Match("allied_" + scheduler_names.get(agent_1) + "_and_" + scheduler_names.get(agent_2),
							            	numGoalPlanTrees,
							            	AllianceType.ALLIED,
							            	currentState.clone(),
							            	new Scheduler[] {schedulers.get(agent_1), scheduler_clones.get(agent_2)},
							            	new String[] {scheduler_names.get(agent_1), scheduler_names.get(agent_2) + "_clone"},
							            	assumed_politeness).run_two_sided_series(true, true);
					        	}
					        	else
					        	{
							        new Match("allied_" + scheduler_names.get(agent_1) + "_and_" + scheduler_names.get(agent_2),
							            	numGoalPlanTrees,
							            	AllianceType.ALLIED,
							            	currentState.clone(),
							            	new Scheduler[] {schedulers.get(agent_1), schedulers.get(agent_2)},
							            	new String[] {scheduler_names.get(agent_1), scheduler_names.get(agent_2)},
							            	assumed_politeness).run_two_sided_series(true, true);
					        	}
			        		}
			        	}
			        }
			        
			        // Neutral experiments
			        for (int agent_1 = 0; agent_1 < schedulers.size(); agent_1++)
			        {
			        	for (int agent_2 = agent_1; agent_2 < schedulers.size(); agent_2++)
			        	{
			        		// The different assumed politeness settings only need to be tried for I_A schedulers that have non-zero vision of the
			        		// other agent's intentions, since 'MCTS_unaware' doesn't actually model the other agent at all.
			        		if ((assumed_politeness == 0)
			        				|| scheduler_names.get(agent_1).equals("MCTS_fully_aware")
			        				|| scheduler_names.get(agent_1).equals("MCTS_partially_aware")
			        				|| scheduler_names.get(agent_2).equals("MCTS_fully_aware")
			        				|| scheduler_names.get(agent_2).equals("MCTS_partially_aware"))
			        		{
					        	if (agent_2 == agent_1)
					        	{
					        		// Mirror match
					        		new Match("neutral_" + scheduler_names.get(agent_1) + "_and_" + scheduler_names.get(agent_2),
							            	numGoalPlanTrees,
							            	AllianceType.NEUTRAL,
							            	currentState.clone(),
							            	new Scheduler[] {schedulers.get(agent_1), scheduler_clones.get(agent_2)},
							            	new String[] {scheduler_names.get(agent_1), scheduler_names.get(agent_2) + "_clone"},
							            	assumed_politeness).run_two_sided_series(true, true);
					        	}
					        	else
					        	{
					        		new Match("neutral_" + scheduler_names.get(agent_1) + "_and_" + scheduler_names.get(agent_2),
							            	numGoalPlanTrees,
							            	AllianceType.NEUTRAL,
							            	currentState.clone(),
							            	new Scheduler[] {schedulers.get(agent_1), schedulers.get(agent_2)},
							            	new String[] {scheduler_names.get(agent_1), scheduler_names.get(agent_2)},
							            	assumed_politeness).run_two_sided_series(true, true);
					        	}
			        		}
			        	}
			        }
		        }
		        
		        // For adversarial experiments, fix assumed politeness to -1
		        for (Scheduler sched : schedulers)
		        {
		        	if (sched instanceof MCTS_Scheduler)
		        	{
		        		((MCTS_Scheduler)sched).assumed_politeness_of_other_agent = -1.0;
		        	}
		        }
		        for (Scheduler sched : scheduler_clones)
		        {
		        	if (sched instanceof MCTS_Scheduler)
		        	{
		        		((MCTS_Scheduler)sched).assumed_politeness_of_other_agent = -1.0;
		        	}
		        }
		        
		        // Adversarial experiments
		        for (int agent_1 = 0; agent_1 < schedulers.size(); agent_1++)
		        {
		        	for (int agent_2 = agent_1; agent_2 < schedulers.size(); agent_2++)
		        	{
			        	if (agent_2 == agent_1)
			        	{
			        		// Mirror match
					        new Match("adversarial_" + scheduler_names.get(agent_1) + "_vs_" + scheduler_names.get(agent_2),
					            	numGoalPlanTrees,
					            	AllianceType.ADVERSARIAL,
					            	currentState.clone(),
					            	new Scheduler[] {schedulers.get(agent_1), scheduler_clones.get(agent_2)},
					            	new String[] {scheduler_names.get(agent_1), scheduler_names.get(agent_2) + "_clone"},
					            	-1.0).run_two_sided_series(true, true);
			        	}
			        	else
			        	{
					        new Match("adversarial_" + scheduler_names.get(agent_1) + "_vs_" + scheduler_names.get(agent_2),
					            	numGoalPlanTrees,
					            	AllianceType.ADVERSARIAL,
					            	currentState.clone(),
					            	new Scheduler[] {schedulers.get(agent_1), schedulers.get(agent_2)},
					            	new String[] {scheduler_names.get(agent_1), scheduler_names.get(agent_2)},
					            	-1.0).run_two_sided_series(true, true);
			        	}
		        	}
		        }
	        }
        }
    }
}
