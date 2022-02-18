package scheduler;
import beliefbase.BeliefBase;
import beliefbase.Condition;
import goalplantree.ActionNode;
import goalplantree.GoalNode;
import goalplantree.PlanNode;
import goalplantree.TreeNode;
import util.Log;

import java.util.ArrayList;

public class Round_Robin_Scheduler extends Scheduler {

    private BeliefBase beliefs;
    private ArrayList<TreeNode> intentions;
    private boolean verbose;
    int index;

    public Round_Robin_Scheduler()
    {
        this.index = 0;
    }
    
    public void update(State state)
    {
        this.beliefs = state.getBeliefBase();
        this.intentions = state.getIntentions();
    }
    
	@Override
	public void reset()
	{
        this.index = 0;
	}
    
	@Override
	public Decision getDecision(State state)
	{
		update(state);
		
		int iter = 0;
		while (!progressable(index))
		{
			index = (index + 1) % intentions.size();
			
			iter++;
			if (iter >= intentions.size())
			{
				return new Decision(-1, -1, true);
			}
		}
		
		int iChoice = index;
		index = (index + 1) % intentions.size();

		int pChoice = -1;
		if ((iChoice != -1) && (intentions.get(iChoice) instanceof GoalNode))
		{
			GoalNode goal = (GoalNode) intentions.get(iChoice);
			Log.info(goal.getType(), verbose);
            
            PlanNode[] plans = goal.getPlans();
            
            // Find the first available plan
            for(int i = 0; i < plans.length; i++)
            {
                Condition[] prec = plans[i].getPrec();
                if(beliefs.evaluate(prec))
                {
                	pChoice = i;
                    break;
                }
            }
		}
		
		return new Decision(iChoice, pChoice, false);
	}

    public boolean progressable(int num)
    {
        if(intentions.get(num) == null)
        {
            return false;
        }
        else
        {
            if(intentions.get(num) instanceof ActionNode)
            {
                ActionNode action = (ActionNode) intentions.get(num);
                Condition[] prec = action.getPrec();
                
                // if its precondition holds
                if(beliefs.evaluate(prec))
                {
                    return true;
                }
                // false, otherwise
                else
                {
                    return false;
                }
            }
            else
            {
                GoalNode goal = (GoalNode) intentions.get(num);
                PlanNode[] plans = goal.getPlans();
                
                // check if there is a plan can be applied
                for(int i = 0; i < plans.length; i++)
                {
                    Condition[] prec = plans[i].getPrec();
                    // if one plan can be applied, return true
                    if(beliefs.evaluate(prec))
                    {
                        return true;
                    }
                }
                
                // if there is no applicable plans, return false
                return false;
            }
        }
    }
}