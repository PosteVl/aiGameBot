package MKAgent;

// Class that is concerned with Evaluating the board.
public class BoardEvaluator {

    // Each weight is applied to a different heuristic
    public static int mancalaWeight = 2;
    public static int holesWeight = 1;
    public static int additionalMoveWeight = 0;
    public static int ourPercentageWeight = 1;
    public static int enemyPercentageWeight = 4;

    // Returns an int which is or heuristic
    public static int evaluateBoard(Board board, Side ourSide) {
        return    (mancalaEvaluate(board,ourSide)              * mancalaWeight)
                + (holesEvaluate(board, ourSide)               * holesWeight)
                + (additionalMoves(board, ourSide)             * additionalMoveWeight)
                + (ourMancalaPercentage(board, ourSide)        * ourPercentageWeight)
                + (enemyMancalaPercentage(board, ourSide)      * enemyPercentageWeight);
    } // evaluateBoard

    // Evaluates the difference between mancalas
    public static int mancalaEvaluate(Board board, Side ourSide) {
        return board.getSeedsInStore(ourSide) - board.getSeedsInStore(ourSide.opposite());
    }

    // Evaluates the difference between the holes
    public static int holesEvaluate(Board board, Side ourSide)
    {
        int finalScoreOur = 0;
        int finalScoreEnemy = 0;

        for(int i = 1; i <= board.getNoOfHoles(); i++)
        {
            finalScoreOur += board.getSeeds(ourSide, i);
            finalScoreEnemy += board.getSeeds(ourSide.opposite(), i);
        }//for
        return finalScoreOur - 2*finalScoreEnemy;
    }//holesEvaluate

    // Returns the percentage of owned stones in our Mancala to the overall in the game
    public static int ourMancalaPercentage(Board board, Side ourSide)
    {
        return board.getSeedsInStore(ourSide) * 98 / 100;
    }//ourMancalaPercentage

    // Returns the percentage of owned stones in our Mancala to the overall in the game
    public static int enemyMancalaPercentage(Board board, Side ourSide) {
        return -board.getSeedsInStore(ourSide.opposite()) * 98 / 100;
    }//enemyMancalaPercentage

    // Check whether additional move is possible
    // 0-7
    public static int additionalMoves(Board board, Side ourSide) {
        int extraMoves = 0;

        for (int i = 1; i <= board.getNoOfHoles(); i++) {
            if (i + board.getSeeds(ourSide, i) == 8 || i + board.getSeeds(ourSide, i) == 23) {
                extraMoves++;
            }//if
        }//for
        return extraMoves;
    }//additionalMoves
}//class BoardEvaluator
