package code;

import static code.ProjectConstants.sleep_;
import static code.ProjectConstants.GameState;
import java.util.*;
import java.lang.*;

/*
 * Tetris AI communicates with tetris engiens, and it knowns the current and next blocks.
 * After calculating reward for each state, it sent a keycontrol to the tetris engine.
 */

public class TetrisAI
{
	private TetrisEngine engine;
	volatile boolean flag = false;
	AIThread thread;

	private double[] registers = new double[29];

	// Time (ms) AI has to wait per keypress
	public static final int waittime = 8;
	// Maximum speed without crashing: (waittime = 1, direct_drop = true)
	private static final boolean direct_drop = false;

	// Evaluation program: it is a GP program
	int prog_size = 74;
	int[] effectiveCode = {787728, 50535184, 330778, 66569, 34408961, 16915456, 988434, 33691674, 51059204, 33688068, 16849411, 16847641, 51059475, 34216471, 70940, 17632027, 50403608, 51122201, 50665984, 69403, 793091, 51255557, 34544398, 923909, 17173529, 793365, 51255815, 50662924, 138240, 50796824, 50993682, 33692418, 50600973, 50731785, 51256337, 50337550, 33888535, 17307674, 50399764, 132113, 50797062, 34544384, 530964, 68122, 34020098, 50665986, 399626, 50922261, 594948, 17433628, 34478610, 51321096, 17503002, 34407693, 662036, 17504024, 17568011, 17040136, 6928, 34542095, 16783884, 72707, 51055383, 34215708, 34148121, 33756700, 33887259, 596229, 396825, 17439245, 6401, 989461, 50863117, 16784136};

    TetrisAI(TetrisPanel panel)
	{
		engine = panel.engine;
		thread = new AIThread();

		// constant registers
		registers[25] = -1;
		registers[26] = -2;
		registers[27] = 2;
		registers[28] = 1;

		// Initialize half of the calculation registers using input registers
		Random rnd = new Random();
		for (int i = 0; i < 16; i++)
			registers[i] = 0.5;

        System.out.println(Arrays.toString(registers));
	}
	
	public void send_ready(){
		if(!flag){
			thread.start();
			flag = true;
			engine.lastnewblock = System.currentTimeMillis();
		}
	}
	
	class AIThread extends Thread{
		public void run(){
			
			while(flag)
			{
				try{
					if(engine.state == GameState.PLAYING){
						if(engine.activeblock == null)
							continue;
						
						BlockPosition temp = computeBestFit(engine);
						if(engine.state == GameState.PLAYING){
							
							int elx = temp.bx;
							int erot = temp.rot;

							drophere(elx, erot);
						}
					}

					sleep_(waittime);
				}catch(Exception e){
					System.out.print("Taking too much time! \n");
				}
			}
			
		}
		
		/* Keypresses to move block to calculated position. */
		private void drophere(int finx, int finrot){
			int st_blocksdropped = engine.blocksdropped;
			int init_state = engine.activeblock.rot;
			int prev_state = init_state;

			while(flag && engine.activeblock.rot != finrot)
			{
				// Rotate first so we don't get stuck in the edges.
				engine.keyrotate();
				
				//Now wait.
				sleep_(waittime);
				
				if(prev_state == engine.activeblock.rot || init_state == engine.activeblock.rot){
					engine.keyslam();
					sleep_(waittime>3? waittime : 3);
				}
				prev_state = engine.activeblock.rot;
			}
			
			prev_state = engine.activeblock.x;
			while(flag && engine.activeblock.x != finx){
				//Now nudge the block.
				if(engine.activeblock.x < finx){
					engine.keyright();
				}
				else if(engine.activeblock.x > finx){
					engine.keyleft();
				}
				
				sleep_(waittime);
				
				if(prev_state == engine.activeblock.x){
					engine.keyslam();
					sleep_(waittime>3? waittime : 3);
				}
				prev_state = engine.activeblock.x;
			}
			
			if(flag && direct_drop){
				engine.keyslam();
				// make the minimum 3 to fix a weird threading glitch
				sleep_(waittime>3? waittime : 3);
				return;
			}
			
			while(flag && engine.blocksdropped == st_blocksdropped){
				//Now move it down until it drops a new block.
				engine.keydown();
				sleep_(waittime);
			}
		}
	}

