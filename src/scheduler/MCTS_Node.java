package scheduler;

import beliefbase.BeliefBase;
import beliefbase.Condition;
import goalplantree.ActionNode;
import goalplantree.GoalNode;
import goalplantree.PlanNode;
import goalplantree.TreeNode;

import java.util.ArrayList;

public class MCTS_Node {

    // state of this node
    public State state;

    // selection information
    int intentionChoice = -1;
    int planChoice = -1;

    // child nodes
    public ArrayList<MCTS_Node> children;
    
    // statistics
    public int nVisits;
    public Match match;
	public double[] totValue;
	public double[] totSqValue;

    public MCTS_Node(State state, Match match)
    {
        this.state = state;
        this.match = match;
        
        // statistics initialisation
        init();
    }

    /**
     * initialisation
     */
    private void init()
    {
        nVisits = 0;
		this.totValue = new double[match.schedulers.length];
		this.totSqValue = new double[match.schedulers.length];
    }

    /**
     * @return true if it is the leaf node; false, otherwise
     */
    public boolean isLeaf()
    {
        return children == null;
    }
    
    /**
     * expand the current node
     */
    public void expand(boolean[] intentionAvailable, boolean include_pass)
    {
        children = new ArrayList<>();

        // Generate all possible child nodes
        for (int i = 0; i < state.intentions.size(); i++)
        {
        	if (!intentionAvailable[i])
        	{
        		continue;
        	}
        	
            // ignore the intention which already has been achieved
            if (state.intentions.get(i) != null)
            {
                // if the next step of this intention is an action
                if(state.intentions.get(i) instanceof ActionNode)
                {
                    // cast it to an action
                    ActionNode action = (ActionNode) state.intentions.get(i);
                    
                    // get its precondition
                    Condition[] prec = action.getPrec();

                    // if its precondition holds
                    if(state.beliefs.evaluate(prec))
                    {
                        // clone the current belief base and update
                        BeliefBase nBeliefs = state.beliefs.clone();
                        
                        // get the postcondition of this action
                        Condition[] postc = action.getPostc();
                        
                        // apply it to the new env
                        nBeliefs.apply(postc);

                        // copy and update intentions
                        ArrayList<TreeNode> nIntentions = new ArrayList<TreeNode>(state.intentions);
                        
                        // update the next step
                        nIntentions.set(i, nIntentions.get(i).nextIstep());
                        
                        // create a new node
                        int nPlayerTurn = (state.playerTurn + 1) % match.numAgents;
                        MCTS_Node node = new MCTS_Node(new State(nBeliefs, nIntentions, nPlayerTurn), match);
                        
                        // update intention selection information, as there is no plan selections, the plan choice is set to -1
                        node.intentionChoice = i;
                        
                        // add the new node to the child list
                        this.children.add(node);
                    }
                }
                
                // if the next step of this intention is achieving a (sub)goal
                else if (state.intentions.get(i) instanceof GoalNode)
                {
                    // cast it to a goal node
                    GoalNode goal = (GoalNode) state.intentions.get(i);
                    
                    // get all relevant plans
                    PlanNode[] pls = goal.getPlans();

                    // check all relevant plans
                    for(int j = 0; j < pls.length; j++)
                    {
                        // check the precondition of each plan
                        Condition[] prec = pls[j].getPrec();

                        if(state.beliefs.evaluate(prec))
                        {
                            TreeNode[] body = pls[j].getPlanbody();
                            
                            ArrayList<TreeNode> nIntentions = new ArrayList<TreeNode>(state.intentions);
                            BeliefBase nBeliefs = state.beliefs.clone();

                            // if the plan is not empty
                            if(body.length > 0)
                            {
                                TreeNode firstStep = body[0];
                                
                                // if the first step in a plan is an action
                                if(firstStep instanceof ActionNode)
                                {
                                    ActionNode action = (ActionNode) firstStep;
                                    
                                    // apply this action
                                    Condition[] postc = action.getPostc();
                                    nBeliefs.apply(postc);
                                    
                                    // update the next step
                                    nIntentions.set(i, firstStep.nextIstep());
                                }
                                else
                                {
                                    nIntentions.set(i, firstStep);
                                }
                            }
                            else
                            {
                                nIntentions.set(i, nIntentions.get(i).nextIstep());
                            }
                            
                            // clone the state and generate a new MCTS node
                            int nPlayerTurn = (state.playerTurn + 1) % match.numAgents;
                            MCTS_Node node = new MCTS_Node(new State(nBeliefs, nIntentions, nPlayerTurn), match);
                            
                            // update intention choice and plan choice
                            node.intentionChoice = i;
                            node.planChoice = j;
                            
                            // add to the child list
                            this.children.add(node);
                        }
                    }
                }
            }
        }
        
        // Give the ability for the player to pass
        if (include_pass)
        {
	        MCTS_Node pass_node = new MCTS_Node(new State(state.beliefs.clone(), new ArrayList<TreeNode>(state.intentions), (state.playerTurn + 1) % match.numAgents), match);
	        
	        // Intention choice equal to intentions.size() represents "pass" action
	        pass_node.intentionChoice = -1;
	        
	        this.children.add(pass_node);
        }
    }

    public int getIntentionChoice()
    {
        return intentionChoice;
    }

    public int getPlanChoice()
    {
        return planChoice;
    }
}
