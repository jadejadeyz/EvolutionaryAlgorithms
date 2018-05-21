package code;

import java.awt.*;
import java.util.*;
import code.SoundManager.Sounds;

import static code.Block.colors;
import static code.ProjectConstants.*;

/* This class calculates most of the block positions,
 * rotations, etc, although the TetrisPanel object
 * still keeps track of the concrete block coordinates.
 * This class will change variables in the TetrisPanel class.
 */
public class TetrisEngine
{
	/* Bunch of hardcoded blocks and their rotations.
	 * Code them high up in the array so that when you
	 * get a new one it appears in the highest spot 
	 * possible.
	 */
	public static final byte[][][][] blockdef =
	{{
		// 0 = I block.
		{
			{ 1, 1, 1, 1 },
			{ 0, 0, 0, 0 },
			{ 0, 0, 0, 0 },
			{ 0, 0, 0, 0 } },
			
			{
			{ 0, 1, 0, 0 },
			{ 0, 1, 0, 0 },
			{ 0, 1, 0, 0 },
			{ 0, 1, 0, 0 } } },
			
			
		// 1 = o block
		{
			{
			{ 0, 1, 1, 0 },
			{ 0, 1, 1, 0 },
			{ 0, 0, 0, 0 },
			{ 0, 0, 0, 0 } } },
			
			
		// 2 = L block
		{
			{
			{ 0, 1, 0, 0 },
			{ 0, 1, 0, 0 },
			{ 0, 1, 1, 0 },
			{ 0, 0, 0, 0 } },
			
			{
			{ 0, 0, 1, 0 },
			{ 1, 1, 1, 0 },
			{ 0, 0, 0, 0 },
			{ 0, 0, 0, 0 } },
			
			{
			{ 1, 1, 0, 0 },
			{ 0, 1, 0, 0 },
			{ 0, 1, 0, 0 },
			{ 0, 0, 0, 0 } },
			
			{
			{ 1, 1, 1, 0 },
			{ 1, 0, 0, 0 },
			{ 0, 0, 0, 0 },
			{ 0, 0, 0, 0 } } },
			
			
		// 3 = J block
		{
			{
			{ 0, 0, 1, 0 },
			{ 0, 0, 1, 0 },
			{ 0, 1, 1, 0 },
			{ 0, 0, 0, 0 } },

			{
			{ 1, 1, 1, 0 },
			{ 0, 0, 1, 0 },
			{ 0, 0, 0, 0 },
			{ 0, 0, 0, 0 } },

			{
			{ 0, 1, 1, 0 },
			{ 0, 1, 0, 0 },
			{ 0, 1, 0, 0 },
			{ 0, 0, 0, 0 } },

			{
			{ 1, 0, 0, 0 },
			{ 1, 1, 1, 0 },
			{ 0, 0, 0, 0 },
			{ 0, 0, 0, 0 } } },
			
			
		// 4 = T block
		{
			{
			{ 0, 1, 0, 0 },
			{ 1, 1, 1, 0 },
			{ 0, 0, 0, 0 },
			{ 0, 0, 0, 0 } },

			{
			{ 0, 1, 0, 0 },
			{ 0, 1, 1, 0 },
			{ 0, 1, 0, 0 },
			{ 0, 0, 0, 0 } },

			{
			{ 1, 1, 1, 0 },
			{ 0, 1, 0, 0 },
			{ 0, 0, 0, 0 },
			{ 0, 0, 0, 0 } },

			{
			{ 0, 1, 0, 0 },
			{ 1, 1, 0, 0 },
			{ 0, 1, 0, 0 },
			{ 0, 0, 0, 0 } } 
		},
			
			
		// 5 = S block
		{
			{
			{ 0, 1, 1, 0 },
			{ 1, 1, 0, 0 },
			{ 0, 0, 0, 0 },
			{ 0, 0, 0, 0 } },

			{
			{ 0, 1, 0, 0 },
			{ 0, 1, 1, 0 },
			{ 0, 0, 1, 0 },
			{ 0, 0, 0, 0 } } 
		},
			
		
		// 6 = Z block
		{
			{
			{ 0, 1, 1, 0 },
			{ 0, 0, 1, 1 },
			{ 0, 0, 0, 0 },
			{ 0, 0, 0, 0 } },
			
			{
			{ 0, 0, 1, 0 },
			{ 0, 1, 1, 0 },
			{ 0, 1, 0, 0 },
			{ 0, 0, 0, 0 } 
		} 
	}};
	
