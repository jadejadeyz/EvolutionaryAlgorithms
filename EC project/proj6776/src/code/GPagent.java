package code;

import java.util.*;

/*
 * A genetic algorithm to find combinations for AI values.
 * This is an interface to the rest of Tetris:
 * they start by calling setAIValues() to let us set some values for the AI, then
 * they call sendScore() to give us what they got.
 */
public class GPagent {

	private Random rnd;
	private final boolean USE_GENETIC = false;

    private int generation = 0;

	// Parameter Configuration
	private final int population   = 10;
	private final int max_prog_len = 100;

	private final int calr_size    = 16 ;  // 0-15
	private final int regs_size    = 25 ;  // 0-24
	private final int cost_size    = 4  ;

	private int[][] individuals  = new int [population][max_prog_len];
	private int[]   program_size = new int [population];
	private long[]  fitness      = new long[population];

	private int[][] effParts     = new int [population][max_prog_len];
	private int[]   effSizes     = new int [population];

	private int[][] mates        = new int [population][max_prog_len];
	private int[]   mates_size   = new int [population];
	private int[]   mates_index  = new int [population];

	// keep track of effective instructions, and which instructions contains constant
	private int[][]   track_ins  = new int [population][max_prog_len];
	private int[][]   track_reg  = new int [population][regs_size+cost_size];
	private double[]  p_const    = new double [population];

	private int[] best_prog = new int [max_prog_len];
	private int   best_prog_size = 0;
	private long  best_fitness;

	int current = 0;   // this used to indicate which one is the player now
	
	GPagent(TetrisEngine tetris){

		rnd = new Random();

		// initialize the population
		for(int i = 0; i < population; i++){

			int op, lh, ra, rb;

		    program_size[i] = rnd.nextInt(20) + 70;
			fitness[i] = 0;

		    for (int j = 0; j < program_size[i]; j++)
            {
				int func_size = 4;
				op = rnd.nextInt(func_size);		   			// 0-3
				lh = rnd.nextInt(calr_size);		    		// 0-15
				if (rnd.nextInt(2) == 0)
					ra = rnd.nextInt(regs_size);
				else
					ra = rnd.nextInt(4) + regs_size;

				if (ra >= regs_size)  // ra is a constant
					rb = rnd.nextInt(regs_size);				// 0-24
				else
				{
					if (rnd.nextInt(2) == 0)
						rb = rnd.nextInt(regs_size);
					else
						rb = rnd.nextInt(4) + regs_size;
				}

                individuals[i][j] |= (op << 24);
                individuals[i][j] |= (lh << 16);
                individuals[i][j] |= (ra << 8);
				individuals[i][j] |=  rb;
            }

            // tracking which instructions are effective
			track_reg[i][0] = 1;
		    tracking(i);

			// Save effective code for each individual
			intro_elimination(i);
		}
	}

	private void tracking(int child)
	{
		int lh, ra, rb;
		int const_num = 0;

		for (int i = program_size[child] - 1; i >= 0; i--)
		{
			lh = (individuals[child][i] >> 16) & 0xFF;
			ra = (individuals[child][i] >> 8)  & 0xFF;
			rb = (individuals[child][i])       & 0xFF;

			if (track_reg[child][lh] == 1)
			{
				track_ins[child][i] = 1;
				if (ra < regs_size)
				{
					track_reg[child][ra] = 1;
				}
				else		// ra is a constant
				{
					track_ins[child][i]++;
					const_num++;	// in order to computer p_const for each program.
				}
				if (rb < regs_size)
				{
					track_reg[child][rb] = 1;
				}
				else
				{
					track_ins[child][i]++;
					const_num++;
				}
			}
			else {
				// this is a non-effective instruction, but we still need it to compute p_const
				if (ra > regs_size || rb > regs_size) {
					const_num++;
					track_ins[child][i]--;
				}
			}
		}
		p_const[child] = const_num / program_size[child];
	}

