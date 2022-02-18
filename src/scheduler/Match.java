package scheduler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import beliefbase.Condition;
import goalplantree.ActionNode;
import goalplantree.GoalNode;
import goalplantree.PlanNode;
import goalplantree.TreeNode;
import util.Log;

public class Match
{
    public enum AllianceType
    {
        ADVERSARIAL,
        ALLIED,
        NEUTRAL;
    };
 
	static final int MAX_CONSECUTIVE_PASSES = 6;
	private int consecutive_passes;
    
    public String[] agent_names;
    public int numGoalPlanTrees;
    public AllianceType allianceType;
    public int numAgents;
    public double assumed_politeness;

	public String m_name;
	public State initialState;
	public Scheduler schedulers[];

    private Decision decision;
	
    public Match(String m_name, int numGoalPlanTrees, AllianceType allianceType, State initialState,
    	Scheduler[] schedulers, String[] agent_names, double assumed_politeness)
    {
    	this.m_name = m_name;
		this.numGoalPlanTrees = numGoalPlanTrees;
		this.allianceType = allianceType;
    	this.initialState = initialState.clone();
    	this.schedulers = schedulers;
		this.agent_names = agent_names;
		this.numAgents = schedulers.length;
		this.consecutive_passes = 0;
		this.assumed_politeness = assumed_politeness;
    }
    
    public boolean getNextDecision(State s)
    {
    	return getNextDecision(s, false);
    }
    
    public boolean getNextDecision(State s, boolean verbose)
    {
    	boolean playerMustPass = true;
    	int iter = 0;
    	
    	while (playerMustPass && (iter < numAgents))
    	{
    		Log.info("\n" + agent_names[s.playerTurn] +  "'s turn...", verbose);
    		
    		// This is an expensive call, hence wrapping in "if (verbose)"
    		if (verbose)
    		{
    			Log.info(s.beliefs.onPrintBB());
    		}
    		
    		this.decision = schedulers[s.playerTurn].getDecision(s);
    		playerMustPass = this.decision.forcedPass;
    		
            if (playerMustPass)
            {
        		Log.info("No available action, passing...", verbose);
        		
        		s.playerTurn = (s.playerTurn + 1) % numAgents;
        		iter++;
            }
    	}
    	
    	boolean atLeastOnePlayerCanMove = (iter < numAgents);
    	
    	if (!atLeastOnePlayerCanMove)
    	{
    		Log.info("All agents are out of actions.", verbose);
    	}
    	
    	return atLeastOnePlayerCanMove;
    }
    
    public void run_two_sided_series(boolean verbose, boolean write_results)
    {
    	run(verbose, write_results, false);
    	
    	// Alternate first agent to act for mirror match
    	State temp_state = initialState.clone();
    	initialState.playerTurn = (initialState.playerTurn + 1) % numAgents;
    	
    	run(verbose, write_results, true);
    	
    	initialState = temp_state;
    }
    