	/* Reference to the TetrisPanel containing this object;*/
	private TetrisPanel tetris;

	/*Random object used to generate new blocks.*/
	private Random rdm;
	
	/* Primitive representation of active block. */
	volatile Tetromino activeblock;

	/* Next block. */
	volatile Tetromino nextblock = null;
	
	/* Time of previous step. */
	private long laststep = System.currentTimeMillis();
	
	/* Thread to run for the game. */
	private Thread gamethread;
	
	/* Size of Tetris window, in pixels. */
	private Dimension bounds;
	
	/* Width and height of the grid, counted in number
	 * of blocks.*/
	public int width = 10, height = (int) (1.8*width);
	
	/* Dimensions (Width and height) of each square. Squares in
	 * Tetris must be the same height and width.*/
	private int squaredim= 300/width;

	// Initialize a DBlock array and set all its contents


	// to DBlock.EMPTY.
	/* DBlock array representation of the gamefield. Blocks are
	 * counted X first starting from the top left: blocks[5][3]
	 * would be a block 5 left and 3 down from (0,0).*/
	public volatile Block[][] blocks = new Block[width][height];
	
	/* Score */
	private int score = 0;
	
	/* Level */
	public int level = 0;
	
	/* Lines cleared */
	private int lines = 0;
	
	/* How many blocks were dropped so far? */
	public int blocksdropped = 0;
	
	/* Maximum time allowed per step in milliseconds. */
	private int steptime = 500;
	
	/* Time used to fade block that have been cleared. */
	private int fadetime = 0;

	/* Current state of the game (PLAYING, PAUSED, etc.) */
	public volatile GameState state;
	
	/* How many lines did the AI get last time? */
	private int lastlines = 0;
	
	long lastnewblock = System.currentTimeMillis();

	private boolean anomaly_flag = false;

	/* Public constructor. Remember to call startengine()
	 * or else this won't do anything!
	 * @param p TetrisPanel.*/
	TetrisEngine(TetrisPanel p)
	{
		// Bounds changed to be thus:
		bounds = new Dimension(squaredim * width,squaredim * height);

		for(int t1 = 0; t1 < blocks.length; t1++)
		{
			for(int t2 = 0; t2 < blocks[t1].length; t2++)
			{
				blocks[t1][t2] = new Block(Block.EMPTY);
			}
		}
		
		// Initialize objects.
		tetris = p;
		rdm = new Random();
		
		// Initialize game thread.
		gamethread = new Thread(() -> {
            while (true) {

                long timeelapsedsincelaststep = System.currentTimeMillis() - laststep;

                //Took too much CPU.
                sleep_(steptime / 2);

                //Break loop if game isn't even playing.
                //Best to put AFTER sleeping.
                synchronized (TetrisEngine.this) {
                    if (!(state == GameState.PLAYING))
                        continue;
                    if (timeelapsedsincelaststep > steptime)
                        step();
                }
            }
        });
	}

