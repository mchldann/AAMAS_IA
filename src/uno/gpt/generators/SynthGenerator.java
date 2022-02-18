/*
 * Copyright 2016 Yuan Yao
 * University of Nottingham
 * Email: yvy@cs.nott.ac.uk (yuanyao1990yy@icloud.com)
 *
 * Modified 2019 IPC Committee
 * Contact: https://www.intentionprogression.org/contact/
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details 
 *  <http://www.gnu.org/licenses/gpl-3.0.html>.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package uno.gpt.generators;

import uno.gpt.structure.*;

import java.util.*;

/**
 * @version 1.0
 */
public class SynthGenerator extends AbstractGenerator {
	/** Default values */
	static final int def_depth = 3,
						def_num_goal = 3,
						def_num_plan = 3,
						def_num_action = 3,
						def_num_var = 30;
	static final double def_safety_factor = 0.5d;
	static final double def_pr_goal_has_single_plan = 0.0d;
	
	/** id of the tree */
	private int id;

	/** total number of goals in this goal plan tree */
	private int treeGoalCount;

	/** total number of plans in this goal plan tree */
	private int treePlanCount;

	/** total number of actions in this goal plan tree */
	private int treeActionCount;

	/** random generators */
	final private Random rm;

	/** depth of the tree */
	final private int tree_depth;

	/** number of trees */
	final private int num_tree;

	/** number of goals */
	final private int num_goal;

	/** number of plans */
	final private int num_plan;

	/** number of actions */
	final private int num_action;

	/** number environment variables */
	final private int num_var;

	/** proportion of plans to make safe for any given goal */
	final private double safety_factor;

	/** proportion of goals that have only a single plan */
	final private double pr_goal_has_single_plan;
	
	/** Constructor */
	SynthGenerator(int seed, int tree_depth, int num_tree, int num_goal, int num_plan, int num_action, int num_var, double safety_factor, double pr_goal_has_single_plan)
	{
		this.rm = new Random(seed);
		this.tree_depth = tree_depth;
		this.num_tree = num_tree;
		this.num_goal = num_goal;
		this.num_plan = num_plan;
		this.num_action = num_action;
		this.num_var = num_var;
		this.safety_factor = safety_factor;
		this.pr_goal_has_single_plan = pr_goal_has_single_plan;
	}

	/**
	 * Generate environment
	 * @return the generated environment*/
	public HashMap<String, Literal> genEnvironment(){
		environment = new HashMap<>();
		Literal workingLit;
		for (int i = 0; i < num_tree; i++) {
			workingLit = new Literal("G-" + i, false, false, false);
			environment.put(workingLit.getId(), workingLit);
		}
		for (int i = 0; i < num_var; i++) {
			workingLit = new Literal("EV-" + i, rm.nextBoolean(), true, false);
			environment.put(workingLit.getId(), workingLit);
		}
		return environment;
	}

	/**
	 * A function for producing the top level goals for the GPTs
	 * @param index The index of the Goal being produced
	 * @return A Goal Node
	 */
	@Override
	public GoalNode genTopLevelGoal(int index) {
		// Set the generator id
		this.id = index;
		// Set the counters for this tree to 0
		this.treeGoalCount = 0;
		this.treePlanCount = 0;
		this.treeActionCount = 0;
		// Make the node and name it TN-G0
		GoalNode topLevelGoal = new GoalNode("T" + this.id + "-G" + this.treeGoalCount++);
		// Add the Goal Condition
		topLevelGoal.getGoalConds().add(produceLiteral("G-" + id, true));
		/* Hand over to the suitable function to populate the goal recursively
			Pass the Goal
			The Current Depth, 0 for a top level goal
			A safe set of
		 */
		populateGoal(topLevelGoal, 1, makeSafe());
		// Return the fully populated goal
		return topLevelGoal;
	}

