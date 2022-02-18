package scheduler;

import java.util.Random;

import scheduler.CScheduler.CoverageType;

public class StochasticCScheduler extends Scheduler {

	private double stochasticity;
	private CScheduler c_sched;
	private Random_Scheduler random_sched;
	
    private static Random rm = new Random();
	
	public StochasticCScheduler(CoverageType coverage_type, double stochasticity)
	{
		this.stochasticity = stochasticity;
		this.c_sched = new CScheduler(coverage_type, false);
		this.random_sched = new Random_Scheduler();
	}
	
	@Override
	public void reset()
	{
		c_sched.reset();
		random_sched.reset();
	}

	@Override
	public Decision getDecision(State state)
	{
		if (rm.nextDouble() < stochasticity)
		{
			return random_sched.getDecision(state);
		}
		else
		{
			return c_sched.getDecision(state);
		}
	}
	
    @Override
    public void loadMatchDetails(Match match, int agent_num, boolean mirror_match)
    {
    	super.loadMatchDetails(match, agent_num, mirror_match);
    	
    	c_sched.loadMatchDetails(match, agent_num, mirror_match);
    	random_sched.loadMatchDetails(match, agent_num, mirror_match);
    }
}