	/* Draws the stuff, minus backgrounds, etc. */
	public synchronized void draw(Graphics g)
	{
		
		//The coordinates of the top left corner of the game board.
		int mainx = (tetris.getWidth() - bounds.width) / 2 + 50;
		int mainy = (tetris.getHeight() - bounds.height) / 2;
		
		// Create a border;
		g.setColor(Color.BLACK);
		g.drawRect(mainx-1,mainy-1,
				bounds.width+2,bounds.height+2);
		
		g.setColor(Color.BLACK);
		g.setFont(new Font(Font.MONOSPACED,Font.BOLD,18));
		
		g.drawString(addLeadingZeroes(score,6), 156, 213);  //Draw score
		g.drawString(addLeadingZeroes(lines, 3), 156, 250); //Draw lines
		
		// Loop and draw all the blocks.
		for(int c1 = 0;c1 < blocks.length;c1++)
		{
			for(int c2 = 0;c2 < blocks[c1].length;c2++)
			{
				// Just in case block's null, it doesn't draw as black.
				g.setColor(Block.emptycolor);
				g.setColor(blocks[c1][c2].getColor());
				
				g.fillRect(mainx+c1*squaredim,
						mainy+c2*squaredim, squaredim, squaredim);
				
				//Draw square borders.
				g.setColor(new Color(255,255,255,25));
				g.drawRect(mainx+c1*squaredim,
						mainy+c2*squaredim, squaredim, squaredim);
				
			}
		}
		
		int nextx = 100;
		int nexty = 340;

		//Less typing.
		Block[][] nextb = null;
		if(nextblock != null)
		{
			nextb = nextblock.array;
			//Loop and draw next block.
			for(int c1 = 0;c1 < nextb.length;c1++)
			{
				for(int c2 = 0;c2 < nextb[c1].length;c2++)
				{
					Color c = nextb[c2][c1].getColor();
					
					if(c != null && !c.equals(Block.emptycolor))
					{
						g.setColor(new Color(0,0,0,128));

						int nextblockdim = 18;
						g.fillRect(nextx+c1* nextblockdim,
							nexty+c2* nextblockdim, nextblockdim, nextblockdim);
					}
				}
			}
		}
		
		if(state == GameState.PAUSED || state == GameState.GAMEOVER)
		{
			g.setColor(new Color(255,255,255,160));
			g.setFont(new Font(Font.SERIF,Font.BOLD,16));
			String pausestring = null;
			
			if(state == GameState.PAUSED)
				pausestring = "(SHIFT to play).";
			
			if(state == GameState.GAMEOVER){
				if(tetris.isHumanControlled)
					pausestring = "Game over (SHIFT to restart).";
				else
					pausestring = Integer.toString(lastlines) + 
						(lastlines==1?" Line":" Lines");
			}
			
			g.drawString(pausestring, 
					(tetris.getWidth() - g.getFontMetrics()
							.stringWidth(pausestring))/ 2 + 50,300);
		}
	}
	
	
	/*Called when the RIGHT key is pressed.*/
	public void keyright()
	{
		if(activeblock==null || state!=GameState.PLAYING)
			return;
		
		activeblock.x++;
		
		//Failsafe: Revert XPosition.
		if(!copy())activeblock.x--;
		
	}
	
	/*Called when the LEFT key is pressed.*/
	public void keyleft()
	{
		if(activeblock==null || state!=GameState.PLAYING)
			return;
		
		activeblock.x--;
		
		//Failsafe: Revert XPosition.
		if(!copy())activeblock.x++;
	}
	
	/*Called when the DOWN key is pressed.*/
	public void keydown()
	{
		if(activeblock==null || state!=GameState.PLAYING)
			return;
		
		step();
	}
	
	/*Called when rotate key is called (Z or UP)*/
	public void keyrotate()
	{
		if(activeblock==null || activeblock.array == null || state!=GameState.PLAYING)
			return;
		
		
		Block[][] lastblock = copy2D(activeblock.array);
		int lastrot = activeblock.rot;
		
		//Next rotation in array.
		if(activeblock.rot == blockdef[activeblock.type].length-1)
		{
			activeblock.rot = 0;
		}
		else activeblock.rot++;
		
		activeblock.array = toBlock2D(
				blockdef[activeblock.type][activeblock.rot]);
		tetris.sound.sfx(Sounds.ROTATE);
		
		//Failsafe revert.
		if(!copy()){
			activeblock.array = lastblock;
			activeblock.rot = lastrot;
		}
	}
	
