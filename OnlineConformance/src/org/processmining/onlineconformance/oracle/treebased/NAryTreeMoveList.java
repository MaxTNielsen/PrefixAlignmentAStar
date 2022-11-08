package org.processmining.onlineconformance.oracle.treebased;

import java.util.ArrayList;
import java.util.List;

import org.processmining.onlineconformance.models.impl.MoveImpl;

public class NAryTreeMoveList {

	public static int NOMOVE = 0x0000FFFF;

	protected int[] moves;
	protected int cost;

	public NAryTreeMoveList() {
		this.moves = new int[0];
		this.cost = 0;
	}

	public NAryTreeMoveList(List<MoveImpl<NAryTreeLabel, NAryTreeNode>> moves) {
		setMoves(moves);
	}

	NAryTreeMoveList(int[] moves, int cost) {
		this.moves = moves;
		this.cost = cost;
	}

	public static int makeMove(int logMove, int modelMove) {
		return logMove << 16 | modelMove;
	}

	public void setMoves(List<MoveImpl<NAryTreeLabel, NAryTreeNode>> moves) {
		this.moves = new int[moves.size() * 2];
		int i = 0;
		int c = 0;
		for (MoveImpl<NAryTreeLabel, NAryTreeNode> move : moves) {
			this.moves[i++] = makeMove(move.getEventLabel() == null ? NOMOVE : move.getEventLabel().getLabel(),
					move.getTransition() == null ? NOMOVE : move.getTransition().getNode());
			this.moves[i++] = move.getCost();
			c += move.getCost();
		}
		this.cost = c;
	}

	public List<MoveImpl<NAryTreeLabel, NAryTreeNode>> getMovementSequence() {
		List<MoveImpl<NAryTreeLabel, NAryTreeNode>> result = new ArrayList<>(moves.length / 2);
		for (int i = 0; i < moves.length / 2; i++) {
			result.add(get(i));
		}
		return result;
	}

	public int size() {
		return moves.length / 2;
	}

	public MoveImpl<NAryTreeLabel, NAryTreeNode> get(int i) {
		i = i * 2;
		short l = (short) ((moves[i] >>> 16) & 0x0000FFFF);
		NAryTreeLabel tl;
		if (l < 0) {
			tl = null;
		} else {
			tl = new NAryTreeLabel(l);
		}

		short n = (short) (moves[i] & 0x0000FFFF);
		NAryTreeNode tn;
		if (n < 0) {
			tn = null;
		} else {
			tn = new NAryTreeNode(n);
		}
		return new MoveImpl<>(tl, tn, moves[i + 1]);
	}

}
