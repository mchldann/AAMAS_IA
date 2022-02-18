package scheduler;

import java.util.Random;

public class Random_Scheduler extends Scheduler {

	public MCTS_Node rootNode;
	
    private static Random rm = new Random();
    
	@Override
	public void reset()
	{
		// Nothing to reset for this scheduler
	}
    
    public Decision getDecision(State state)
    {
    	this.rootNode = new MCTS_Node(state, match);
    	
    	this.rootNode.expand(available_intentions[agent_num], true);
    	
    	boolean playerMustPass = rootNode.children.size() == 1;
    	
        int i = rm.nextInt(rootNode.children.size());

        return new Decision(rootNode.children.get(i).getIntentionChoice(),
        	rootNode.children.get(i).getPlanChoice(),
        	playerMustPass);
    }
}