	/*Called when slam key (SPACE) is pressed.*/
	public void keyslam()
	{
		if(activeblock==null || state!=GameState.PLAYING)
			return;
		
		laststep = System.currentTimeMillis();
		
		//This will game over pretty damn fast!
		if(activeblock.array == null)newblock();
		
		while(true)
		{
			activeblock.y++;
		
			if(!copy())
			{
				donecurrent();
				return;
			}
		}
	}

	/* Should be called AFTER swing initialization. This is so
	 * the first block doesn't appear halfway down the screen.*/
	public synchronized void startengine()
	{
		if(!gamethread.isAlive())  gamethread.start();
	}
	
	/*Resets the blocks but keeps everything else.*/
	private synchronized void clear()
	{
		for(int i = 0;i < blocks.length;i++)
		{
			for(int j = 0;j < blocks[i].length;j++)
			{
				blocks[i][j] = new Block(Block.EMPTY);
			}
		}
	}
	
	/*Fully resets everything.*/
	private synchronized void reset()
	{
		score = 0;
		lines = 0;
		clear();
		activeblock.array = null;
	}

	/* Done the current block; plays the FALL sound and changes
	 * all active blocks to filled.
	 */
	private synchronized void donecurrent()
	{	
		tetris.sound.sfx(Sounds.FALL);
		for (Block[] block : blocks) {
			for (Block aBlock : block) {
				if (aBlock.getState() == Block.ACTIVE)
					aBlock.setState(Block.FILLED);
			}
		}
		
		checkforclears();//Moving this here.
	}

	/* Called when Game Over (Blocks stacked so high that copy() fails)*/
	private synchronized void gameover()
	{
		//Check first.
		if(state == GameState.GAMEOVER) {
            return;
        }
		
		//Return immediately.
		new Thread(() -> {
			//pause the game first.
			state = GameState.GAMEOVER;
			if(!tetris.isHumanControlled)
				tetris.controller.flag = false;

			//die sound.
			tetris.sound.sfx(Sounds.DIE);

			if(!tetris.isHumanControlled){
				lastlines = lines;
			}

			int lastscore = score;

			sleep_(1000);
			reset();
			sleep_(100);

			if(!tetris.isHumanControlled){
				if(!anomaly_flag)
					tetris.genetic.sendScore(lastscore);
				tetris.controller = new TetrisAI(tetris);
				tetris.genetic.setAIValues(tetris.controller);
				state = GameState.PLAYING;
				tetris.controller.send_ready();
				anomaly_flag = false;
				lastnewblock = System.currentTimeMillis();
			}

		}).start();
		
	}

	/* Copies the position of the active block into
	 * the abstract block grid. Returns false if a block
	 * already exists under it, true otherwise.
	 * 
	 * This method isn't very efficient. Thus, it must be
	 * synchronized.*/
	private synchronized boolean copy()
	{
		try{

		if(activeblock==null || activeblock.array==null)
			return false;//Early NullPointerException failsafe
		
		int x = activeblock.x;
		int y = activeblock.y;
		Block[][] buffer = copy2D(blocks);
		
		//Check if any blocks already have a block under them.
		//If yes, immediately return.
		for(int i = 0;i < 4;i++)
		{
			for(int r = 0;r < 4;r++)
			{
				if(activeblock.array[r][i].getState() == Block.ACTIVE
					&&buffer[x+i][y+r].getState() == Block.FILLED)
				{
					return false;
				}
			}
		}
		
		//First remove all active blocks.
			for (Block[] aBuffer : buffer) {
				for (Block anABuffer : aBuffer) {
					if (anABuffer.getState() == Block.ACTIVE) {
						anABuffer.setState(Block.EMPTY);
						anABuffer.setColor(Block.emptycolor);
					}
				}
			}
		
		//Then fill in blocks from the new position.
		for(int i = 0;i < 4;i++)
		{
			for(int r = 0;r < 4;r++)
			{
				if(activeblock.array[i][r].getState() == Block.ACTIVE)
				{
					buffer[x+r][y+i].setState(Block.ACTIVE);
					buffer[x+r][y+i].setColor(activeblock.color);
				}
			}
		}
		
		//Nothing threw an exception; now copy the buffer.
		blocks = copy2D(buffer);
		
		}catch(ArrayIndexOutOfBoundsException e)
		{return false;}//Noob bounds detection.
					//Exceptions are supposedly slow but
					//performance isn't really an issue
					//here.
		
		return true;
	}