	/**
	 * A function to recursively constuct a goal's subtree
	 * @param goalNode The goal node to build from
	 * @param currentDepth The current tree_depth, used to control recursion
	 * @param preConds The precoditions for plans to complete this goal
	 * @return Returns the list of potential states the truth of which will vary based on the plan used
	 */
	private List<Literal> populateGoal(GoalNode goalNode, int currentDepth, List<Literal> preConds) {
		// Init the full list of preconditions for paths that follow
		// These may occur after this goal is complete but are not guaranteed
		HashSet<Literal> potentialState = new HashSet<>();

		// Populate plans
		int sampled_num_plan = num_plan;
		if (rm.nextDouble() < pr_goal_has_single_plan)
		{
			sampled_num_plan = 1;
		}
		
		// Ensure that you have enough PreConditions to grabbag for randomness
		while (preConds.size() < sampled_num_plan){
			preConds.addAll(preConds.subList(0, preConds.size()));
		}
		
		for (int i = 0; i < sampled_num_plan; i++) {
			// Create a blank plan
			PlanNode workingPlan = new PlanNode("T" + this.id + "-P" + this.treePlanCount++);
			// Add this plan to the goal's set of plans
			goalNode.getPlans().add(workingPlan);
			// Add one of the PreConditions at random.
			workingPlan.getPre().add(preConds.remove(rm.nextInt(preConds.size())));
			// Populate the plan
			List<Literal> workingPotState = populatePlan(workingPlan, currentDepth, goalNode.getGoalConds(), sampled_num_plan);
			//Add the potential state after this plan to the overall potential state
			potentialState.addAll(workingPotState);
		}

		return new ArrayList<>(potentialState);
	}

	/**
	 * Method to populate a plan recursively
	 * @param planNode The plan to be populated
	 * @param currentDepth The current tree_depth, used to control recursion.
	 * @param goalConds The condition that the plan is intending to fulfil
	 * @return Return the conditions this plan ensures
	 */
	private List<Literal> populatePlan (PlanNode planNode, int currentDepth, List<Literal> goalConds, int sampled_num_plan) {
		// The potential preConds for subGoals and actions
		List<Literal> internalPreConds = new ArrayList<>(planNode.getPre());
		// The unsafe list of preconditions used for making extra plan options
		List<Literal> unsafePreConds = new ArrayList<>(internalPreConds);
		// The potential postConds and goalConds
		List<Literal> internalPostGoalConds = new ArrayList<>(getInvertEnvLiterals());
		// The post conditions this plan will ensure
		List<Literal> certainPostConds = new ArrayList<>();

		// Remove the internal PreConds from the internal Post and Goal Conds
		internalPostGoalConds.removeIf(internalPreConds :: contains);
		// Also remove the future goal conditions to prevent early completion
		internalPostGoalConds.removeIf(internalPreConds :: contains);

		// Make the actions
		for (int i = 0; i < num_action; i++) {
			// Create a new action
			ActionNode workingAction = new ActionNode("T" + this.id + "-A" + this.treeActionCount++);
			// Provide a precondition for the action
			workingAction.getPreC().add(internalPreConds.get(rm.nextInt(internalPreConds.size())));

			// Select a post condition
			Literal workingPostCond = internalPostGoalConds.remove(rm.nextInt(internalPostGoalConds.size()));
			// Add it to the internalPreConds and to the certainPostConds
			internalPreConds.add(workingPostCond);
			certainPostConds.add(workingPostCond);
			// Add to the action
			workingAction.getPostC().add(workingPostCond);

			// Check if this action needs to be goal completing
			if ((currentDepth == tree_depth) && (i == (num_action - 1))){
				// If so add the goal conditions to the post condition
				workingAction.getPostC().addAll(goalConds);
			}

			// Add the action to the plan body
			planNode.getPlanBody().add(workingAction);
		}

		// Check for recursive depth
		if (currentDepth != tree_depth){
			// Make the goals
			for (int i = 0; i < num_goal; i++) {
				// Create a new subGoal
				GoalNode workingGoal = new GoalNode("T" + this.id + "-G" + this.treeGoalCount++);

				// Make copies of the lists to allow selection without replacement
				List<Literal> workingSafePreConds = new ArrayList<>(internalPreConds);
				List<Literal> workingUnsafePreConds = new ArrayList<>(unsafePreConds);
				// Make a new list to populate
				List<Literal> workingPreConds = new ArrayList<>();

				// Fill the safe requirements
				while (workingPreConds.size() < Math.ceil(safety_factor * sampled_num_plan)){
					// If there are safe preconditions in the bag pick one
					if (workingSafePreConds.size() > 0){
						workingPreConds.add(workingSafePreConds.remove(rm.nextInt(workingSafePreConds.size())));
					// Else replenish the bag if needed
					} else {
						workingSafePreConds.addAll(internalPreConds);
					}
				}
				// Fill the unsafe requirements
				while (workingPreConds.size() < sampled_num_plan){
					// If there are unsafe preconditions in the bag pick one
					if (workingUnsafePreConds.size() > 0) {
						workingPreConds.add(workingUnsafePreConds.remove(rm.nextInt(workingUnsafePreConds.size())));
					// Else replenish the bag if needed
					} else if (unsafePreConds.size() > 0){
						workingUnsafePreConds.addAll(unsafePreConds);
					// If you can't replenish from the unsafe set use safe ones
					} else {
						workingUnsafePreConds.addAll(internalPreConds);
					}
				}

				// Select a goal condition
				Literal workingPostCond = internalPostGoalConds.remove(rm.nextInt(internalPostGoalConds.size()));
				// Add to the goal
				workingGoal.getGoalConds().add(workingPostCond);

				// Check if this goal needs to be superGoal completing
				if (i == (num_goal - 1)){
					// If so add the goal conditions to the goal conditions
					workingGoal.getGoalConds().addAll(goalConds);
				}

				// Populate the goal and add the total effects to the unsafe list
				unsafePreConds.addAll(populateGoal(workingGoal, currentDepth+1, new ArrayList<>(internalPreConds)));

				// Add the goals post conditions to the internalPreConds and to the certainPostConds
				internalPreConds.add(workingPostCond);
				certainPostConds.add(workingPostCond);

				// Add the goal to the plan
				planNode.getPlanBody().add(workingGoal);
			}
		}
		// Return the list of certain post conditions
		return certainPostConds;
	}

