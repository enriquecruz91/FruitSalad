package fruit.g1;

import java.util.*;

public class Player extends fruit.sim.Player
{

    private Random random = new Random();
    
    private int nplayers;
    private int bowlsize; 
    private int[] preferences;
    private int[] uniform_platter;
    private int[] r0_seen_fruit;
    private int[] r1_seen_fruit;
    private int[] bowls_seen; 
    
    private double[] fruit_probs;

    private final int NUM_FRUITS = 12;
    private final int FIRST = 0;
    private final int SECOND = 1;
    
    public void init(int nplayers, int[] pref) {
        this.nplayers = nplayers;
        preferences = pref.clone();
        r0_seen_fruit = new int[preferences.length];
        r1_seen_fruit = new int[preferences.length];
        uniform_platter = new int[preferences.length];
        fruit_probs = new double[preferences.length];
        bowls_seen = new int[2];
    }

    public boolean pass(int[] bowl, int bowlId, int round,
                        boolean canPick,
                        boolean musTake) {
        
        // we add the new information to the list of frui we have seen
        // depending on which round we are on 
        bowlsize = 0;
        bowls_seen[round]++;
        for (int i=0; i < bowl.length; i++) {
            bowlsize += bowl[i];
            if (round == FIRST) {
                r0_seen_fruit[i] += bowl[i];
            }
            else {
                r1_seen_fruit[i] += bowl[i];
            }
        }

        // we try to infer the current platter using the information that we have
        
        // first we generate a platter
        // for now uniform, must improve on this
        int total_fruit = bowlsize*nplayers;
        for (int i=0; i < uniform_platter.length; i++) {
            uniform_platter[i] = Math.round(total_fruit/uniform_platter.length);
        }
        // update quantities based on observations
        updatePlatterQuantities(uniform_platter, round);
        disp(uniform_platter);



        if (!canPick || musTake) {
            return false;
        }
        else {
            int ev = calculateExpectedValue(uniform_platter);
            System.out.println("EV = " + ev + "\n");
            
            // compute the score of the bowl we received
            int score = scoreBowl(bowl);
            System.out.println("BOWL = " + score + "\n");

            // we do a linear interpolation based on the number of bowls we
            // will get to see and set the threshold for our decision based
            // on that
            double lin_range = 0.66; // why this value?
            int bowls_ill_see = nplayers - getIndex();
            int max_score = 12*bowlsize;
            double seg = (max_score-ev)*lin_range/bowls_ill_see;
            return score > (ev + seg*(bowls_ill_see - bowls_seen[round] -1));
        }        
    }

    // UPDATE A GIVEN PLATTERS QUANTITIES BY THE OBSERVATIONS WE HAVE MADE
    // IN THE GIVEN ROUND TO CALCULATE THE REMAINING FRUITS IN THE PLATTER
    private void updatePlatterQuantities(int[] platter, int round) {
        for (int i=0; i < platter.length; i++) {
            if (round == FIRST) {
                platter[i] -= r0_seen_fruit[i];
            }
            else {
                platter[i] -= r1_seen_fruit[i];
            }
        }
    }

    private int scoreBowl(int[] bowl) {
        int score = 0;
        for (int i = 0; i < preferences.length; i++) {
            score += bowl[i] * preferences[i];
        }
        return score;
    }

    private void disp(int[] bowl) {
        String str = "|";
        for (int i = 0; i < bowl.length; i++) {
            str += " " + bowl[i] + " |";
        }
        str += "\n";
        System.out.println(str);
    }

    // GIVEN A PLATTER EMULATING THE DISTRIBUTION OF THE SERVING BOWL
    // IT WILL CALCULATE THE EMPIRICAL EXPECTED VALUE ACCOUNTING FOR
    // CLUSTERING AND SERVING IN THE SAME MANNER AS THE SIMULATOR
    private int calculateExpectedValue(int[] platter) {
        int[] bowl;
        int total_score = 0;
        for (int i=0; i < 10000; i++) {
            bowl = createBowl(platter);
            total_score += scoreBowl(bowl);
        }
        return Math.round(total_score/10000);
    }

    // GENERATES A BOWL IN THE SAME FASHION THAT THE SIMULATOR DOES
    // TAKING INTO ACCOUNT CLUSTERING FACTOR
    private int[] createBowl(int [] platter)
    {
        int[] bowl = new int[NUM_FRUITS];
        int sz = 0;
        while (sz < bowlsize) {
            // pick a fruit according to current fruit distribution
            int fruit = pickFruit(platter); 
            int c = 1 + random.nextInt(3);
            c = Math.min(c, bowlsize - sz);
            c = Math.min(c, platter[fruit]);

            bowl[fruit] += c;
            sz += c;
        }
        return bowl;
    }

    // GIVEN A PLATER WITH A BUNCH OF QUANTITIES FOR EACH FRUIT
    // PICKS A FRUIT INDEX TO SERVER UNIFORMLY FRUIT THE ACTUAL BOWL
    // SAME WAY THE SIMULATOR ACTUALLY DOES IT
    private int pickFruit(int [] platter)
    {
        // generate a prefix sum
        int[] prefixsum = new int[NUM_FRUITS];
        prefixsum[0] = platter[0];
        for (int i = 1; i != NUM_FRUITS; ++i)
            prefixsum[i] = prefixsum[i-1] + platter[i];

        int currentFruitCount = prefixsum[NUM_FRUITS-1];
        // roll a dice [0, currentFruitCount)
        int rnd = random.nextInt(currentFruitCount);
        
        for (int i = 0; i != NUM_FRUITS; ++i)
            if (rnd < prefixsum[i])
                return i;

        assert false;

        return -1;
    }
       
}