	/*Steps into the next phase if possible.*/
	private synchronized void step()
	{
		if(activeblock == null)
		{//step() gives you a random block if none is available.
			newblock();
			
			return;
		}
		
		laststep = System.currentTimeMillis();
		
		//move 1 down.
		activeblock.y++;
		
		if(!copy())
			donecurrent();
		
	}
	
	
	/* Runs the checkforclears() on a seperate thread. Also performs
	 * the fade out effect.*/
	private synchronized void checkforclears()
	{
		//Threading fix?
		activeblock = null;
		
		Thread th = new Thread(() -> {
            //Some copy/pasting here! =)
            ArrayList<Block> fadeblocks = new ArrayList<Block>();

            loop:
            for(int i = blocks[0].length-1;i>=0;i--)
            {
                //check for unfilled blocks.
				for (Block[] block : blocks) {
					if (!(block[i].getState() == Block.FILLED))
						continue loop;
				}

                //passed; now add blocks.
				for (Block[] block : blocks) {
					fadeblocks.add(block[i]);
				}
            }

            long before = System.currentTimeMillis();
            int approxloops = fadetime/20;

            state = GameState.BUSY;

            //Fade loop: works by object referencing
            while(System.currentTimeMillis() - before < fadetime)
            {
                if(fadeblocks.size()==0) break;//Lol yea.

                //This is a linear fade algorithm.
                for(Block b : fadeblocks)
                {
                    //Not the best color algorithm, but works most of
                    //the time.

                    //New fading algorithm. Only changes the ALPHA value
                    //and leaves the rgb.
                    Color bcol = b.getColor();
                    int R = bcol.getRed();
                    int G = bcol.getGreen();
                    int B = bcol.getBlue();
                    int AL = bcol.getAlpha();

                    int fade = (AL-Block.emptycolor.getAlpha()) /approxloops;

                    if(AL>0)
                        AL-=fade;

                    if(AL < 0) //Occasionally crashes without this.
                        AL = 0;

                    Color newc = new Color(R,G,B,AL);
                    b.setColor(newc);
                }

                sleep_(20);
            }

            state = GameState.PLAYING;

            //Now actually remove the blocks.
            checkforclears(0,null);
            newblock();
        });
		
		th.start();
	}
	
	
	/* As expected this function checks whether there are any clears.
	 * Uses recursion if more than one line can be cleared.
	 * Don't run this on the EDT!*/
	private synchronized void checkforclears(int alreadycleared, Block[][] b)
	{
		if(b == null)
			b = blocks;
		int whichline = -1;
		int old = alreadycleared;
		
		//Loops to find any row that has every block filled.
		// If one block is not filled, the loop breaks.
		ML:
		for(int i = b[0].length-1;i>=0;i--)
		{
			for (Block[] aB : b) {
				if (!(aB[i].getState() == Block.FILLED))
					continue ML;
			}
			
			alreadycleared++;
			whichline = i;
			break ML;
		}
		
		//If this recursive step produced more clears:
		if(alreadycleared>old)
		{
			for(int i = whichline;i>0;i--)
			{//Iterate and copy the state of the block on top of itself
				//to its location.
				for(int y = 0;y < b.length;y++)
				{
					b[y][i] = b[y][i-1];
				}
			}
			
			// Recursion step! Necessary if you want to clear more than
			// 1 line at a time!
			checkforclears(alreadycleared, b);
		}
		else if(alreadycleared > 0)
		{
			// Use Nintendo's original scoring system.
			switch(alreadycleared)
			{
			case 1:
				score += 40;
				break;
			case 2:
				score += 100;
				break;
			case 3:
				score += 300;
				break;
			case 4:
				score += 1200;
				break;
			}
			
			//No new lines were cleared.
			if(alreadycleared >= 4)tetris.sound.sfx(Sounds.TETRIS);
			else tetris.sound.sfx(Sounds.CLEAR);
			
			lines += alreadycleared;
		}
		
		blocks = b;
	}

