package scheduler;

public class FIFO_Scheduler extends Scheduler {

	public MCTS_Node rootNode;
	
	private int previousIChoice;
	private int previousPChoice;
	
    public FIFO_Scheduler()
    {	
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
    	this.rootNode = new MCTS_Node(state, match);
    	
    	this.rootNode.expand(available_intentions[agent_num], false);
    	
    	boolean playerMustPass = rootNode.children.size() == 0;
    	
    	if (playerMustPass)
    	{
    		previousIChoice = -1;
    		previousPChoice = -1;
    		return new Decision(previousIChoice, previousPChoice, true);
    	}
    	
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
    	
    	// Worst case, return the first available intention and plan
		previousIChoice = rootNode.children.get(0).intentionChoice;
		previousPChoice = rootNode.children.get(0).planChoice;
		return new Decision(previousIChoice, previousPChoice, false);
    }
}
