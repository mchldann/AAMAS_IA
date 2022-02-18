package scheduler;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import util.Log;

public class MCTS_Scheduler extends Scheduler {

    public enum VisionType
    {
    	FULL,
    	UNAWARE,
    	PARTIALLY_AWARE
    }
    
	public VisionType vision_type;
	
	public MCTS_Node rootNode;
	
	public int alpha, beta;
	public double c, rollout_stochasticity, assumed_politeness_of_other_agent;
	private Scheduler rollout_schedulers[];
	public boolean[] gpt_visible;
	
    // random
    static Random rm = new Random();
    
    // a very small value used for breaking the tie and dividing by 0
    static final double epsilon = 1e-6;
	
    // statistics
    public int nRollouts;
    
    public MCTS_Scheduler(VisionType vision_type, int alpha, int beta, double c, double rollout_stochasticity, double assumed_politeness_of_other_agent)
    {
    	this.vision_type = vision_type;
    	this.alpha = alpha;
    	this.beta = beta;
    	this.c = c;
    	this.rollout_stochasticity = rollout_stochasticity;
    	this.assumed_politeness_of_other_agent = assumed_politeness_of_other_agent;
    }
    
	@Override
	public void reset()
	{
		// Nothing to reset for this scheduler
	}
    
    @Override
    public void loadMatchDetails(Match match, int agent_num, boolean mirror_match)
    {
    	super.loadMatchDetails(match, agent_num, mirror_match);
    	
		this.gpt_visible = new boolean[match.numGoalPlanTrees];
		
		for (int intentionNum = 0; intentionNum < match.numGoalPlanTrees; intentionNum++)
		{
			int agentToAssignIntention = mirror_match? ((intentionNum + 1) % match.numAgents) : (intentionNum % match.numAgents);
			
			if (agentToAssignIntention == agent_num)
			{
				gpt_visible[intentionNum] = true; // Can always see own GPTs
			}
			else
			{
				switch(vision_type)
				{
					case FULL:
						gpt_visible[intentionNum] = true;
						break;
						
					case PARTIALLY_AWARE:
						
						if ((intentionNum / match.numAgents) % 2 == 0)
						{
							gpt_visible[intentionNum] = true;
						}
						else
						{
							gpt_visible[intentionNum] = false;
						}

						break;
						
					case UNAWARE:
					default:
						gpt_visible[intentionNum] = false;
				}
			}
		}
		
    	this.rollout_schedulers = new Scheduler[match.numAgents];
    	for (int i = 0; i < match.numAgents; i++)
    	{
    		this.rollout_schedulers[i] = new Stochastic_FIFO_Scheduler(rollout_stochasticity, gpt_visible);
    	}
    }
    
    public Decision getDecision(State state)
    {
    	rootNode = new MCTS_Node(state, match);
    	nRollouts = 0;
    	
    	run(alpha, beta);
    	
        int iChoice = -1;
        int pChoice = -1;
        int visits = -1;
        double average = Double.NEGATIVE_INFINITY;

        Log.info("Actions available:");
        
        for(int i = 0; i < rootNode.children.size(); i++)
        {
        	Log.info("Intention " + rootNode.children.get(i).getIntentionChoice() + ", plan " + rootNode.children.get(i).getPlanChoice()
        			+ ": Ave. val = " + (rootNode.children.get(i).totValue[agent_num] / rootNode.children.get(i).nVisits)
        			+ ", visits = " + rootNode.children.get(i).nVisits);
        	
            if(rootNode.children.get(i).totValue[agent_num] / rootNode.children.get(i).nVisits > average)
            {
                iChoice = rootNode.children.get(i).getIntentionChoice();
                pChoice = rootNode.children.get(i).getPlanChoice();
                visits = rootNode.children.get(i).nVisits;
                average = rootNode.children.get(i).totValue[agent_num] / rootNode.children.get(i).nVisits;
            }
        }
        
        Log.info("Intention choice: " + iChoice + ", plan choice: " + pChoice
        		+ " (Averaged " + average + " from " + visits + " visits)");
        
        return new Decision(iChoice, pChoice, (rootNode.children.size() == 1));
    }
    
