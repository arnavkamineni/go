package go;

import java.util.Collections;
import java.util.List;

public class Minimax {
    public static int evaluationCount = 0;
    private Board board;
    private boolean aiIsWhite;
    private int difficulty;             // 0=Normal, 1=Hard, 2=Impossible
    private static final int WIN_SCORE = 100_000_000;

    /**
     * @param board       current board
     * @param aiIsWhite   true if AI plays White, false if Black
     * @param difficulty  0=Normal,1=Hard,2=Impossible
     */
    public Minimax(Board board, boolean aiIsWhite, int difficulty) {
        this.board = board;
        this.aiIsWhite = aiIsWhite;
        this.difficulty = difficulty;
    }

    public static int getWinScore() {
        return WIN_SCORE;
    }

    public static double evaluateBoardForWhite(Board board, boolean blacksTurn) {
        evaluationCount++;
        double blackScore = getScore(board, true, blacksTurn);
        double whiteScore = getScore(board, false, blacksTurn);
        if (blackScore == 0) blackScore = 1.0;
        return whiteScore / blackScore;
    }

    public static int getScore(Board board, boolean forBlack, boolean blacksTurn) {
        int[][] bm = board.getBoardMatrix();
        return evaluateHorizontal(bm, forBlack, blacksTurn)
             + evaluateVertical(bm, forBlack, blacksTurn)
             + evaluateDiagonal(bm, forBlack, blacksTurn);
    }

    public int[] calculateNextMove(int plyDepth) {
        // adjust depth by difficulty
        int depth = plyDepth + (difficulty == 1 ? 1 : difficulty == 2 ? 2 : 0);
        board.thinkingStarted();
        int[] move;
        // instant win search
        Object[] bestWin = searchWinningMove(board);
        if (bestWin != null) {
            move = new int[]{(Integer) bestWin[1], (Integer) bestWin[2]};
        } else {
            Object[] res = minimaxSearchAB(depth, new Board(board), aiIsWhite, -Double.MAX_VALUE, Double.MAX_VALUE);
            if (res[1] == null) move = null;
            else move = new int[]{(Integer) res[1], (Integer) res[2]};
        }
        board.thinkingFinished(); evaluationCount = 0;
        return move;
    }