    public State run(boolean verbose, boolean write_results, boolean mirror_match)
    {
    	for (int i = 0; i < numAgents; i++)
    	{
    		schedulers[i].reset();
    		schedulers[i].loadMatchDetails(this, i, mirror_match);
    	}
    	
		// Reset to the initial state
    	State currentState = initialState.clone();

    	Log.info("MATCH COMMENCED", verbose);
        long startTime = System.currentTimeMillis();
    	
        boolean atLeastOnePlayerCanMove = getNextDecision(currentState, verbose);

        // While there are available executions
        while (atLeastOnePlayerCanMove)
        {
            // Execute the selected intention
            if (decision.iChoice == -1)
            {
            	consecutive_passes++;
            	Log.info("no intention selected (pass)", verbose);
            	
            	if (consecutive_passes >= MAX_CONSECUTIVE_PASSES)
            	{
                	Log.info("Game over due to repetition!", verbose);
            		break;
            	}
            }
            else
            {
            	consecutive_passes = 0;
            	Log.info("intention " + decision.iChoice +  " selected", verbose);
            	
	            TreeNode selected = currentState.intentions.get(decision.iChoice);
	            
	            if (selected instanceof GoalNode)
	            {
	                // Select plans based on the result of plan selection
	                PlanNode plan = ((GoalNode) selected).getPlanAt(decision.pChoice);
	                
	                Log.info(plan.getType() + " selected", verbose);
	
	                // Check pre-condition
	                Condition[] prec = plan.getPrec();
	                if(currentState.beliefs.evaluate(prec))
	                {
	                    Log.info(plan.getType() + " starts", verbose);
	                    
	                    // check the first step in this plan
	                    TreeNode first = plan.getPlanbody()[0];
	                    if(first instanceof ActionNode)
	                    {
	                        Log.info(first.getType() + " starts", verbose);
	                        
	                        // get its precondition
	                        Condition[] precA = ((ActionNode) first).getPrec();
	                        if(currentState.beliefs.evaluate(precA))
	                        {
	                            // update the environment
	                            Condition[] post = ((ActionNode) first).getPostc();
	                            currentState.beliefs.apply(post);
	                            
	                            // update intentions
	                            currentState.intentions.set(decision.iChoice, nextIstep(first));
	                            
	                            Log.info(first.getType() + " succeeds", verbose); 
	                        }
	                        else
	                        {
	                            Log.info(first.getType() + " fails", verbose);
	                        }
	                    }
	                    else if (first instanceof GoalNode)
	                    {
	                    	currentState.intentions.set(decision.iChoice, first);
	                    }
	                }
	                else
	                {
	                    Log.info(plan.getType() + " fails", verbose);
	                }
	
	            }
	            else if (selected instanceof ActionNode)
	            {
	            	
	                // cast it to an action
	                ActionNode action = (ActionNode) selected;
	                
	                // check the precondition of this action
	                Condition[] prec = action.getPrec();
	                
	                Log.info(action.getType() + " starts", verbose);
	                
	                if(currentState.beliefs.evaluate(prec)){
	                	
	                    // get postcondition
	                    Condition[] postc = action.getPostc();
	                    
	                    // update the environment
	                    currentState.beliefs.apply(postc);
	                    
	                    // update the intentions
	                    currentState.intentions.set(decision.iChoice, nextIstep(selected));
	                    
	                    Log.info(action.getType() + " succeeds", verbose);
	                }
	                else
	                {
	                    Log.info(action.getType() + " fails", verbose);
	                }
	            }
        	}

            currentState.playerTurn = (currentState.playerTurn + 1) % numAgents;
            
            atLeastOnePlayerCanMove = getNextDecision(currentState, verbose);
        }

        long total_match_time = System.currentTimeMillis() - startTime;
        
        // This is an expensive call, hence wrapping in "if (verbose)"
        if (verbose)
        {
        	Log.info(currentState.beliefs.onPrintBB());
        }
        
        for (int i = 0; i < numAgents; i++)
        {
        	schedulers[i].match = null; // Free match memory in case the scheduler is still referenced in the main method
            Log.info(agent_names[i] + "'s score = " + currentState.getStateScore(schedulers[i]), verbose);
        }
        Log.info("", verbose);
        
        if (write_results)
        {
        	writeResults(currentState, total_match_time);
        }
        
        return currentState;
    }
    
    public void writeResults(State s, long total_match_time)
    {
		try
		{
			String csv_file = Log.getLogDir() + "/match_results.csv";
			File f = new File(csv_file);
			boolean firstWrite = !f.exists();
			
			FileWriter fw = new FileWriter(csv_file, true);
	        BufferedWriter bw = new BufferedWriter(fw);
	        PrintWriter out = new PrintWriter(bw);
	        
	        // Output the header row if this is the first record we're writing
	        if (firstWrite)
	        {
	        	out.println("MatchName,ForestName,AssumedPoliteness,MatchTimeMillis,P1Name,P1Score,P1IntentionsComplete,P2Name,P2Score,P2IntentionsComplete");
	        }
	        
	        StringBuilder str = new StringBuilder();
            str.append(m_name);
            str.append("," + initialState.forest_name);
            str.append("," + assumed_politeness);
            str.append("," + total_match_time);
            
	        for (int i = 0; i < numAgents; i++)
	        {
	        	str.append("," + agent_names[i].replace("_clone", ""));
	        	str.append("," + s.getStateScore(schedulers[i]));
	        	str.append("," + s.getNumIntentionsCompleted(schedulers[i].available_intentions[i]));
	        }
	        
	        out.println(str.toString());
    	    out.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * @return the next step of an action in the given intention
     */
    public TreeNode nextIstep(TreeNode node)
    {
        // if it is not the last step in a plan
        if(node.getNext() != null)
        {
            return node.getNext();
        }
        // if it is
        else
        {
            // if it is the top-level goal
            if(node.getParent() == null)
            {
                return null;
            }
            else
            {
                // get the goal it is going to achieve
                GoalNode gn = (GoalNode) node.getParent().getParent();
                return nextIstep(gn);
            }
        }
    }
}
