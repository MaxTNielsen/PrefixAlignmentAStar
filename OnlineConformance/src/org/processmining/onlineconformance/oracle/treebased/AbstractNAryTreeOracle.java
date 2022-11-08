package org.processmining.onlineconformance.oracle.treebased;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TShortObjectMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TShortObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.processmining.onlineconformance.oracle.MovementSequence;
import org.processmining.plugins.etm.model.narytree.NAryTree;
import org.processmining.plugins.etm.model.narytree.StateBuilder;

public abstract class AbstractNAryTreeOracle<S extends NAryTreeState, M extends MovementSequence<S, NAryTreeLabel, NAryTreeNode>> {

	public static class NodeSet {
		private final int[] moves;

		public NodeSet(int treeSize) {
			this(treeSize, false);
		}

		public NodeSet(NodeSet m) {
			this.moves = new int[m.moves.length];
			System.arraycopy(m.moves, 0, this.moves, 0, this.moves.length);
		}

		public NodeSet(int treeSize, boolean allIncluded) {
			moves = new int[1 + (treeSize - 1) / 32];
			if (allIncluded) {
				Arrays.fill(moves, ~0);
			}
		}

		public void add(int node) {
			moves[elt(node)] |= bit(node);
		}

		private int bit(int node) {
			return 1 << (node & 31);
		}

		private int elt(int node) {
			return node >>> 5;
		}

		public NodeSet copy() {
			return new NodeSet(this);
		}

		public boolean contains(int node) {
			return (moves[elt(node)] & bit(node)) == bit(node);
		}

		public void remove(int node) {
			moves[elt(node)] &= ~bit(node);
		}

		public void retainAll(NodeSet m) {
			for (int n = moves.length; n-- > 0;) {
				moves[n] &= m.moves[n];
			}
		}

		public void addAll(NodeSet m) {
			for (int n = moves.length; n-- > 0;) {
				moves[n] |= m.moves[n];
			}
		}

		public void removeInterval(int from, int toExclusive) {
			int i = from;
			int e = elt(i);
			int b = bit(i);
			while (i < toExclusive) {
				moves[e] &= ~b;
				i++;
				b = bit(i);
				if (b == 0) {
					e = elt(i);
				}
			}
		}

		public void addInterval(int from, int toExclusive) {
			int i = from;
			int e = elt(i);
			int b = bit(i);
			while (i < toExclusive) {
				moves[e] |= b;
				i++;
				b = bit(i);
				if (b == 0) {
					e = elt(i);
				}
			}
		}
	}

	protected static final int NONODE = -1;

	protected final NAryTree tree;
	protected final int configuration;
	protected final StateBuilder statebuilder;
	protected final byte[] initialState;

	private int hit = 0;
	private int poll = 0;
	private int depth = 0;
	private int miss = 0;

	protected int scaling = 10000;

	protected final TShortObjectMap<NodeSet> allowedModelMoves;

	protected final int stopAt;

	private final class LimitedSizeCache extends LinkedHashMap<StateLabel, M[]> {
		private static final long serialVersionUID = 8409099709501206661L;
		private final int maxEntries;

		public LimitedSizeCache(int initialCapacity, float loadFactor, int maxEntries) {
			super(initialCapacity, loadFactor, true);
			this.maxEntries = maxEntries;
		}

		protected boolean removeEldestEntry(Map.Entry<StateLabel, M[]> eldest) {
			return size() > maxEntries;
		}
	}

	private static class StateLabel {
		private byte[] state;
		private short label;

		public StateLabel(byte[] state, short label) {
			this.state = state;
			this.label = label;
		}

		public int hashCode() {
			return Arrays.hashCode(state) + 37 * label;
		}

		public boolean equals(Object o) {
			if (!(o instanceof StateLabel)) {
				return false;
			}
			StateLabel sl = (StateLabel) o;
			return sl.label == label && Arrays.equals(sl.state, state);
		}
	}

	protected final Map<StateLabel, M[]> cache;

	protected final Set<NAryTreeLabel> nodeLabels = new HashSet<>();
	protected final int maxDepth;

	protected final TIntIntMap moveModelCost;

	protected final int treeSize;

	protected final NodeSet nodesUnderLoop;

	public AbstractNAryTreeOracle(NAryTree tree, int configuration, TIntIntMap moveModelCost, int maxDepth) {
		this(tree, configuration, moveModelCost, maxDepth, true, Integer.MAX_VALUE);
	}

