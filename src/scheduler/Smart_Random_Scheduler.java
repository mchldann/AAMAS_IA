package scheduler;
import beliefbase.BeliefBase;
import beliefbase.Condition;
import goalplantree.ActionNode;
import goalplantree.GoalNode;
import goalplantree.PlanNode;
import goalplantree.TreeNode;
import util.Log;

import java.util.ArrayList;
import java.util.Random;

public class Smart_Random_Scheduler extends Scheduler {
    
    private static Random rm = new Random();
    
    private BeliefBase beliefs;
    private ArrayList<TreeNode> intentions;
    private double[] coverage;
    private boolean verbose;

    public Smart_Random_Scheduler(boolean verbose)
    {
        this.verbose = verbose;
    }
    
    public Smart_Random_Scheduler()
    {
    	this(true);
    }

    public void update(State state)
    {
        this.beliefs = state.getBeliefBase();
        this.intentions = state.getIntentions();
        this.coverage = new double[intentions.size()];
    }
    
	@Override
	public void reset()
	{
        
	}
    
	@Override
	public Decision getDecision(State state)
	{
		update(state);
		
        // re-calculate the coverage
        calculate();
        
        int iChoice = -1;
        ArrayList<Integer> ava = new ArrayList<>();
        
        for (int i = 0; i < coverage.length; i++)
        {
        	// Only allow an intention to be selected if it belongs to the player
            if (available_intentions[agent_num][i] && progressable(i) && (coverage[i] > 0.0) && (coverage[i] < 1.0))
            {
            	ava.add(i);
            }
        }

        if (ava.size() > 0)
        {
        	iChoice = ava.get(rm.nextInt(ava.size()));
        }
        else
        {
            ava = new ArrayList<>();
            
            for (int i = 0; i < coverage.length; i++)
            {
            	// Only allow an intention to be selected if it belongs to the player
                if(available_intentions[agent_num][i] && progressable(i))
                {
                	ava.add(i);
                }
            }
            
            if (ava.size() > 0)
            {
            	iChoice = ava.get(rm.nextInt(ava.size()));
            }
        }
        
		int pChoice = -1;
		if ((iChoice != -1) && (intentions.get(iChoice) instanceof GoalNode))
		{
			GoalNode goal = (GoalNode) intentions.get(iChoice);
			Log.info(goal.getType(), verbose);
            
            PlanNode[] plans = goal.getPlans();
            
            // Choose a random plan
            ArrayList<Integer> ava_plans = new ArrayList<>();
            for(int i = 0; i < plans.length; i++)
            {
                Condition[] prec = plans[i].getPrec();
                if(beliefs.evaluate(prec))
                {
                	ava_plans.add(i);
                }
            }
            
            if (ava_plans.size() > 0)
            {
            	pChoice = ava_plans.get(rm.nextInt(ava_plans.size()));
            }
            else
            {
            	pChoice = -1;
            }
		}

		boolean forced_pass = (iChoice == -1);
		
		if (rm.nextDouble() < 0.2)
		{
			return new Decision(-1, -1, forced_pass);
		}
		else
		{
			return new Decision(iChoice, pChoice, forced_pass);
		}
	}

    /**
     * calculate the current coverage for each intention
     */
    public void calculate()
    {
        for(int i = 0; i < coverage.length; i++)
        {
            // if this intention is finished
            if(intentions.get(i) == null)
            {
                coverage[i] = 0;
            }
            // if it is not
            else
            {
                double result = 1;

                TreeNode current = intentions.get(i);

                while (current != null)
                {
                    if(current instanceof GoalNode)
                    {
                        GoalNode goal = (GoalNode) current;
                        result = result * goal.getEc();
                    }

                    current = nextCoverage(current);
                }

                coverage[i] = result;
            }
        }

        for (int i = 0; i < coverage.length; i++)
        {
        	Log.info("intention " + i + ": " + coverage[i], verbose);
        }

    }

    private TreeNode nextCoverage(TreeNode node)
    {
        // if there is another step in the same plan
        if(node.getNext() != null)
        {
            return node.getNext();
        }
        else
        {
            // if it is the top-level goal
            if(node.getParent() == null)
            {
                return null;
            }
            else
            {
                // if it is the last step in a plan
                TreeNode goal = node.getParent().getParent();// get the parent node it is going to achieve
                return nextCoverage(goal);
            }
        }
    }


    public double[] getCoverage()
    {
        return this.coverage;
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