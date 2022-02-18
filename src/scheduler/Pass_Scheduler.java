package scheduler;

public class Pass_Scheduler extends Scheduler {

	@Override
	public void reset()
	{
		// Nothing to reset for this scheduler
	}
	
	@Override
	public Decision getDecision(State state)
	{
		return new Decision(-1, -1, true);
	}
}
