package MKAgent;
import java.io.*;

// This class implements the player class for the Mancala game.
// There is a play() function which plays the game (receives and sends msgs with the game engine)
// The bot plays with standard AlphaBetaMinMax using 7 Threads maximum to calculate each subtree of its first choices
public class KalahPlayer extends Thread
{

    // The player has attributes
    private Side ourSide;
    private Kalah kalah;
    private int holes;
    private int maxDepth = 11;

    // Constructor
    public KalahPlayer(int holes, int seeds)
    {
        this.ourSide = Side.SOUTH;
        this.holes = holes;
        this.kalah = new Kalah(new Board(holes, seeds));
    }//public KalahPlayer

    public int bestNextMove(int maxDepth, Board board)
    {
        // Implement Threading

        // Arrays for the value we override
        Value [] valueArray = new Value[this.holes+1];
        TreeThread[] threadArray = new TreeThread[this.holes+1];

        // For every move we currently can do
        // Check if the move is legal
        // If it is spawn a separate thread and perform MinMaxAlphaBeta until we get the value of that move

        for(int i=1; i <= this.holes; i++)
        {
            Move move = new Move(this.ourSide, i);
            Board boardNew = new Board(board);

            // Initialise arrays
            threadArray[i] = null;
            valueArray[i] = new Value(Integer.MIN_VALUE);

            if (kalah.isLegalMove(boardNew, move))
            {
                threadArray[i] = new TreeThread(this.ourSide, Kalah.makeMove(boardNew, move ), maxDepth, boardNew,
                        i, valueArray[i]);
                threadArray[i].start();
            }//if
        }//for


        // Make sure that all threads have finished before moving forward
        while ((threadArray[1] != null && threadArray[1].isAlive()) || (threadArray[2] != null && threadArray[2].isAlive()) ||
                (threadArray[3] != null && threadArray[3].isAlive()) || (threadArray[4] != null && threadArray[4].isAlive()) ||
                (threadArray[5] != null && threadArray[5].isAlive()) || (threadArray[6] != null && threadArray[6].isAlive()) ||
                (threadArray[7] != null && threadArray[7].isAlive()))
        {
            try
            {
                // Make this thread sleep for a bit to not use computational power while waiting
                Thread.sleep(10);
            }//try
            catch (Exception e)
            {
                System.err.println("Something thread related went wrong");
            }//catch

        }//while

        // After the values in valueArray are populated we check which one has the highest value
        int maxValue = Integer.MIN_VALUE;
        int bestMove = 0;

        for(int i=1; i<=this.holes; i++)
        {
          if(maxValue < valueArray[i].value)
          {
              maxValue = valueArray[i].value;
              bestMove = i;
          }//if
        }//for

        // return the best move
        return bestMove;
    }//bestNextMove

    protected void swap()
    {
        this.ourSide = this.ourSide.opposite();
    }

    public void play() throws IOException, InvalidMessageException
    {
        boolean maySwap = false;
        String msg = Main.recvMsg();
        MsgType msgType = Protocol.getMessageType(msg);
        if (msgType != MsgType.END)
        {

            if (msgType != MsgType.START)
            {
                throw new InvalidMessageException("Expected a start message but got something else.");
            }//if
            else
            {
                if (Protocol.interpretStartMsg(msg))
                {
                    this.ourSide = Side.SOUTH;
                    // Because our heuristics are hardcoded we tested which move is the most neutral from our
                    // point of view and since this is the start of the game this value is always going to be
                    // the same.
                    // This is why this value is hardcoded for simplicity and speed reasons
                    Main.sendMsg(Protocol.createMoveMsg(3));
                }//if
                else
                {
                    this.ourSide = Side.NORTH;
                    maySwap = true;
                }//else

                while (true)
                {
                    msg = Main.recvMsg();
                    msgType = Protocol.getMessageType(msg);

                    if (msgType == MsgType.END)
                    {
                        return;
                    }//if

                    if (msgType != MsgType.STATE)
                    {
                        throw new InvalidMessageException("Expected a state message but got something else.");
                    }

                    Protocol.MoveTurn moveTurn = Protocol.interpretStateMsg(msg, this.kalah.getBoard());
                    if (moveTurn.move == -1)
                    {
                        this.swap();
                    }

                    if (moveTurn.again && !moveTurn.end)
                    {
                        msg = null;

                        if (maySwap)
                        {
                            // Since there are only 7 options the opponent can take and we
                            // have a hardcoded heuristics we can have a hardcoded moves which we agree to
                            // swap on (they return a positive number for us) and a ones which we wouldn't want to.
                            Board moveBoard1 = new Board(7,7);
                            Move move1 = new Move(this.ourSide.opposite(), 1);
                            Kalah.makeMove(moveBoard1, move1);

                            Board moveBoard2 = new Board(7,7);
                            Move move2 = new Move(this.ourSide.opposite(), 2);
                            Kalah.makeMove(moveBoard2, move2);

                            Board moveBoard4 = new Board(7,7);
                            Move move4 = new Move(this.ourSide.opposite(), 4);
                            Kalah.makeMove(moveBoard4, move4);

                            if(kalah.getBoard().equals(moveBoard1) || kalah.getBoard().equals(moveBoard2)
                            || kalah.getBoard().equals(moveBoard4))
                            {
                                this.swap();
                                msg = Protocol.createSwapMsg();
                            }//if
                        }//if

                        maySwap = false;
                        if (msg == null)
                        {
                            int nextMove = this.bestNextMove(maxDepth, this.kalah.getBoard());
                            msg = Protocol.createMoveMsg(nextMove);
                        }//if

                        Main.sendMsg(msg);
                    }//if
                }//while
            }//else
        }//if
    }//play()
}//class KalahPlayer