        private Object[] minimaxSearchAB(int depth, Board node, boolean max, double alpha, double beta) {
        // Terminal: check if the side who just moved has won
        boolean justMovedIsWhite = !max;
        if (hasFiveInARow(node, justMovedIsWhite)) {
            double val = (justMovedIsWhite == aiIsWhite) ? +Double.MAX_VALUE : -Double.MAX_VALUE;
            return new Object[]{ val, null, null };
        }
        // Depth cutoff
        if (depth == 0) {
            return new Object[]{ evaluateBoardForWhite(node, !max), null, null };
        }
        // Generate and order moves
        List<int[]> moves = node.generateMoves();
        if (moves.isEmpty()) {
            // no moves: treat as draw/evaluation
            return new Object[]{ evaluateBoardForWhite(node, !max), null, null };
        }
        Collections.sort(moves, (a, b) -> Double.compare(
            heuristicValue(node, b, max),
            heuristicValue(node, a, max)
        ));

        // Initialize best from the first move
        int[] firstMove = moves.get(0);
        node.addStoneNoGUI(firstMove[1], firstMove[0], !max);
        Object[] firstRes = minimaxSearchAB(depth - 1, node, !max, alpha, beta);
        node.removeStoneNoGUI(firstMove[1], firstMove[0]);
        double bestScore = (Double) firstRes[0];
        Object[] best = new Object[]{ bestScore, firstMove[0], firstMove[1] };

        // Update alpha/beta from initial
        if (max) alpha = Math.max(alpha, bestScore);
        else     beta  = Math.min(beta, bestScore);

        // Loop remaining moves
        for (int i = 1; i < moves.size(); i++) {
            int[] mv = moves.get(i);
            node.addStoneNoGUI(mv[1], mv[0], !max);
            Object[] cand = minimaxSearchAB(depth - 1, node, !max, alpha, beta);
            node.removeStoneNoGUI(mv[1], mv[0]);
            double score = (Double) cand[0];
            if (max) {
                if (score > bestScore) {
                    bestScore = score;
                    best[0] = bestScore;
                    best[1] = mv[0];
                    best[2] = mv[1];
                }
                alpha = Math.max(alpha, score);
                if (alpha >= beta) break;
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    best[0] = bestScore;
                    best[1] = mv[0];
                    best[2] = mv[1];
                }
                beta = Math.min(beta, score);
                if (alpha >= beta) break;
            }
        }
        return best;
    }



    private Object[] searchWinningMove(Board b) {
        boolean aiPlaysBlack = !aiIsWhite;
        for (int[] mv : b.generateMoves()) {
            evaluationCount++;
            Board tmp = new Board(b);
            tmp.addStoneNoGUI(mv[1], mv[0], aiPlaysBlack);
            if (hasFiveInARow(tmp, !aiPlaysBlack))
                return new Object[]{null, mv[0], mv[1]};
        }
        return null;
    }

    private static boolean hasFiveInARow(Board board, boolean forWhite) {
        int[][] m = board.getBoardMatrix();
        int target = forWhite ? 1 : 2;
        int N = m.length;
        // horizontal/vertical
        for (int i=0;i<N;i++) for (int j=0;j<N;j++) {
            if (j<=N-5) {boolean ok=true; for(int k=0;k<5;k++) if(m[i][j+k]!=target){ok=false;break;} if(ok)return true;}
            if (i<=N-5) {boolean ok=true; for(int k=0;k<5;k++) if(m[i+k][j]!=target){ok=false;break;} if(ok)return true;}
        }
        // diagonals
        for (int i=0;i<=N-5;i++) for (int j=0;j<=N-5;j++) {
            boolean ok1=true,ok2=true;
            for(int k=0;k<5;k++){
                if(m[i+k][j+k]!=target)ok1=false;
                if(m[i+4-k][j+k]!=target)ok2=false;
            }
            if(ok1||ok2) return true;
        }
        return false;
    }

    private static double heuristicValue(Board b, int[] mv, boolean forWhite) {
        Board tmp = new Board(b);
        tmp.addStoneNoGUI(mv[1], mv[0], !forWhite);
        return evaluateBoardForWhite(tmp, !forWhite);
    }

    
	// This function calculates the score by evaluating the stone positions in horizontal direction
	public static int evaluateHorizontal(int[][] boardMatrix, boolean forBlack, boolean playersTurn ) {

		int[] evaluations = {0, 2, 0}; // [0] -> consecutive count, [1] -> block count, [2] -> score
		// blocks variable is used to check if a consecutive stone set is blocked by the opponent or
		// the board border. If the both sides of a consecutive set is blocked, blocks variable will be 2
		// If only a single side is blocked, blocks variable will be 1, and if both sides of the consecutive
		// set is free, blocks count will be 0.
			// By default, first cell in a row is blocked by the left border of the board.
		// If the first cell is empty, block count will be decremented by 1.
		// If there is another empty cell after a consecutive stones set, block count will again be 
		// decremented by 1.
		// Iterate over all rows
		for(int i=0; i<boardMatrix.length; i++) {
			// Iterate over all cells in a row
			for(int j=0; j<boardMatrix[0].length; j++) {
				// Check if the selected player has a stone in the current cell
				evaluateDirections(boardMatrix,i,j,forBlack,playersTurn,evaluations);
			}
			evaluateDirectionsAfterOnePass(evaluations, forBlack, playersTurn);
		}

		return evaluations[2];
	}
	
	// This function calculates the score by evaluating the stone positions in vertical direction
	// The procedure is the exact same of the horizontal one.
	public static  int evaluateVertical(int[][] boardMatrix, boolean forBlack, boolean playersTurn ) {

		int[] evaluations = {0, 2, 0}; // [0] -> consecutive count, [1] -> block count, [2] -> score
		
		for(int j=0; j<boardMatrix[0].length; j++) {
			for(int i=0; i<boardMatrix.length; i++) {
				evaluateDirections(boardMatrix,i,j,forBlack,playersTurn,evaluations);
			}
			evaluateDirectionsAfterOnePass(evaluations,forBlack,playersTurn);
			
		}
		return evaluations[2];
	}

	// This function calculates the score by evaluating the stone positions in diagonal directions
	// The procedure is the exact same of the horizontal calculation.
	public static  int evaluateDiagonal(int[][] boardMatrix, boolean forBlack, boolean playersTurn ) {

		int[] evaluations = {0, 2, 0}; // [0] -> consecutive count, [1] -> block count, [2] -> score
		// From bottom-left to top-right diagonally
		for (int k = 0; k <= 2 * (boardMatrix.length - 1); k++) {
		    int iStart = Math.max(0, k - boardMatrix.length + 1);
		    int iEnd = Math.min(boardMatrix.length - 1, k);
		    for (int i = iStart; i <= iEnd; ++i) {
		        evaluateDirections(boardMatrix,i,k-i,forBlack,playersTurn,evaluations);
		    }
		    evaluateDirectionsAfterOnePass(evaluations,forBlack,playersTurn);
		}
		// From top-left to bottom-right diagonally
		for (int k = 1-boardMatrix.length; k < boardMatrix.length; k++) {
		    int iStart = Math.max(0, k);
		    int iEnd = Math.min(boardMatrix.length + k - 1, boardMatrix.length-1);
		    for (int i = iStart; i <= iEnd; ++i) {
				evaluateDirections(boardMatrix,i,i-k,forBlack,playersTurn,evaluations);
		    }
			evaluateDirectionsAfterOnePass(evaluations,forBlack,playersTurn);
		}
		return evaluations[2];
	}
	public static void evaluateDirections(int[][] boardMatrix, int i, int j, boolean isBot, boolean botsTurn, int[] eval) {
		// Check if the selected player has a stone in the current cell
		if (boardMatrix[i][j] == (isBot ? 2 : 1)) {
			// Increment consecutive stones count
			eval[0]++;
		}
		// Check if cell is empty
		else if (boardMatrix[i][j] == 0) {
			// Check if there were any consecutive stones before this empty cell
			if (eval[0] > 0) {
				// Consecutive set is not blocked by opponent, decrement block count
				eval[1]--;
				// Get consecutive set score
				eval[2] += getConsecutiveSetScore(eval[0], eval[1], isBot == botsTurn);
				// Reset consecutive stone count
				eval[0] = 0;
				// Current cell is empty, next consecutive set will have at most 1 blocked side.
			}
			// No consecutive stones.
			// Current cell is empty, next consecutive set will have at most 1 blocked side.
			eval[1] = 1;
		}
		// Cell is occupied by opponent
		// Check if there were any consecutive stones before this empty cell
		else if (eval[0] > 0) {
			// Get consecutive set score
			eval[2] += getConsecutiveSetScore(eval[0], eval[1], isBot == botsTurn);
			// Reset consecutive stone count
			eval[0] = 0;
			// Current cell is occupied by opponent, next consecutive set may have 2 blocked sides
			eval[1] = 2;
		} else {
			// Current cell is occupied by opponent, next consecutive set may have 2 blocked sides
			eval[1] = 2;
		}
	}
	private static void evaluateDirectionsAfterOnePass(int[] eval, boolean isBot, boolean playersTurn) {
		// End of row, check if there were any consecutive stones before we reached right border
		if (eval[0] > 0) {
			eval[2] += getConsecutiveSetScore(eval[0], eval[1], isBot == playersTurn);
		}
		// Reset consecutive stone and blocks count
		eval[0] = 0;
		eval[1] = 2;
	}

	// This function returns the score of a given consecutive stone set.
	// count: Number of consecutive stones in the set
	// blocks: Number of blocked sides of the set (2: both sides blocked, 1: single side blocked, 0: both sides free)
	public static  int getConsecutiveSetScore(int count, int blocks, boolean currentTurn) {
		final int winGuarantee = 1000000;
		// If both sides of a set is blocked, this set is worthless return 0 points.
		if(blocks == 2 && count < 5) return 0;

		switch(count) {
		case 5: {
			// 5 consecutive wins the game
			return WIN_SCORE;
		}
		case 4: {
			// 4 consecutive stones in the user's turn guarantees a win.
			// (User can win the game by placing the 5th stone after the set)
			if(currentTurn) return winGuarantee;
			else {
				// Opponent's turn
				// If neither side is blocked, 4 consecutive stones guarantees a win in the next turn.
				if(blocks == 0) return winGuarantee/4;
				// If only a single side is blocked, 4 consecutive stones limits the opponents move
				// (Opponent can only place a stone that will block the remaining side, otherwise the game is lost
				// in the next turn). So a relatively high score is given for this set.
				else return 200;
			}
		}
		case 3: {
			// 3 consecutive stones
			if(blocks == 0) {
				// Neither side is blocked.
				// If it's the current player's turn, a win is guaranteed in the next 2 turns.
				// (User places another stone to make the set 4 consecutive, opponent can only block one side)
				// However the opponent may win the game in the next turn therefore this score is lower than win
				// guaranteed scores but still a very high score.
				if(currentTurn) return 50_000;
				// If it's the opponent's turn, this set forces opponent to block one of the sides of the set.
				// So a relatively high score is given for this set.
				else return 200;
			}
			else {
				// One of the sides is blocked.
				// Playmaker scores
				if(currentTurn) return 10;
				else return 5;
			}
		}
		case 2: {
			// 2 consecutive stones
			// Playmaker scores
			if(blocks == 0) {
				if(currentTurn) return 7;
				else return 5;
			}
			else {
				return 3;
			}
		}
		case 1: {
			return 1;
		}
		}

		// More than 5 consecutive stones? 
		return WIN_SCORE*2;
	}
}
