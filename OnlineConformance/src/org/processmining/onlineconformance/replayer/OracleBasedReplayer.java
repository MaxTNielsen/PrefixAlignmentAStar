package org.processmining.onlineconformance.replayer;

import gnu.trove.map.TObjectIntMap;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.processmining.onlineconformance.models.impl.MoveImpl;
import org.processmining.onlineconformance.oracle.MovementSequence;
import org.processmining.onlineconformance.oracle.Oracle;

public class OracleBasedReplayer<S, L, M> {

	protected final Oracle<S, L, M> oracle;

	protected ArrayList<Map.Entry<S, Integer>> currentStates;

	//	protected List<S> currentStateList;
	//	protected TIntList currentCosts;

	protected final int maxQueueSize;

	protected final TObjectIntMap<? extends L> logMoveCost;

	protected int scaling = 10000;

	public <K> ArrayList<Map.Entry<K, Integer>> sortByValue(Map<K, Integer> map) {
		ArrayList<Map.Entry<K, Integer>> list = new ArrayList<>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, Integer>>() {
			@Override
			public int compare(Map.Entry<K, Integer> o1, Map.Entry<K, Integer> o2) {
				return (o1.getValue() / scaling) - (o2.getValue() / scaling);
			}
		});
		return list;
		//		Map<K, V> result = new LinkedHashMap<>();
		//		for (Map.Entry<K, V> entry : list) {
		//			result.put(entry.getKey(), entry.getValue());
		//		}
		//		return result;
	}

	public OracleBasedReplayer(Oracle<S, L, M> oracle, int maxQueueSize, TObjectIntMap<? extends L> logMoveCost) {
		this.oracle = oracle;
		this.maxQueueSize = maxQueueSize;
		this.logMoveCost = logMoveCost;
		this.currentStates = new ArrayList<>();// new LinkedHashMap<S, Integer>(Math.min(100, maxQueueSize), 0.5f);
		//		this.currentStateList = new ArrayList<>();
		//		this.currentCosts = new TIntArrayList();

		S initialState = oracle.getInitialState();

		insert(initialState, 0);
	}

	private void insert(S state, int cost) {
		currentStates.add(new AbstractMap.SimpleEntry<>(state, cost));
	}

	/**
	 * Process an event labeled with L and update the internal list
	 * 
	 * @param label
	 */
	public void update(L label) {

		Map<S, Integer> newStates = new LinkedHashMap<S, Integer>(Math.min(100, maxQueueSize), 0.5f);
		int newStatesMaxCost = -1;

		// go through each state in the set of current states
		//		for (S state : currentStates.keySet()) {
		for (Map.Entry<S, Integer> entry : currentStates) {
			S state = entry.getKey();
			if (newStates.size() >= maxQueueSize && entry.getValue() >= newStatesMaxCost) {
				break;
			}
			Integer costAlreadyReached;

			if (label != null) {
				// compute the state when doing logmove:
				MoveImpl<L, M> logMove = new MoveImpl<>(label, null, getCostFor(label));
				costAlreadyReached = newStates.get(state);

				if (costAlreadyReached == null || //
						entry.getValue() + logMove.getCost() < costAlreadyReached) {
					// Add the new state if it is new in the currentstates or if it is reached 
					// with lower costs
					followEdge(newStates, state, state, logMove, entry.getValue() + logMove.getCost());
					if (entry.getValue() + logMove.getCost() > newStatesMaxCost) {
						newStatesMaxCost = entry.getValue() + logMove.getCost();
					}

				}
			}

			S newState;
			int cost;
			Collection<? extends MovementSequence<S, L, M>> syncMoves;
			if (label == null && oracle.isFinal(state)) {
				// we reached a final state without doing more moves.
				newState = state;
				cost = 0;
				syncMoves = null;
				costAlreadyReached = newStates.get(newState);

				if (costAlreadyReached == null || //
						entry.getValue() + cost < costAlreadyReached) {
					// Add the new state if it is new in the currentstates or if it is reached 
					// with lower costs
					followEdge(newStates, state, newState, (MovementSequence<S, L, M>) null, entry.getValue() + cost);
					if (entry.getValue() + cost > newStatesMaxCost) {
						newStatesMaxCost = entry.getValue() + cost;
					}

					//				System.out.println("reached " + newState + " from " + state);
				}
			} else if (entry.getValue() < newStatesMaxCost) {

				syncMoves = oracle.getSyncronousMoveSequences(state, label);
				// if no moves are possible, or the list of moves is empty (signalled by both model and log move null), continue
				for (MovementSequence<S, L, M> movementSequence : syncMoves) {
					//					if (movementSequence.getLastLogMove() == null && movementSequence.getLastModelMove() == null) {
					//						// current state was a final state. keep it
					//						newStates.put(state, entry.getValue());
					//					}
					cost = movementSequence.getTotalCost();
					newState = movementSequence.getToState();//oracle.execute(state, syncMoves);

					costAlreadyReached = newStates.get(newState);

					if (costAlreadyReached == null || //
							entry.getValue() + cost < costAlreadyReached) {
						// Add the new state if it is new in the currentstates or if it is reached 
						// with lower costs
						followEdge(newStates, state, newState, movementSequence, entry.getValue() + cost);
						if (entry.getValue() + cost > newStatesMaxCost) {
							newStatesMaxCost = entry.getValue() + cost;
						}
						//				System.out.println("reached " + newState + " from " + state);
					}
				}
			}

		}

		// now limit the size of the set of newStates to size again
		//		System.out.println("sorting: " + newStates.size() + " states");
		currentStates = sortByValue(newStates);
		for (int i = currentStates.size(); i-- > maxQueueSize;) {
			currentStates.remove(i);
		}
	}

	public int getCostFor(L label) {
		return 1 + scaling * logMoveCost.get(label);
	}

	protected void followEdge(Map<S, Integer> newStates, S fromState, S toState, MovementSequence<S, L, M> moves,
			int cost) {
		newStates.remove(toState);
		S copyOfNewState = oracle.createCopy(toState);
		newStates.put(copyOfNewState, cost);
	}

	protected void followEdge(Map<S, Integer> newStates, S fromState, S toState, MoveImpl<L, M> move, int cost) {
		newStates.remove(toState);
		S copyOfNewState = oracle.createCopy(toState);
		newStates.put(copyOfNewState, cost);
	}
	
	public S peek() {
		if (currentStates.isEmpty()) {
			return null;
		}
		return currentStates.get(0).getKey();
	}

	public List<Map.Entry<S, Integer>> getCurrentStates() {
		return Collections.unmodifiableList(currentStates);
	}

	/**
	 * Like a regular replayer, the oracle uses a scaling parameter to ensure
	 * progress in the alignments. Set this scaling large enough, surely larger
	 * than the longest trace in the log + the shortest execution of the model.
	 * 
	 * @return
	 */
	public int getScaling() {
		return scaling;
	}

	/**
	 * Like a regular replayer, the oracle uses a scaling parameter to ensure
	 * progress in the alignments. Set this scaling large enough, surely larger
	 * than the longest trace in the log + the shortest execution of the model.
	 * 
	 * @return
	 */
	public void setScaling(int scaling) {
		this.scaling = scaling;
	}

}