	// crossover
	private void crossover(int mom, int dad)
	{
		int len_max = max_prog_len;
		int len_min = 3;

		int ls_max = 10;
		int dc_max = 5;
		int ds_max = 5;

		int spos1, spos2;
		int lseg1, lseg2;

		int shorterOne, longerOne;

		if (Math.min(mates_size[mom], mates_size[dad]) == mates_size[mom]) {
			shorterOne = mom;
			longerOne = dad;
		}
		else {
			shorterOne = dad;
			longerOne = mom;
		}

		// random select instruction positions
		do{
			spos1 = rnd.nextInt(mates_size[shorterOne]);
			spos2 = rnd.nextInt(mates_size[longerOne]);
		} while ( Math.abs(spos1 - spos2) > Math.min(mates_size[shorterOne] - 1, dc_max));

		// select an instruction segments
		lseg1 = rnd.nextInt(Math.min(ls_max, mates_size[shorterOne]-spos1)) + 1;
		do{
			lseg2 = rnd.nextInt(Math.min(ls_max, mates_size[longerOne]-spos2)) + 1;
		} while (Math.abs(lseg1 - lseg2) > ds_max || lseg2 < lseg1);

		if (mates_size[longerOne] - (lseg2 - lseg1) < len_min || mates_size[shorterOne] + (lseg2 - lseg1) > len_max)
		{
			if (rnd.nextInt(2) == 0) {
				lseg2 = lseg1;
			}
			else
			{
				lseg1 = lseg2;
			}
			if (spos1 + lseg1 > mates_size[shorterOne]) {
				lseg1 = mates_size[shorterOne] - spos1;
				lseg2 = lseg1;
			}
		}

		int[] child1 = new int[max_prog_len];
		int[] child2 = new int[max_prog_len];

		// child of mom
		System.arraycopy(mates[shorterOne], 0, child1, 0, spos1);
		System.arraycopy(mates[longerOne], spos2, child1, spos1, lseg2);
		System.arraycopy(mates[shorterOne], spos1+lseg1, child1, spos1+lseg2, mates_size[shorterOne]-spos1-lseg1);

		// child of dad
		System.arraycopy(mates[longerOne], 0, child2, 0, spos2);
		System.arraycopy(mates[shorterOne], spos1, child2, spos2, lseg1);
		System.arraycopy(mates[longerOne], spos2+lseg2, child2, spos2+lseg1, mates_size[longerOne]-spos2-lseg2);

		program_size[shorterOne] = mates_size[shorterOne] - lseg1 + lseg2;
		program_size[longerOne]  = mates_size[longerOne] - lseg2 + lseg1;

		Arrays.fill(individuals[shorterOne], 0);
		Arrays.fill(individuals[longerOne], 0);

		System.arraycopy(child1, 0, individuals[shorterOne], 0, program_size[shorterOne]);
		System.arraycopy(child2, 0, individuals[longerOne], 0, program_size[longerOne]);
	}

    // mutation, mutate on a copy of the original individual and then copy back to the population
    private void pure_mutation(int mateKid)
    {
        int picked_instruction, picked_element;
        int ra, rb;

		picked_instruction = rnd.nextInt(mates_size[mateKid]);  // random pick one instruction
		picked_element     = rnd.nextInt(4);                			   // <op, lh, ra, ra> uniformly select one

		// get the operands before mutation
        ra = (mates[mateKid][picked_instruction] >> 8) & 0xFF;
        rb = (mates[mateKid][picked_instruction]     ) & 0xFF;

        if (picked_element == 0)	// mutate function
        {
			mates[mateKid][picked_element] &= 0x00FFFFFF;
			mates[mateKid][picked_element] |= (rnd.nextInt(4) << 24);
        }
		if (picked_element == 1)	// mutate destination register
		{
			mates[mateKid][picked_instruction] &= 0xFF00FFFF;
			mates[mateKid][picked_instruction] |= (rnd.nextInt(calr_size) << 16);
		}
		if (picked_element == 2)	// mutate operand 1
		{
			mates[mateKid][picked_instruction] &= 0xFFFF00FF;
			if (rb < regs_size)     // guarantee that there must be at least one variable register
			{
				// uniformly select a new operand from variable register or constant register
				if (rnd.nextInt(2) == 0) 	// new operand selected from variable register
					mates[mateKid][picked_instruction] |= (rnd.nextInt(regs_size) << 8);
				else
					mates[mateKid][picked_instruction] |= ((rnd.nextInt(4) + regs_size) << 8);
			}
			else			// the other non-mutate operand is a constant, thus new picked must select from registers
			{
				mates[mateKid][picked_instruction] |= (rnd.nextInt(regs_size) << 8);
			}
		}
		if (picked_element == 3)	// mutate operand 2
		{
			mates[mateKid][picked_instruction] &= 0xFFFFFF00;
			// similar to mutate operand 1
			if (ra < regs_size) {
				if (rnd.nextInt(2) == 0)
					mates[mateKid][picked_instruction] |= (rnd.nextInt(regs_size));
				else
					mates[mateKid][picked_instruction] |= (rnd.nextInt(4) + regs_size);
			}
			else {
				mates[mateKid][picked_instruction] |= (rnd.nextInt(regs_size));
			}
		}
    }