	public AbstractNAryTreeOracle(NAryTree tree, int configuration, TIntIntMap moveModelCost, int maxDepth,
			boolean useCache, int stopAt) {
		this.tree = tree;
		this.treeSize = tree.size();
		this.moveModelCost = moveModelCost;
		this.stopAt = stopAt;
		this.configuration = configuration;
		this.maxDepth = maxDepth;
		this.statebuilder = new StateBuilder(tree, configuration, true);
		this.nodesUnderLoop = new NodeSet(treeSize);

		for (int i = treeSize; i-- > 0;) {
			if (tree.getTypeFast(i) == NAryTree.LOOP) {
				nodesUnderLoop.addInterval(i + 1, tree.getNextFast(tree.getNextFast(i + 1)));
			}
		}

		this.statebuilder.setPushDownUnderAND(false);
		if (!tree.isLeaf(0)) {
			this.initialState = statebuilder.executeAll(statebuilder.initializeState(), 0);
		} else {
			this.initialState = statebuilder.initializeState();
		}

		int node = 0;
		do {
			if (tree.isLeaf(node)) {
				if (tree.getTypeFast(configuration, node) >= 0) {
					nodeLabels.add(new NAryTreeLabel(tree.getTypeFast(configuration, node)));
				}
				node = tree.getNextLeaf(node);
			} else {
				node++;
			}
		} while (node < treeSize);
		if (useCache) {
			cache = new LimitedSizeCache(10000, 0.5f, Integer.MAX_VALUE);
		} else {
			cache = null;
		}

		allowedModelMoves = new TShortObjectHashMap<>();

		//		fillCache();
	}

	public Collection<M> getSyncronousMoveSequences(S currentState, NAryTreeLabel label) {

		StateLabel stateLabel;
		if (label == null) {
			stateLabel = new StateLabel(currentState.getState(), (short) -1);
		} else {
			if (!nodeLabels.contains(label)) {
				return Collections.emptyList();
			}
			stateLabel = new StateLabel(currentState.getState(), label.getLabel());
		}
		if (cache != null && cache.containsKey(stateLabel)) {
			hit++;
			return Arrays.asList(cache.get(stateLabel));
		}
		miss++;

		TObjectIntMap<M> result = new TObjectIntHashMap<>(5, 0.7f, Integer.MAX_VALUE);

		// frustratingly, if there are enabled OR termination nodes enabled, we also need 
		// to consider all combinations of these OR termination exeuctions.

		// in a state where the OR has terminated already, which is important
		// for example for the tree LOOP( LEAF: A , AND( OR( LEAF: B , LEAF: C ) , LEAF: D ) , LEAF: F ) and
		// trace {A, B, D, C, D, F} where the C should not be executed immediately

		if (label != null) {
			getSyncronousMoveSequencesSyncMove(currentState, stateLabel, label, result);

		} else {
			getSyncronousMoveSequencesTermination(currentState, stateLabel, result);
		}

		// No more paths can be found.
		if (cache != null) {
			cache.put(stateLabel, result.keys(this.<M>toArray(result.size())));
		}
		return result.keySet();

	}

