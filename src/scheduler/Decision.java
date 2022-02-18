package scheduler;

public class Decision {
	
    public int iChoice;
    public int pChoice;
    public boolean forcedPass;

    public Decision(int iChoice, int pChoice, boolean forcedPass)
    {
    	this.iChoice = iChoice;
    	this.pChoice = pChoice;
    	this.forcedPass = forcedPass;
    }
}