	/** Get the environmental or non-goal variables as ids */
	private List<String> getEnvLitsAsStrings(){
		// Make a new List from the literals of the environment
		List<String> envLits = new ArrayList<>(environment.keySet());
		// Filter out the goal Literals
		envLits.removeIf(l -> (l.startsWith("G-")));
		// Return this sublist
		return envLits;
	}

	/** Get full environmental variables as copies with both values. */
	private List<Literal> getInvertEnvLiterals(){
		// Pull and the environment literals
		List<String> envLits = getEnvLitsAsStrings();
		// Make an empty list
		List<Literal> fullEnvLits = new ArrayList<>();
		// For each environment lit
		for (String litId : envLits) {
			// Clone it
			Literal newLit = environment.get(litId).clone();
			// Flip the copy
			newLit.flip();
			// Add that copy
			fullEnvLits.add(newLit);
		}
		// Return the new list
		return fullEnvLits;
	}

	/** Default wrapper */
	private List<Literal> makeSafe(){
		// Call full with defaults
		return makeSafe(new ArrayList<>(), num_plan);
	}

	/** Makes a given list of literals into a safe one by adding p ¬p pairs. */
	private List<Literal> makeSafe(List<Literal> list, int target){
		// Get the IDs for the literals we are working with.
		List<String> envLits = getEnvLitsAsStrings();
		// Loop over the given list and remove those from the pool.
		for (Literal lit: list) {
			envLits.removeIf(s -> lit.getId().equals(s));
		}
		// If you can add a pair
		while(list.size() < target) {
			// Select an ID, remove it from the pool, get the corresponding literal and make a copy.
			Literal newLit = environment.get(envLits.remove(rm.nextInt(envLits.size()))).clone();
			// Add a copy of the clone, (One value)
			list.add(newLit.clone());
			// Flip the copy you haven't added
			newLit.flip();
			// Add that copy as well to have added both values of the literal
			list.add(newLit);
			//Ensure the environment isn't depleted
			if (envLits.size() == 0) {
				// Replenish it
				envLits = getEnvLitsAsStrings();
			}
		}
		// Return the list
		return list;
	}
}