    /**
     * @return a node with maximum UCT value
     */
    private MCTS_Node select(MCTS_Node currentNode)
    {
        // Initialisation
        MCTS_Node selected = null;
        double bestUCT = Double.NEGATIVE_INFINITY;
        
        // Calculate the UCT value for each of its selected nodes
        for(int i = 0; i < currentNode.children.size(); i++)
        {
            // UCT calculation
            double uctValue = currentNode.children.get(i).totValue[currentNode.state.playerTurn] / (currentNode.children.get(i).nVisits + epsilon)
            		+ c * Math.sqrt(Math.log(nRollouts + 1) / (currentNode.children.get(i).nVisits + epsilon))
            		+ epsilon * rm.nextDouble(); // For tie-breaking
            
            // Compare with the current maximum value
            if(uctValue > bestUCT)
            {
                selected = currentNode.children.get(i);
                bestUCT = uctValue;
            }
        }
        
        // Return the nodes with maximum UCT value, null if current node is a leaf node (contains no child nodes)
        return selected;
    }
    
    /**
     * The main MCTS process
     * @param alpha number of iterations
     * @param beta number of simulation per iteration
     */
    private void run(int alpha, int beta)
    {	 
        long startTime = System.currentTimeMillis();
        
        // Record the list of nodes that has been visited
        List<MCTS_Node> visited = new LinkedList<>();
        
        // Run alpha iterations
        for(int i = 0; i < alpha; i++)
        {
            visited.clear();
            
            // Set the current node to this node
            MCTS_Node currentNode = this.rootNode;
            
            // Add this node to the list of visited node
            visited.add(currentNode);
            
            // Find the leaf node which has the largest UCT value
            while ((currentNode != null) && !currentNode.isLeaf())
            {
                currentNode = select(currentNode);
                
                if (currentNode != null)
                {
                    visited.add(currentNode);
                }
            }

        	boolean[] intentionAvailable = new boolean[match.numGoalPlanTrees];
        	for (int int_num = 0; int_num < match.numGoalPlanTrees; int_num++)
        	{
        		intentionAvailable[int_num] = available_intentions[currentNode.state.playerTurn][int_num] && gpt_visible[int_num];
        	}
        	
    		currentNode.expand(intentionAvailable, true);

            // Select a node for simulation
            currentNode = select(currentNode);
            visited.add(currentNode);
            
            // TODO: Tidy this logic later
            int otherAgentNum = 1 - agent_num;
            
            // Simulation
            for (int j = 0; j < beta; j++)
            {
            	// TODO: This assumes that the rollout schedulers are not stateful. It might be safer to clone them.
                Match m = new Match("MCTS_rollout", match.numGoalPlanTrees, match.allianceType, currentNode.state.clone(),
                	rollout_schedulers, new String[] {"rollout_a1", "rollout_a2"}, match.assumed_politeness);
                
                State endOfGame = m.run(false, false, mirror_match);
                
                nRollouts++;
                
                // Back-propagation
                // TODO: Fix this logic later so that it caters for more than two agents. It's pretty hacky right now.
                for(MCTS_Node node : visited)
                {
                    node.nVisits++;
                    
                    // Update value for this scheduler
                    double value_for_this_agent = endOfGame.getStateScore(this);
                	node.totValue[agent_num] += value_for_this_agent;
        			node.totSqValue[agent_num] += value_for_this_agent * value_for_this_agent;
                    
                    // Update value for other agent
        			if (match.schedulers.length > 1)
        			{
        				// Only count intentions that *this* scheduler can see
        		    	boolean[] intention_counts_towards_score = new boolean[endOfGame.intentions.size()];
	        	        for (int intNum = 0; intNum < endOfGame.intentions.size(); intNum++)
        		    	{
        		    		intention_counts_towards_score[intNum] = gpt_visible[intNum];
        		    	}

	        	    	int score = 0;
	        	    	
	        	        for (int intNum = 0; intNum < endOfGame.intentions.size(); intNum++)
	        	        {
	        	        	if (intention_counts_towards_score[intNum])
	        	        	{
	        	        		// If the intention is completed...
	        	                if (endOfGame.intentions.get(intNum) == null)
	        	                {
	        	                	// If the intention belongs to *this* agent
	        	                	if (available_intentions[agent_num][intNum])
	        	                	{
		        	                	score += assumed_politeness_of_other_agent;
	        	                	}
	        	                	else
	        	                	{
	        	                		// Intention belongs to the other agent (assuming there are two agents total)
		        	                	score += 1.0;
	        	                	}
	        	                }
	        	        	}
	        	        }
	        	        
	                	node.totValue[otherAgentNum] += score;
	        			node.totSqValue[otherAgentNum] += score * score;
        			}
                }
            }
        }

        Log.info("MCTS calculation time = " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