	private void getSyncronousMoveSequencesSyncMove(S currentState, StateLabel stateLabel, NAryTreeLabel label,
			TObjectIntMap<M> result) {

		Set<S> statesDone = new HashSet<S>();

		//		FastLookupPriorityQueue statesToDo = new FastLookupPriorityQueue(32);

		// we need to keep track of the state we visited, sorted by cost of reaching it
		// as a first order and depth as a second order criterion.
		TreeSet<NAryTreeStateVisit<S>> statesToDo = createToDoSet();

		NodeSet startWithAllowedMoves = allowedModelMoves.get(label.getLabel());
		if (startWithAllowedMoves == null) {
			startWithAllowedMoves = getScope(treeSize, label.getLabel());
			allowedModelMoves.put(label.getLabel(), startWithAllowedMoves);
		}

		statesToDo.add(new NAryTreeStateVisit<S>(currentState, null, null, startWithAllowedMoves.copy(), 0, 0));

		//Consider each leaf node at most once for each state in the queue
		TIntSet labeledLeafs = new TIntHashSet(treeSize, 0.7f, -1);
		// initialize the labeled leafs.
		for (int n = 0; n < treeSize; n++) {
			if (tree.isLeaf(n) && tree.getTypeFast(configuration, n) == label.getLabel()) {
				labeledLeafs.add(n);
			}
		}

		NodeSet modelMoves;
		while (!statesToDo.isEmpty() && result.size() < stopAt && !labeledLeafs.isEmpty()) {
			//			NAryTreeStateVisit fromState = statesToDo.poll();
			NAryTreeStateVisit<S> fromState = statesToDo.pollFirst();
			modelMoves = fromState.getAllowedMoves();

			if (!statesDone.add(fromState.getState())) {
				continue;
			}

			int cost = fromState.getCostSoFar();

			if (fromState.getDepth() > depth) {
				depth = fromState.getDepth();
			}
			poll++;

			// iterate over the enabled node in this state, 
			TIntIterator it = statebuilder.enabledIterator(fromState.getState().getState());

			while (it.hasNext() && result.size() < stopAt && !labeledLeafs.isEmpty()) {
				int nodeEnabled = it.next();
				if (nodeEnabled < treeSize) {
					if (!modelMoves.contains(nodeEnabled)) {
						continue;
					}
					modelMoves.remove(nodeEnabled);
				} else {
					if (!modelMoves.contains(nodeEnabled - treeSize)) {
						continue;
					}
					// we cannot sequentialize termination of an OR, so keep that 
					// in the set of allowed Model Moves.
				}

				byte[] newState = statebuilder.executeAll(fromState.getState().getState(), nodeEnabled);

				// We need to build a new set of allowed model moves, which contains
				// all current modelMoves as well as all newly added ones after executing this node as long
				// as they are within the scope of the label we are looking for
				NodeSet newModelMoves = determineNewAllowedModelMoves(startWithAllowedMoves, modelMoves, fromState
						.getState().getState(), newState);

				int nodeEnabledCost = getCostForNode(nodeEnabled);
				int nodeEnabledDepth = getDepthForNode(nodeEnabled);
				S toState = createState(currentState, newState);
				if (tree.getType(configuration, nodeEnabled) >= 0) {
					// a leaf node with a label was found.
					if (label.getLabel() == tree.getType(configuration, nodeEnabled)) {
						// we found an enabled node with the right label. We're done here.
						// Now store the movementsequence up to here and continue;
						if (labeledLeafs.remove(nodeEnabled)) {
							// only add a movementSequence if we did not do so yet for this particular enabled node.
							M ms = createMovementSequence(currentState, fromState, toState, label, nodeEnabled, cost);
							// Only keep the movementSequence if we did not reach this state before.
							if (result.get(ms) > ms.getTotalCost()) {
								result.put(ms, ms.getTotalCost());
							}
							if (nodesUnderLoop.contains(nodeEnabled)) {
								// if the current node is under some loop's do or redo part, then
								// invesigate the necessity to terminate or's
								int p = tree.getParentFast(nodeEnabled);
								do {
									if (tree.getTypeFast(p) == NAryTree.OR && statebuilder.isEnabled(newState, p)) {
										// in the tree above the found leaf, there is an OR which is
										// now ready to terminate. Add a movementsequence for that OR
										// as well.
										newState = statebuilder.executeAll(newState, p + treeSize);

										ms = appendOrToMovementSequence(ms, createState(currentState, newState), p
												+ treeSize, getCostForNode(p + treeSize));

										// Only keep the movementSequence if we did not reach this state before.
										if (result.get(ms) > ms.getTotalCost()) {
											result.put(ms, ms.getTotalCost());
										}
									}
									p = tree.getParentFast(p);
								} while (p != NAryTree.NONE);
							}
						}
						//						// sync move, remove costs and depth
						//						nodeEnabledCost = 1;
						//						nodeEnabledDepth = 0;
					} else if (nodeEnabled > 0 && tree.getTypeFast(tree.getParentFast(nodeEnabled)) == NAryTree.OR) {
						// a leaf node under an OR node. Do not execute if 
						// OR ready to terminate
						if (statebuilder.isEnabled(fromState.getState().getState(), tree.getParentFast(nodeEnabled))) {
							continue;
						}
					}
				}

				if (!statesDone.contains(toState)) {
					// If we already have seen this state, no need to reschedule as we reached it with higher cost
					// However, if we didn't see this state yet, continue
					// continue the search after queueing toState
					NAryTreeStateVisit<S> newVisit = new NAryTreeStateVisit<S>(toState, fromState, new NAryTreeNode(
							nodeEnabled), newModelMoves, cost + nodeEnabledCost, fromState.getDepth()
							+ nodeEnabledDepth);
					if (newVisit.getDepth() <= maxDepth) {
						statesToDo.add(newVisit);
					}
				}
			}
		}
	}