	/* Generates a random block , in a random rotation. */
	private synchronized void newblock()
	{
		// Check:
		if(activeblock != null)
			return;
		if(nextblock == null)
			nextblock = getRandBlock();
		
		//Next block becomes this block.
		activeblock = nextblock.clone();
		
		//Generate random block.
		nextblock = getRandBlock();
		
		if(!copy()){
			gameover();
		}
		
		//Bonus?
		score += 1;
		
		//Successfully dropped 1 block, here.
		blocksdropped += 1;
		
		if(!tetris.isHumanControlled && 
			System.currentTimeMillis() - lastnewblock > (2000 + 50 * TetrisAI.waittime)){
			System.out.println("Ooooooooops!!! ");
			anomaly_flag = true;
			gameover();
		}
		
		lastnewblock = System.currentTimeMillis();
	}
	
	/* Create and return a random block.*/
	private synchronized Tetromino getRandBlock()
	{
		Tetromino ret = new Tetromino();
		int x = blockdef.length;
		int rnd1 = rdm.nextInt(x);
		
		int y = blockdef[rnd1].length;
		int rnd2 = rdm.nextInt(y);
		
		ret.type=rnd1;
		ret.rot=rnd2;
		
		ret.array = toBlock2D(blockdef[rnd1][rnd2]);
		
		ret.x = width/2 -2;
		ret.y = 0;

		ret.color = colors[rnd1];
		
		//Fill the block with their colors first.
		for(int i = 0;i < ret.array.length;i++)
		{
			for(int k = 0;k < ret.array[i].length;k++)
			{
				if(ret.array[i][k].getState()==Block.ACTIVE)
					ret.array[i][k].setColor(ret.color);
			}
		}
		return ret;
	}
	
	/* Copies an array, but runs in n^2 time. */
	private static Block[][] copy2D(Block[][] in)
	{
		//if(in == null) return null;
		Block[][] ret;
		ret = new Block[in.length][in[0].length];
		for(int i = 0;i < in.length;i++) {
			for(int j = 0;j < in[0].length;j++) {
				ret[i][j] = in[i][j].clone();
			}
		}
		return ret;
	}
	
	/* Function to convert byte[][] to Block[][] */
	private static Block[][] toBlock2D(byte[][] b)
	{
		if(b == null)return null;
		Block[][] ret = new Block[b.length][b[0].length];
		for(int i = 0;i < b.length;i++) {
			for(int j = 0;j < b[0].length;j++) {
				switch(b[i][j])
				{
				case 1:
					ret[i][j] = new Block(Block.ACTIVE);
					break;
				default:
					ret[i][j] = new Block(Block.EMPTY);
				}
			}
		}
		return ret;
	}

	/* Function to convert Block[][] to byte[][] */
	static byte[][] toByte2D(Block[][] b)
	{
		if(b == null)return null;
		byte[][] ret = new byte[b.length][b[0].length];
		for(int i = 0;i < b.length;i++) {
			for(int j = 0;j < b[0].length;j++) {
				ret[i][j] = b[i][j].toByte();
			}
		}
		
		return ret;
	}

}
