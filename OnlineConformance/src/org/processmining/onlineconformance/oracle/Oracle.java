package org.processmining.onlineconformance.oracle;

import java.util.Collection;

/**
 * The oracle allows a replay algorithm to get advice on what the possible moves
 * are. The oracle should always return the cheapest possible sequence of moves
 * to enable a synchronous move on a label.
 * 
 * Any implementation of the Oracle should be able to execute a move with a
 * given label in a given state, i.e. there should not be any nondeterminism as
 * to the question what the next state is when executing a move labelled <l,m>
 * in state s. For Petri-net based or tree-based oracles, this implies that m
 * should refer to the transition or leaf respectively, while l refers to the
 * label of the event in the log. Internally, a mapping between these needs to
 * be kept.
 * 
 * @author bfvdonge
 * 
 * @param <S>
 */
public interface Oracle<S, L, M> {

	/**
	 * Returns a movementsequence needed to synchronously execute an activity
	 * labeled L. The sequence of moves is null if and only if there is no
	 * possibility to execute an activity L now or in the future according to
	 * the Oracle.
	 * 
	 * If the movementsequence is not null, then the oracle found a sequence of
	 * moves to execute an activity labeled with <code>label</code> in the
	 * future. The movementSequence has information about the total cost of the
	 * moves as well as the state reached. This state does not need to be a
	 * fresh copy as the replayer takes care of copying the state.
	 * 
	 * If <code>label</code> is null, then a list of modelmoves is given to
	 * reach the end state
	 * 
	 * @param currentState
	 * @param label
	 * @return
	 */
	public Collection<? extends MovementSequence<S, L, M>> getSyncronousMoveSequences(S currentState, L label);

	/**
	 * creates a fresh copy of the current state, i.e. a new object which
	 * represents the same state.
	 * 
	 * @param currentState
	 * @return
	 */
	public S createCopy(S currentState);

	/**
	 * determines if the currentstate is a final state
	 * 
	 * @param currentState
	 * @return
	 */
	public boolean isFinal(S currentState);

	/**
	 * returns the initial state in which all process instances are assumed to
	 * begin
	 * 
	 * @return
	 */
	public S getInitialState();

	/**
	 * Like a regular replayer, the oracle uses a scaling parameter to ensure
	 * progress in the alignments. Set this scaling large enough, surely larger
	 * than the longest trace in the log + the shortest execution of the model.
	 * 
	 * @return
	 */
	public int getScaling();

	/**
	 * Like a regular replayer, the oracle uses a scaling parameter to ensure
	 * progress in the alignments. Set this scaling large enough, surely larger
	 * than the longest trace in the log + the shortest execution of the model.
	 * 
	 * @return
	 */
	public void setScaling(int scaling);

}
