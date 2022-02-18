package scheduler;
import beliefbase.BeliefBase;
import beliefbase.Condition;
import goalplantree.ActionNode;
import goalplantree.GoalNode;
import goalplantree.PlanNode;
import goalplantree.TreeNode;
import util.Log;

import java.util.ArrayList;

public class CScheduler extends Scheduler {

    public enum CoverageType
    {
        C0,
        C1;
    };
    
    private BeliefBase beliefs;
    private ArrayList<TreeNode> intentions;
    private double[] coverage;
    private CoverageType coverage_type;
    private boolean verbose;
    int index;

    public CScheduler(CoverageType coverage_type, boolean verbose)
    {
        this.coverage_type = coverage_type;
        this.verbose = verbose;
        this.index = -1;
    }
    
    public CScheduler(CoverageType coverage_type)
    {
    	this(coverage_type, true);
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
        this.index = -1;
	}
    
	@Override
	public Decision getDecision(State state)
	{
		update(state);
		
		int iChoice = -1;
		switch(coverage_type)
		{
			case C0:
				iChoice = getNextStepC0();
				break;
				
			case C1:
			default:
				iChoice = getNextStepC1();
		}
		
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
		
		return new Decision(iChoice, pChoice, (iChoice == -1));
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

    /**
     * C0 Priority:
     * 1. progressability
     * 2. focus on one intention
     * 3. low coverage
     */
    public int getNextStepC0()
    {
        // if there is an intention we are currently focusing and it is progressable, then return this index
        if(index >= 0 && index < intentions.size() && intentions.get(index) != null && progressable(index))
        {
            return index;
        }
        else
        {
            // re-calculate the coverage
            calculate();
            
            // and select a new intention to progress
            this.index = select();
            return this.index;
        }
    }

    /**
     * C1 Priority:
     * 1. progressability
     * 2. low coverage
     */
    public int getNextStepC1()
    {
        // re-calculate the coverage
        calculate();
        
        // and select a new intention to progress
        this.index = select();
        return this.index;
    }

    /**
     * select
     */
    public int select()
    {	
        ArrayList<Integer> ava = new ArrayList<>();
        
        // Sort the intentions according to their coverage
        Outer:
        for(int i = 0; i < coverage.length; i++)
        {
        	// Only allow an intention to be selected if it belongs to the player
            if(!available_intentions[agent_num][i])
            {
                continue;
            }
            
            // insert according to the coverage
            for(int j = 0; j < ava.size(); j++)
            {
                if(coverage[i] < coverage[ava.get(j)])
                {
                    ava.add(j, i);
                    continue Outer;
                }
            }
            
            // insert it at the tail
            ava.add(i);
        }

        for(int i = 0; i < ava.size(); i++)
        {
            // if this intention is progressable
            if(progressable(ava.get(i)))
            {
                return ava.get(i);
            }
        }
        
        // if no available
        return -1;
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