	/**
	 * determines the new model move set.
	 * 
	 * Simply put, it builds A' = A union ((E' \ E) intersection ( E_s)), where
	 * E' are the enabled nodes in the new state, E are the enabled nodes in the
	 * from state, E_s are the nodes that are in the scope of the model and A is
	 * the cu
	 * 
	 * 
	 * @param scopedModelMoves
	 * @param currentModelMoves
	 * @param fromState
	 * @param newState
	 * @return
	 */
	public NodeSet determineNewAllowedModelMoves(NodeSet scopedModelMoves, NodeSet currentModelMoves, byte[] fromState,
			byte[] newState) {
		NodeSet newModelMoves = new NodeSet(currentModelMoves);

		for (int node = treeSize; node-- > 0;) {
			if (statebuilder.isEnabled(newState, node) && !statebuilder.isEnabled(fromState, node)) {
				newModelMoves.add(node);
			}
		}
		if (scopedModelMoves != null) {
			newModelMoves.retainAll(scopedModelMoves);
		}

		return newModelMoves;
	}

	private void getSyncronousMoveSequencesTermination(S currentState, StateLabel stateLabel, TObjectIntMap<M> result) {

		Set<S> statesDone = new HashSet<S>();

		//		FastLookupPriorityQueue statesToDo = new FastLookupPriorityQueue(32);

		// we need to keep track of the state we visited, sorted by cost of reaching it
		// as a first order and depth as a second order criterion.
		TreeSet<NAryTreeStateVisit<S>> statesToDo = createToDoSet();

		statesToDo.add(new NAryTreeStateVisit<S>(currentState, null, null, new NodeSet(treeSize, true), 0, 0));

		//Consider each leaf node at most once for each state in the queue
		TIntSet labeledLeafs = new TIntHashSet(treeSize, 0.7f, -1);
		// initialize the labeled leafs.
		for (int n = 0; n < treeSize; n++) {
			if (tree.isLeaf(n)) {
				labeledLeafs.add(n);
			}
		}

		NodeSet modelMoves;
		while (!statesToDo.isEmpty() && result.size() < stopAt && !labeledLeafs.isEmpty()) {
			//			NAryTreeStateVisit fromState = statesToDo.poll();
			NAryTreeStateVisit<S> fromState = statesToDo.pollFirst();
			if (!statesDone.add(fromState.getState())) {
				continue;
			}

			modelMoves = fromState.getAllowedMoves();
			int cost = fromState.getCostSoFar();

			if (statebuilder.isFinal(fromState.getState().getState())) {
				//we were looking for the final state and we reached it
				M ms = createMovementSequence(currentState, fromState, fromState.getState(), null, NONODE, cost);
				if (result.get(ms) > ms.getTotalCost()) {
					result.put(ms, ms.getTotalCost());
				}
			}

			if (fromState.getDepth() > depth) {
				depth = fromState.getDepth();
			}
			poll++;

			// iterate over the enabled node in this state, 
			TIntIterator it = statebuilder.enabledIterator(fromState.getState().getState());

			while (it.hasNext() && result.size() < stopAt && !labeledLeafs.isEmpty()) {
				int nodeEnabled = it.next();

				if (nodeEnabled < treeSize) {
					if (!modelMoves.contains(nodeEnabled)) {
						continue;
					}
					modelMoves.remove(nodeEnabled);
				} else {
					// If we can terminate an OR, we always need to do so when looking for termination
					// and we should not consider any scenario in which the children are executed
					modelMoves.removeInterval(nodeEnabled - treeSize, tree.getNextFast(nodeEnabled - treeSize));
				}

				if (nodeEnabled > 0 && nodeEnabled < treeSize
						&& tree.getTypeFast(tree.getParentFast(nodeEnabled)) == NAryTree.OR) {
					// a node under an OR node. Do not execute if 
					// OR ready to terminate
					if (statebuilder.isEnabled(fromState.getState().getState(), tree.getParentFast(nodeEnabled))) {
						continue;
					}
				}

				byte[] newState = statebuilder.executeAll(fromState.getState().getState(), nodeEnabled);

				// We need to build a new set of allowed model moves, which contains
				// all current modelMoves as well as all newly added ones after executing this node as long
				// as they are within the scope of the label we are looking for
				NodeSet newModelMoves = determineNewAllowedModelMoves(null, modelMoves,
						fromState.getState().getState(), newState);

				int nodeEnabledCost = getCostForNode(nodeEnabled);
				int nodeEnabledDepth = getDepthForNode(nodeEnabled);
				S toState = createState(currentState, newState);

				if (!statesDone.contains(toState)) {
					// We haven't seen this state, no need to reschedule as we reached it with higher cost
					// continue the search after queueing toState
					NAryTreeStateVisit<S> newVisit = new NAryTreeStateVisit<S>(toState, fromState, new NAryTreeNode(
							nodeEnabled), newModelMoves, cost + nodeEnabledCost, fromState.getDepth()
							+ nodeEnabledDepth);
					statesToDo.add(newVisit);

				}
				//				}
			}
		}
	}

