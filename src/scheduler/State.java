package scheduler;

import beliefbase.BeliefBase;
import goalplantree.TreeNode;

import java.util.ArrayList;

public class State {

	public String forest_name;
	
    // current belief base
    public BeliefBase beliefs;

    // the list of intentions
    public ArrayList<TreeNode> intentions;
    
    public int playerTurn;

    public State(String forest_name, BeliefBase beliefs, ArrayList<TreeNode> intentions, int playerTurn)
    {
    	this.forest_name = forest_name;
        this.beliefs = beliefs;
        this.intentions = new ArrayList<TreeNode>(intentions);
        this.playerTurn = playerTurn;
    }
    
    public State(BeliefBase beliefs, ArrayList<TreeNode> intentions, int playerTurn)
    {
    	this("", beliefs, intentions, playerTurn);
    }
    
    public BeliefBase getBeliefBase()
    {
        return this.beliefs;
    }

    public ArrayList<TreeNode> getIntentions()
    {
        return this.intentions;
    }
    
    public int getTotalNumberOfGPTs()
    {
    	return this.intentions.size();
    }
    
    public int getNumIntentionsCompleted(boolean[] intention_available)
    {
    	int intentions_completed = 0;
    	
        for (int i = 0; i < intentions.size(); i++)
        {
        	if (intention_available[i] && (intentions.get(i) == null))
        	{
                intentions_completed++;
        	}
        }

        return intentions_completed;
    }
    
    public double getStateScore(Scheduler sched)
    {
    	double[] intention_values = sched.intention_values[sched.agent_num];

    	boolean[] intention_counts_towards_score = new boolean[intention_values.length];
    	if (sched instanceof MCTS_Scheduler)
    	{
    		for (int i = 0; i < intention_counts_towards_score.length; i++)
    		{
    			intention_counts_towards_score[i] = ((MCTS_Scheduler)sched).gpt_visible[i];
    		}
    	}
    	else
    	{
    		intention_counts_towards_score = sched.available_intentions[sched.agent_num];
    	}
    	
    	int score = 0;
    	
        for (int i = 0; i < intentions.size(); i++)
        {
        	if (intention_counts_towards_score[i])
        	{
                if (intentions.get(i) == null)
                {
                	score += intention_values[i];
                }
        	}
        }
        
        return (double)score;
    }

    /**
     * @return a copy of the current state
     */
    @Override
    public State clone()
    {
        // generate the new state
        return new State(forest_name, beliefs.clone(), new ArrayList<TreeNode>(intentions), playerTurn);
    }

}
