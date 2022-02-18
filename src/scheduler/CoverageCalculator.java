package scheduler;

import beliefbase.Condition;
import goalplantree.GoalNode;
import goalplantree.PlanNode;
import java.util.ArrayList;

/**
 * This calculator only works for plans which has a single literal as its precondition
 * If a plan has multiple preconditions then we need a model counter to calculate its coverage
 */

public class CoverageCalculator {


    public CoverageCalculator(){

    }

    /**
     * calculate the basic coverage of a given plan
     * @param plan
     * @return
     */
    private double calculateBC(PlanNode plan){
        // calculate the basic coverage
        double result = 1;
        Condition[] prec = plan.getPrec();
        int count = prec.length;

        if(prec.length > 1){
            System.err.println("The precondition contains more than two literals, please use model counter " +
                    "to calculate its coverage");
        }
        return result / Math.pow(2, count);
    }

    /**
     *
     * @param goal
     */
    public void calGoalCoverage(GoalNode goal){

        // set the extended coverage for this goal
        double result = 0;
        PlanNode[] plans = goal.getPlans();

        for(int i = 0; i < plans.length; i++){
            calPlanCoverage(plans[i]);
        }


        // if the plans are leaf plans
        ArrayList<Condition> checked = new ArrayList<>();
        //
        for(int i = 0; i < plans.length; i++){
            Condition prec = plans[i].getPrec()[0]; // assume the precondition of a plan only contains one literal
            for(int j = 0; j < checked.size(); j++){
                if(prec.isOpposite(checked.get(j))){
                    goal.setEc(1 * plans[0].getEc());
                    return;
                }
            }
            checked.add(prec); // add it to the checked list
        }
                // if there is no collision
        result = (Math.pow(2, plans.length) - 1) / Math.pow(2, plans.length) * plans[0].getEc();

        goal.setEc(result);
    }

    /**
     *
     * @param goal
     */
    public void calG(GoalNode goal){
        PlanNode[] plans = goal.getPlans();
        double result = 0;

        for(int i = 0; i < plans.length; i++){
            calPlanCoverage(plans[i]);
        }

        for(int i = 0; i < plans.length; i++){
            result += calCombCoverage(plans, i+1);
        }

        goal.setEc(result);


    }


    /**
     * calculate and set the basic and extended coverage of a given plan
     * @param plan
     */
    public void calPlanCoverage(PlanNode plan){
        // set basic coverage
        plan.setBc(calculateBC(plan));
        // check if it is a leave plan
        ArrayList<GoalNode> subgoals = plan.getSubgoals();
        if(subgoals.size() == 0){
            plan.setEc(1); // if so the extended coverage is set to 1
        }
        else{
            double result = 1;
            // calculate its extended coverage based on the subgoals in this plan
            for(int i = 0; i < subgoals.size(); i++){
                //calGoalCoverage(subgoals.get(i));
                calG(subgoals.get(i));
                result = result * subgoals.get(i).getEc();
            }
            plan.setEc(result);
        }
    }




    /**
     * given the set of plans to achieve a goal, and the number of selected plans, calculate the coverage of all
     * possible combinations of the selected plans, and return the sum of the coverage
     * @param plans
     * @param num
     * @return
     */
    public double calCombCoverage(PlanNode[] plans, int num){
        String[] input = new String[plans.length];

        for(int i = 0; i < plans.length; i++){
            input[i] = ""+ i;
        }

        //
        double result = 0;

        // get the possible combinations
        ArrayList<String> combs = combine(input, num);

        for(int i = 0; i < combs.size(); i++){
            String[] temp = combs.get(i).split(" ");
            ArrayList<Integer> indexes = new ArrayList<>();
            for(int j = 0; j < temp.length; j++){
                indexes.add(Integer.parseInt(temp[j]));
            }
            result += calCoverage(plans, indexes);
        }

        return result;
    }

    /**
     * given the list of selected plans, calculate the coverage when only the selected plans are applicable
     * @param plans
     * @param selected
     * @return
     */
    private double calCoverage(PlanNode[] plans, ArrayList<Integer> selected){
        // empty environment
        ArrayList<Condition> env = new ArrayList<>();

        for(int i = 0; i < plans.length; i++){
            // get its pre-condition
            Condition[] prec = plans[i].getPrec();

            // if this plan is one of the selected plan, then its precondition holds
            if(selected.contains(i)){
                for(int j = 0; j < env.size(); j++){
                    // we assume the plan's precondition is a single literal
                    if(env.get(j).isOpposite(prec[0])){
                        // if there is a contradiction
                        return 0;
                    }
                }
                // if they can be true at the same time
                env.add(prec[0]);
            }else {
                // if it is not in the selected plan then check its negation
                for(int j = 0; j < env.size(); j++){
                    if(env.get(j).isSame(prec[0])){
                        // if there is a contradiction
                        return 0;
                    }
                }
                // otherwise
                env.add( new Condition(prec[0].getLiteral(), !prec[0].isPositive()));
            }
        }

        // check the number of literals which are considered as the precondition of plans
        ArrayList<String> literals= new ArrayList<>();
        for(int i = 0; i < env.size(); i++){
            if(!literals.contains(env.get(i).getLiteral())){
                literals.add(env.get(i).getLiteral());
            }
        }

        // if there is the possibility that the precondition of the selected plans holds, and the other plans didn't
        double result = 1 / Math.pow(2, literals.size());

        double max = 0;
        // find the maximum ec of the selected plans
        for(int i = 0; i < selected.size(); i++){
            if(plans[selected.get(i)].getEc() > max){
                max = plans[selected.get(i)].getEc();
            }
        }

        return result * max;
    }


    /**
     * return all possible combinations given the set A and the number of elements
     * @param a
     * @param num
     * @return
     */
    private ArrayList<String> combine(String[] a, int num) {
        ArrayList<String> list = new ArrayList<String>();
        StringBuffer sb = new StringBuffer();
        String[] b = new String[a.length];

        for (int i = 0; i < b.length; i++) {
            if (i < num) {
                b[i] = "1";
            } else
                b[i] = "0";
        }

        int point = 0;
        int nextPoint = 0;
        int count = 0;
        int sum = 0;
        String temp = "1";
        while (true) {

            for (int i = b.length - 1; i >= b.length - num; i--) {
                if (b[i].equals("1"))
                    sum += 1;
            }

            for (int i = 0; i < b.length; i++) {
                if (b[i].equals("1")) {
                    point = i;
                    sb.append(a[point]);
                    sb.append(" ");
                    count++;
                    if (count == num)
                        break;
                }
            }

            list.add(sb.toString());


            if (sum == num) {
                break;
            }
            sum = 0;


            for (int i = 0; i < b.length - 1; i++) {
                if (b[i].equals("1") && b[i + 1].equals("0")) {
                    point = i;
                    nextPoint = i + 1;
                    b[point] = "0";
                    b[nextPoint] = "1";
                    break;
                }
            }

            for (int i = 0; i < point - 1; i++)
                for (int j = i; j < point - 1; j++) {
                    if (b[i].equals("0")) {
                        temp = b[i];
                        b[i] = b[j + 1];
                        b[j + 1] = temp;
                    }
                }

            sb.setLength(0);
            count = 0;
        }
        //
        return list;

    }

}