	// =============== Here be the Tetris Player AI code. ===============
	/* This can calculate the best possible fit for it given the current
	 * state the blocks are in. */
	private BlockPosition computeBestFit(TetrisEngine ge){

		byte[][][] allrotations = TetrisEngine.blockdef[ge.activeblock.type];
		int nrots = allrotations.length;

		// List of all the possible fits.
		List<BlockPosition> posfits = new ArrayList<>();

		// Loop through the rotations.
		// Here we generate all of the unique valid fits, and evaluate them later.
		for(int i = 0; i < nrots; i++)
		{
			byte[][] trotation = allrotations[i];
			int free = freeSpaces(trotation);
			int freeL = free / 10;
			int freeR = free % 10;
			int minX = 0 - freeL;
			int maxX = (ge.width-4) + freeR;
			// now loop through each position for a rotation.
			for(int j = minX; j <= maxX; j++){
				BlockPosition put = new BlockPosition();
				put.bx = j;
				put.rot = i;
				posfits.add(put);
			}
		}
		
		// Do everything again for the next block
		byte[][][] allrotations2 = TetrisEngine.blockdef[ge.nextblock.type];
		int nrots2 = allrotations2.length;
		List<BlockPosition> posfits2 = new ArrayList<>();

		for(int i = 0; i < nrots2; i++){
			byte[][] trotation = allrotations2[i];
			int free = freeSpaces(trotation);
			int freeL = free / 10;
			int freeR = free % 10;
			int minX = 0 - freeL;
			int maxX = (ge.width-4) + freeR;
			for(int j=minX; j<=maxX; j++){
				BlockPosition put = new BlockPosition();
				put.bx = j;
				put.rot = i;
				posfits2.add(put);
			}
		}

		// now we begin the evaluation.
		// for each element in the list we have, calculate a score, and pick the best
		double[] rewards = new double[posfits.size() * posfits2.size()];

		for(int i = 0; i < posfits.size(); i++)
		{
			for(int j = 0; j < posfits2.size(); j++){
				rewards[i*posfits2.size()+j] = evalPosition(ge, posfits.get(i), posfits2.get(j));
			}
		}

		//retrieve the move with highest rewards.
		double max = rewards[0];
		BlockPosition max_b = null;
		for(int i = 0; i < rewards.length; i++){
			if(rewards[i] >= max){
				max_b = posfits.get(i/posfits2.size());
				max = rewards[i];
			}
		}

		// Return final position.
		return max_b;
	}