	public TreeSet<NAryTreeStateVisit<S>> createToDoSet() {
		return new TreeSet<NAryTreeStateVisit<S>>() {
			private static final long serialVersionUID = 1L;

			public String toString() {
				StringBuilder builder = new StringBuilder();
				for (NAryTreeStateVisit<S> v : this) {
					builder.append("{");
					builder.append(statebuilder.toString(v.getState().getState()));
					builder.append(",");
					builder.append(v.getCostSoFar());
					builder.append("},");

				}
				return builder.toString();
			}
		};
	}

	@SafeVarargs
	protected final <T> T[] toArray(int length, T... array) {
		return Arrays.copyOf(array, length);
	}

	protected abstract S createState(S predecessor, byte[] newState);

	public abstract S getInitialState();

	public abstract S createCopy(S currentState);

	/**
	 * Create a movementsequence from "startState" to "toState". The given
	 * "lastState" is the last visited state from which a move can be executed
	 * with the given label (MoveModel is label == null, SyncMove is label
	 * !=null) to reach "toState". The cost represent the cost for the entire
	 * sequence, including the last element
	 * 
	 * 
	 * @param currentState
	 * @param toState
	 * @param label
	 * @param nodeEnabled
	 * @param cost
	 * @return
	 */
	protected abstract M createMovementSequence(S startState, NAryTreeStateVisit<S> lastState, S toState,
			NAryTreeLabel label, int nodeEnabled, int cost);

	protected abstract M appendOrToMovementSequence(M ms, S toState, int orNode, int costForNode);

	protected int getCostForNode(int nodeEnabled) {
		if (nodeEnabled >= treeSize) {
			// terminating an OR
			// different from offline replayer, where this has cost 0 because 
			// of the difficulties when doing ILP
			return 1;
		}
		return 1 + (tree.isHidden(0, nodeEnabled) ? 0 : scaling) * moveModelCost.get(nodeEnabled);
	}

	protected int getDepthForNode(int nodeEnabled) {
		if (nodeEnabled >= treeSize || !tree.isLeaf(nodeEnabled) || tree.getTypeFast(nodeEnabled) == NAryTree.TAU) {
			return 0;
		}
		return 1;
	}

	public boolean isFinal(S currentState) {
		return statebuilder.isFinal(currentState.getState());
	}

	public Map<StateLabel, M[]> getCache() {
		return cache;
	}

	public int getHits() {
		return hit;
	}

	public int getMisses() {
		return miss;
	}

	public int getPolls() {
		return poll;
	}

	public int getDepth() {
		return depth;
	}

	public void resetHitMiss() {
		hit = 0;
		miss = 0;
		poll = 0;
		depth = 0;
	}

