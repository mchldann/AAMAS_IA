package scheduler;

import java.util.Random;

public class Stochastic_FIFO_Scheduler extends Scheduler {

	public MCTS_Node rootNode;
	
    private static Random rm = new Random();
    
	private int previousIChoice;
	private int previousPChoice;
	
	private double stochasticity;
	private boolean[] gpt_visible;
	
    public Stochastic_FIFO_Scheduler(double stochasticity, boolean[] gpt_visible)
    {	
    	this.stochasticity = stochasticity;
    	this.gpt_visible = gpt_visible;
    	
    	previousIChoice = -1;
    	previousPChoice = -1;
    }
    
	@Override
	public void reset()
	{
    	previousIChoice = -1;
    	previousPChoice = -1;
	}
    
    public Decision getDecision(State state)
    {
    	boolean[] restricted_intentions = new boolean[gpt_visible.length];
    	for (int i = 0; i < restricted_intentions.length; i++)
    	{
    		restricted_intentions[i] = (gpt_visible[i] && available_intentions[agent_num][i]);
    	}
    	
		if (rm.nextDouble() < stochasticity)
		{
	    	this.rootNode = new MCTS_Node(state, match);
	    	
	    	this.rootNode.expand(restricted_intentions, true);
	    	
	    	boolean playerMustPass = rootNode.children.size() == 1;
	    	
	        int i = rm.nextInt(rootNode.children.size());

    		//return new Decision(rootNode.children.get(i).getIntentionChoice(), rootNode.children.get(i).getPlanChoice(), playerMustPass);
    		
	        previousIChoice = rootNode.children.get(i).getIntentionChoice();
    		previousPChoice = rootNode.children.get(i).getPlanChoice();
    		return new Decision(previousIChoice, previousPChoice, playerMustPass);
		}
		
    	this.rootNode = new MCTS_Node(state, match);
    	
    	this.rootNode.expand(restricted_intentions, false);
    	
    	boolean playerMustPass = rootNode.children.size() == 0;
    	
    	if (playerMustPass)
    	{
    		previousIChoice = -1;
    		previousPChoice = -1;
    		return new Decision(previousIChoice, previousPChoice, true);
    	}
    	
    	if (previousIChoice != -1)
    	{
	    	// Stick with previous intention and plan if possible
	    	for (MCTS_Node child : rootNode.children)
	    	{
	    		if (child.intentionChoice == previousIChoice && child.planChoice == previousPChoice)
	    		{
	        		previousIChoice = child.intentionChoice;
	        		previousPChoice = child.planChoice;
	        		return new Decision(previousIChoice, previousPChoice, false);
	    		}
	    	}
	    	
	    	// At least stick with previous intention if not possible to stick with previous plan
	    	for (MCTS_Node child : rootNode.children)
	    	{
	    		if (child.intentionChoice == previousIChoice)
	    		{
	        		previousIChoice = child.intentionChoice;
	        		previousPChoice = child.planChoice;
	    			return new Decision(previousIChoice, previousPChoice, false);
	    		}
	    	}
    	}
    	
    	// Worst case, return a random intention and plan
		int childNum = rm.nextInt(rootNode.children.size());
    	previousIChoice = rootNode.children.get(childNum).intentionChoice;
		previousPChoice = rootNode.children.get(childNum).planChoice;
		return new Decision(previousIChoice, previousPChoice, false);
    }
}