	// Evaluate position not with one, but with two blocks.
	private double evalPosition(TetrisEngine ge, BlockPosition p, BlockPosition q)
	{
		// First thing: Simulate the drop. Do this on a mock grid.
		byte[][] mockgrid = new byte[ge.width][ge.height];
		for(int i = 0; i < ge.width; i++)
		{
			for (int j = 0; j < ge.height; j++) {
				byte s = (byte) ge.blocks[i][j].getState();
				if (s == 2)
					s = 0;
				mockgrid[i][j] = s;
			}
		}

		int cleared = 0;
		for(int block = 1; block <= 2; block++)
		{
			
			byte[][] bl;
			BlockPosition r;
			
			if(block == 1)
				r = p;
			else r = q;
			
			if(block == 1)
				bl = TetrisEngine.blockdef[ge.activeblock.type][r.rot];
			else bl = TetrisEngine.blockdef[ge.nextblock.type][r.rot];

			// Now we find the fitting height by starting from the bottom and
			// working upwards. If we're fitting a line-block on an empty
			// grid then the height would be height-1, and it can't be any
			// lower than that, so that's where we'll start.
	
			int h;
			for(h = ge.height - 1; ; h--){
	
				// indicator. 1: fits. 0: doesn't fit. -1: game over.
				int fit_state = 1;
	
				for(int i = 0; i < 4; i++)
					for(int j = 0; j < 4; j++){
						//check for bounds.
	
						boolean block_p = bl[j][i] >= 1;
	
						//we have to simulate lazy evaluation in order to avoid
						//out of bounds errors.
	
						if(block_p){
							//still have to check for overflow. X-overflow can't
							//happen at this stage but Y-overflow can.
	
							if(h+j >= ge.height)
								fit_state = 0;
	
							else if(h+j < 0)
								fit_state = -1;
	
							else{
								boolean board_p = mockgrid[i+r.bx][h+j] >= 1;
	
								// Already filled, doesn't fit.
								if(board_p)
									fit_state = 0;
	
								// Still the possibility that another block
								// might still be over it.
								if(fit_state == 1){
									for(int h1 = h+j-1; h1 >= 0; h1--) {
										if (mockgrid[i + r.bx][h1] >= 1) {
											fit_state = 0;
											break;
										}
									}
								}
							}
						}
					}
	
				//We don't want game over so here:
				if(fit_state == -1)
					return -99999999;
				
				//1 = found!
				if(fit_state == 1)
					break;
	
			}
	
			// copy over block position
			for(int i = 0; i < 4; i++)
				for(int j = 0; j < 4; j++)
					if(bl[j][i] == 1)
						mockgrid[r.bx+i][h+j] = 2;
			
			// check for clears
			boolean foundline;
			do{
				foundline = false;

				CHECKHERE:
				for(int i = mockgrid[0].length-1;i >= 0; i--)
				{
					for (byte[] aMockgrid : mockgrid) {
						if (!(aMockgrid[i] > 0))
							continue CHECKHERE;
					}
					
					// line i is full, clear it and copy
					cleared++;
					foundline = true;
					for(int a = i; a > 0; a--)
					{
						for(int y = 0; y < mockgrid.length; y++)
						{
							mockgrid[y][a] = mockgrid[y][a-1];
						}
					}
					break CHECKHERE;
				}
			}while(foundline);
		}

		// Now we evaluate the resulting position.
		int   holes = 0;
		int   curhole = 0;
		int   blockades = 0;
		int   cols_with_holes = 0;
		int   touching_walls = 0;
		int   touching_edges = 0;
		int   touching_floors = 0;
		int[] each_col_height = new int[ge.width];
		int   roughness = 0;

		//horizontal pairs
		for(int i = 0; i < ge.height; i++) {
			for (int j = 0; j < ge.width - 1; j++) {
				if (j == 0 && mockgrid[j][i] == 2)
					touching_walls++;
				if (j + 1 == ge.width - 1 && mockgrid[j + 1][i] == 2)
					touching_walls++;
				if (mockgrid[j][i] + mockgrid[j + 1][i] >= 3)
					touching_edges++;
			}
		}

		//vertical pairs
		for(int i = 0; i < ge.width; i++) {
			for (int j = 0; j < ge.height - 1; j++) {
				if (j + 1 == ge.height - 1 && mockgrid[i][j + 1] == 2)
					touching_floors++;
				if (mockgrid[i][j] + mockgrid[i][j + 1] >= 3)
					touching_edges++;
			}
		}

		// Height
		for (int i = 0; i < ge.width; i++)
		{
			boolean bf = false;
			for (int j = 0; j < ge.height; j++)
			{
				if (mockgrid[i][j] > 0)
					bf = true;
				if (bf && mockgrid[i][j] > 0) {
					each_col_height[i] = ge.height - j - 1;
					break;
				}
			}
		}

		for(int i = 0; i < ge.width; i++) {
			// Part 1: Count how many holes (space beneath blocks)
			boolean f = false;
			for(int j = 0; j < ge.height; j++) {
				curhole = 0;
				if(mockgrid[i][j] > 0) f = true;
				if(f && mockgrid[i][j] == 0)
				{
					holes++;
					curhole++;
				}
			}

			if (curhole > 0)
				cols_with_holes++;

			// Part 2: Count how many blockades (block above space)
			f = false;
			for(int j = ge.height-1; j >= 0; j--){
				if(mockgrid[i][j]==0)
					f=true;
				if(f&&mockgrid[i][j]>0)
					blockades++;
			}
		}

		for (int i = 0; i < ge.width-1; i++){
			roughness += Math.abs(each_col_height[i] - each_col_height[(i+1)]);
		}

		// MAX, MIN, AVERAGE height
		int hmax = each_col_height[0];
		int hmin = each_col_height[0];
		int hmean = 0;

		for (int i = 0; i < ge.width; i++)
		{
			if (each_col_height[i] > hmax)
				hmax = each_col_height[i];
			if(each_col_height[i] < hmin)
				hmin = each_col_height[i];
			hmean += each_col_height[i];
		}

		// number of clears in the current turn
		registers[16] = cleared;
		// number of holes in the current state
		registers[17] = holes;
		// number of blockades
		registers[18] = blockades;
		// Roughness: the sum of height difference
		registers[19] = roughness;
		// Feature 5: number of columns with holes
		registers[20] = cols_with_holes;
		// Feature 6: number of touching edges
		registers[21] = touching_edges;
		// Feature 7: number of touching walls
		registers[22] = touching_walls;
		// Feature 8: number of touching floors
		registers[23] = touching_floors;
		// mean height
		registers[24] = hmean / ge.width;

		// Compute reward for each state
		int op, lh, ra, rb;
		for (int i = 0; i < prog_size; i++)
		{
			op = (effectiveCode[i] >> 24) & 0xFF;
			lh = (effectiveCode[i] >> 16) & 0xFF;
			ra = (effectiveCode[i] >> 8) & 0xFF;
			rb = (effectiveCode[i]) & 0xFF;

			switch(op)
			{
				case 0:	// '+'
				{
					registers[lh] = registers[ra] + registers[rb];
					break;
				}
				case 1:	// '-'
				{
					registers[lh] = registers[ra] - registers[rb];
					break;
				}
				case 2: // '*'
				{
					registers[lh] = registers[ra] * registers[rb];
					break;
				}
				case 3: // '/' protected
				{
					if (registers[rb] != 0.0) {
						registers[lh] = registers[ra] / registers[rb];
					}
					else registers[lh] = 1.0;
					break;
				}
			}
		}

		return registers[0];
	}

	// Takes a int array and calculates how many blocks of free spaces are there
	// on the left and right. The return value is a 2 digit integer.
	private static int freeSpaces(byte[][] in){

		// It's free if all of them are zero, and their sum is zero.
		boolean c1free = in[0][0] + in[1][0] + in[2][0] + in[3][0] == 0;
		boolean c2free = in[0][1] + in[1][1] + in[2][1] + in[3][1] == 0;
		boolean c3free = in[0][2] + in[1][2] + in[2][2] + in[3][2] == 0;
		boolean c4free = in[0][3] + in[1][3] + in[2][3] + in[3][3] == 0;

		int lfree = 0;
		// Meh, I'm too lazy to code a loop for this.
		if(c1free){
			lfree++;
			if(c2free){
				lfree++;
				if(c3free){
					lfree++;
					if(c4free){
						lfree++;
		} } } }

		int rfree = 0;
		if(c4free){
			rfree++;
			if(c3free){
				rfree++;
				if(c2free){
					rfree++;
					if(c1free){
						rfree++;
		} } } }

		return lfree * 10 + rfree;
	}
}

class BlockPosition{
	int bx;
	int rot;
}