// Object Value
// It is used in combination with the threads
class Value
{
    public int value;
    public Value(int givenValue)
    {
        value = givenValue;
    }//public Value
}//class Value

// Thread class
// Overrides the run run() method in order to attain functionality
class TreeThread extends Thread
{

   Side ourSideThread;
   Side givenSideThread;
   int maxDepthThread;
   Board boardThread;
   int valueItOverridesThread;
   Value value;


   // Constructor
   public TreeThread(Side givenOurSide, Side givenSide, int maxDepth, Board board, int valueItOverrides,
                                                                                                      Value givenValue)
   {
       ourSideThread = givenOurSide;
       givenSideThread = givenSide;
       maxDepthThread = maxDepth;
       boardThread = board;
       valueItOverridesThread = valueItOverrides;
       value = givenValue;
   }//TreeThread

    // The run method just assigns the found value on the Value object.
   public void run()
   {
       value.value = MinMaxAlphaBeta(givenSideThread, maxDepthThread, 0, boardThread,
               Integer.MIN_VALUE, Integer.MAX_VALUE);
   }//run


    // Implements the MinMaxAlphaBeta Algorithm
    // Returns an int which is the value of the current board given
    // Resembles a DFS tree search
   public int MinMaxAlphaBeta(Side givenSide, int maxDepth, int currentDepth, Board board,  int givenAlpha,
                                                                                                          int givenBeta)
   {
       currentDepth++;

       // If we have reached the max given depth we evaluate the current board and start going up
       if (currentDepth == maxDepth || Kalah.gameOver(board))
           return BoardEvaluator.evaluateBoard(board, ourSideThread);


       // Node variables concerned with the best move for the current player (Either Min or Max)
       int currentBestMove = 0;
       int ourValue = 0;

       // For every possible move at current position
       for (int i = 1; i <= board.getNoOfHoles(); i++)
       {
           Move move = new Move(givenSide, i);
           Board boardNew = new Board(board);

           // Check if the move is legal
           if (Kalah.isLegalMove(boardNew, move))
           {
               // If it is go deeper
               int branchValue = MinMaxAlphaBeta(Kalah.makeMove(boardNew, move), maxDepth, currentDepth, boardNew,
                       givenAlpha, givenBeta);


               // After some value is returned, check if it is the first branch we encounter
               // If it is not depending on if it is the MAX or MIN player compare the current value
               // to the found value and act accordingly.
               if (currentBestMove == 0)
               {
                   currentBestMove = i;
                   ourValue = branchValue;
               }//if
               else if((ourSideThread == givenSide && ourValue < branchValue) ||
                       (ourSideThread != givenSide && ourValue > branchValue))
               {
                   currentBestMove = i;
                   ourValue = branchValue;
               }//else if

               // Comparing these values with Alpha and Beta depending on if we are going to be Max or Min
               if (ourSideThread == givenSide && ourValue > givenAlpha)
                   givenAlpha = ourValue;

               if (ourSideThread != givenSide && ourValue < givenBeta)
                   givenBeta = ourValue;

               // Stop the search if it is pointless
               if(givenAlpha > givenBeta)
                   break;
           }//if
       }//for

       return ourValue;
   }//AlphaBetaMinMax
}//class TreeThread