    /*
    private void effmut(int child)
	{
		int rnd_ins, rnd_seg;
		int ra, rb;

		do {
			rnd_ins = rnd.nextInt(mate_pg_size[child]);
		} while (tracking[child][rnd_ins] < 1);

		rnd_seg = rnd.nextInt(2);     // which part to change

		ra = (mating[child][rnd_ins] >> 8) & 0xFF;
		rb = (mating[child][rnd_ins]     ) & 0xFF;

		if (rnd_seg == 0)	// mutate function
		{
			mating[child][rnd_ins] &= 0x00FFFFFF;
			mating[child][rnd_ins] |= (rnd.nextInt(4) << 24);
		}

		if (rnd_seg == 1)	// mutate register
		{
			// which one: destination or operand?
			if (rnd.nextInt(2) == 0)  // destination
			{
				mating[child][rnd_ins] &= 0xFF00FFFF;
				mating[child][rnd_ins] |= (rnd.nextInt(calr_size) << 16);
			}
			else		// operand
			{
				int temp;
				if (rnd.nextFloat() < p_const[mate_index[child]]) {    // select from constant
					temp = rnd.nextInt(4);

					if (ra >= regs_size) // using the new const to replace ra
					{
						mating[child][rnd_ins] &= 0xFFFF00FF;
						mating[child][rnd_ins] |= ((temp + regs_size) << 8);
					}
					if (rb >= regs_size)  // replace rb
					{
						mating[child][rnd_ins] &= 0xFFFFFF00;
						mating[child][rnd_ins] |= (temp + regs_size);
					}
					if (ra < regs_size && rb < regs_size) // pick one to replace
					{
						if (rnd.nextInt(2) == 0)		//replace ra
						{
							mating[child][rnd_ins] &= 0xFFFF00FF;
							mating[child][rnd_ins] |= ((temp + regs_size) << 8);
						}
						else		// repalce rb
						{
							mating[child][rnd_ins] &= 0xFFFFFF00;
							mating[child][rnd_ins] |= (temp + regs_size);
						}
					}
				}
				else	// select a new register
				{
					temp = rnd.nextInt(regs_size);
					if (rnd.nextInt(2) == 0)		//replace ra
					{
						mating[child][rnd_ins] &= 0xFFFF00FF;
						mating[child][rnd_ins] |= (temp << 8);
					}
					else		// replace rb
					{
						mating[child][rnd_ins] &= 0xFFFFFF00;
						mating[child][rnd_ins] |= temp;
					}
				}
			}
		}

		Arrays.fill(individuals[child], 0);
		System.arraycopy(mating[child], 0, individuals[child], 0, mate_pg_size[child]);

	}
	*/

	private void updatePopulation(){

		int tk = 5;

		// Before doing variation, get the best one of the old population
		best_fitness = fitness[0];
		for (int i = 0; i < population; i++)
		{
			if(best_fitness < fitness[i])
				best_fitness = fitness[i];
		}

		// Parent selection: tournament selection
		//Arrays.fill(mates, 0);
		double judgeBar;
		int winner = 0, member;
		for (int i = 0; i < population; i++){
			judgeBar = Double.NEGATIVE_INFINITY;

			for (int j = 0; j < tk; j++){
				member = rnd.nextInt(population);
				if (fitness[member] > judgeBar) {
					winner = member;
					judgeBar = fitness[member];
				}
			}

			for (int j = 0; j < max_prog_len; j++)
				mates[i][j] = individuals[winner][j];

			mates_size[i]  = program_size[winner];
			mates_index[i] = winner;
		}

		// debug on 2018.3.22, just use all purpose mutation
		for (int i = 0; i < population; i++)
		{
			if (rnd.nextDouble() < 0.9)
				pure_mutation(i);
		}

		System.arraycopy(mates, 0, individuals, 0, population);
		System.arraycopy(mates_size, 0, program_size, 0, population);


		// Do variation, either mutation or crossover
		/*
		for (int i = 0; i < population; i++)
		{
			if (rnd.nextInt(2) >= 10)		// crossover
			{
				crossover(i, (i+1)%population);
				i = i + 2;
			}
			else {
				//mutation(i, i);
				effmut(i);
			}

		}*/

        for (int i = 0; i < population; i++) {
			tracking(i);
			intro_elimination(i);
		}

		System.out.println("Generation " + generation + "; best fitness = " + best_fitness);

		generation++;
		current = 0;
	}

	private void intro_elimination(int child)
	{
		int[] Reff = new int[21];
		int next = 0;
		int lf, ra, rb;
		int curLen = 0;

		Reff[0] = 1;
		for (int i = 1; i < 21; i++)
			Reff[i] = 0;

		// Scan bottom-up
		for(int i = program_size[child]-1; i >= 0; i--)
		{
			lf = (individuals[child][i] >> 16) & 0xFF;
			ra = (individuals[child][i] >>  8) & 0xFF;
			rb = (individuals[child][i]      ) & 0xFF;

			if (Reff[lf] == 1)
			{
				effParts[child][next] = individuals[child][i];
				curLen++;
				if (ra < 21)
					Reff[ra] = 1;
				if (rb < 21)
					Reff[rb] = 1;

				next++;
			}
		}

		effSizes[child] = curLen;
	}

	void setAIValues(TetrisAI ai){

		if(!USE_GENETIC)
			return;

		ai.prog_size = effSizes[current];

		for (int i = 0; i < effSizes[current]; i++)
		    ai.effectiveCode[effSizes[current]-1-i] = effParts[current][i];
	}
	
	void sendScore(int score){

		if(!USE_GENETIC)
		    return;

		String s = "Generation " + generation + "; GP policy " + (current+1) + " Fitness (Game Scores) = " + score;
		System.out.println(s);
		System.out.println("Instructions:");
		System.out.println(Arrays.toString(individuals[current]));

		fitness[current] = score;
		current++;

		if(current == population)
            updatePopulation();
	}
	
	// Double array to string, two decimal places
	private String aToS(double[] a){
		String s = "";
		for(int i = 0; i < a.length; i++)
		{
			s += Double.toString(((double)Math.round(a[i]*100))/100);
			if(i != a.length-1) s += ", ";
		}
		return "[" + s + "]";
	}
}