	//	private int[] computeScope(byte[] state, int lastMove) {
	//		// we reached this record through a modelMove
	//
	//		// we reached rec through a model move
	//		// determine the subtree we are in by looking at the first parent of lastMove which
	//		// is not state N 
	//		// We also skip over parents in state F, as these are LOOP-REDO nodes, unless
	//		// they are OR nodes with at least one child being executed.
	//
	//		int scope = lastMove > treeSize ? lastMove - treeSize : lastMove;
	//		int lastNode = scope;
	//		int s = statebuilder.getState(state, scope);
	//		// p is the parent of lastMove. 
	//		while (s == StateBuilder.N
	//				|| (s == StateBuilder.F && (tree.getTypeFast(scope) != NAryTree.OR || allChildrenN(state, scope)))) {
	//			scope = tree.getParentFast(scope);
	//			s = statebuilder.getState(state, scope);
	//		}
	//		// p is the grandparent of lastMove which is not in state N, so allow only for 
	//		// modelmoves which are in the scope of p
	//		if (s == StateBuilder.E) {
	//			// special case. We just finished a subtree which re-enabled itself. Can only be if
	//			// scope is the middle child of a loop and the leftmost child of that loop is a TAU
	//			// or a hidden subtree
	//			assert tree.getTypeFast(tree.getParentFast(scope)) == NAryTree.LOOP;
	//			assert scope == tree.getNextFast(tree.getParentFast(scope) + 1);
	//			// make sure to allow for the other sibling to be in the scope
	//			scope = tree.getParentFast(scope);
	//		}
	//
	//		if (scope < lastNode && tree.getTypeFast(scope) == NAryTree.AND) {
	//			// if the type of node scope is AND, choose the first 
	//			// child of the AND in which there is something enabled
	//			// i.e. the first subtree of the AND in state != N
	//			int i = scope + 1;
	//			do {
	//				if ((i < lastNode) || statebuilder.getState(state, i) == StateBuilder.N) {
	//					i = tree.getNextFast(i);
	//				} else {
	//					return new int[] { i, tree.getNextFast(i) };
	//				}
	//			} while (tree.getParent(i) == scope);
	//			// allow no movel moves
	//			return new int[] { 0, -1 };
	//		}
	//		if (scope < lastNode && tree.getTypeFast(scope) == NAryTree.OR) {
	//			// if the type of scope is OR, allow for all nodes in 
	//			// under scope larger than lastMove, but under the OR
	//
	//			//return new int[] { scope, lastNode };
	//			return new int[] { lastNode + 1, tree.getNextFast(scope) };
	//		}
	//
	//		return new int[] { scope, tree.getNextFast(scope) };
	//	}
	//
	//	private boolean allChildrenN(byte[] state, int scope) {
	//
	//		boolean allN = true;
	//		int c = scope + 1;
	//		do {
	//			allN &= statebuilder.getState(state, c) == StateBuilder.N;
	//			c = tree.getNextFast(c);
	//		} while (allN && tree.getParent(c) == scope);
	//
	//		return allN;
	//	}

	public int getScaling() {
		return scaling;
	}

	public void setScaling(int scaling) {
		this.scaling = scaling;
	}

	protected NodeSet getScope(int treeSize, short nodeLabel) {
		NodeSet moves = new NodeSet(treeSize);

		for (int n = 0; n < treeSize; n++) {
			if (tree.getTypeFast(n) == nodeLabel) {
				fillSetUp(moves, n);
			}
		}
		return moves;
	}

	// adds all the parents of a leaf to the given set.
	private void fillSetUp(NodeSet set, int n) {
		int last = n;
		do {
			set.add(n);
			//			if (tree.getTypeFast(n) == NAryTree.OR)
			//				set.add(n + treeSize);
			if (tree.getTypeFast(n) == NAryTree.ILV) {
				// add entire subtree of all non-leaf nodes.
				int c = n + 1;
				do {
					if (!tree.isLeaf(c)) {
						set.addInterval(c, tree.getNextFast(c));
					}
					c = tree.getNextFast(c);
				} while (c < tree.size() && tree.getParentFast(c) == n);
			} else if (tree.getTypeFast(n) == NAryTree.SEQ) {
				// also add all children up to last
				for (int i = n + 1; i < last; i++) {
					set.add(i);
					//					if (tree.getTypeFast(i) == NAryTree.OR)
					//						set.add(i + treeSize);
				}
			} else if (tree.getTypeFast(n) == NAryTree.REVSEQ) {
				// also add all children from last
				for (int i = last; i < tree.getNextFast(n); i++) {
					set.add(i);
					//					if (tree.getTypeFast(i) == NAryTree.OR)
					//						set.add(i + treeSize);
				}
			} else if (tree.getTypeFast(n) == NAryTree.LOOP) {
				//last node was a child of loop.
				// Add the subtree under the first child and the second child
				for (int i = n + 1; i < tree.getNextFast(tree.getNextFast(n + 1)); i++) {
					set.add(i);
					//					if (tree.getTypeFast(i) == NAryTree.OR)
					//						set.add(i + treeSize);
				}
			}
			last = n;
			n = tree.getParentFast(n);
		} while (n != NAryTree.NONE);
	}
}
