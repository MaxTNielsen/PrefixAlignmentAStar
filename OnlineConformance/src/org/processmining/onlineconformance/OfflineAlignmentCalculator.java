package org.processmining.onlineconformance;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XEventImpl;
import org.processmining.onlineconformance.models.impl.MoveImpl;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeHistoryAwareMovementSequence;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeLabel;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeNode;
import org.processmining.plugins.etm.model.narytree.NAryTree;
import org.processmining.plugins.etm.model.narytree.replayer.AStarAlgorithm;
import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeDelegate;
import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPDelegate;
import org.processmining.plugins.etm.model.narytree.replayer.NAryTreeHead;
import org.processmining.plugins.etm.model.narytree.replayer.StubbornNAryTreeAStarThread;
import org.processmining.plugins.etm.model.narytree.replayer.StubbornNAryTreeAStarThread.MemoryEfficient;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;
import org.processmining.plugins.etm.model.narytree.replayer.empty.NAryTreeEmptyDelegate;
import org.processmining.plugins.etm.model.narytree.replayer.hybridilp.NAryTreeHybridILPDelegate;
import org.processmining.plugins.etm.model.narytree.test.LogCreator;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import nl.tue.astar.AStarException;
import nl.tue.astar.AStarThread;
import nl.tue.astar.AStarThread.ASynchronousMoveSorting;
import nl.tue.astar.AStarThread.Canceller;
import nl.tue.astar.AStarThread.QueueingModel;
import nl.tue.astar.AStarThread.Type;
import nl.tue.astar.Tail;
import nl.tue.astar.Trace;
import nl.tue.astar.impl.memefficient.MemoryEfficientAStarAlgorithm;
import nl.tue.astar.util.LinearTrace;

public class OfflineAlignmentCalculator {

	private static final Canceller CANCELLER = new Canceller() {

		public boolean isCancelled() {
			return false;
		}

	};

	protected final AbstractNAryTreeDelegate<? extends Tail> delegate;
	protected final MemoryEfficientAStarAlgorithm<NAryTreeHead, ?> memEffAlg;
	protected final AStarAlgorithm algorithm;
	protected final XEventClasses classes;
	protected final NAryTree tree;
	private boolean reliable;
	protected final double timeLimit;

	public OfflineAlignmentCalculator(NAryTree tree, String[] activities, String[][] traces, double timeLimit,
			TObjectIntMap<NAryTreeLabel> logMoveCost, TIntIntMap modelMoveCost, boolean useFuture) {

		this.tree = tree;
		this.timeLimit = timeLimit;
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryNaiveImpl());
		XLog log = LogCreator.createLog(traces);

		classes = new XEventClasses(new XEventNameClassifier());
		Map<XEventClass, Integer> activity2Cost = new HashMap<XEventClass, Integer>();
		for (int i = 0; i < activities.length; i++) {
			XEventImpl e = new XEventImpl();
			XAttribute a = new XAttributeLiteralImpl(XConceptExtension.KEY_NAME, activities[i]);
			e.getAttributes().put(XConceptExtension.KEY_NAME, a);
			classes.register(e);
			activity2Cost.put(new XEventClass(activities[i], i), logMoveCost.get(new NAryTreeLabel((short) i)));
		}

		algorithm = new AStarAlgorithm(log, classes, activity2Cost);

		int[] node2Cost = new int[tree.size()];
		for (int i = 0; i < tree.size(); i++) {
			if (tree.isLeaf(i) && tree.getType(i) != NAryTree.TAU) {
				node2Cost[i] = modelMoveCost.get(i);
			}
		}

		if (useFuture) {
			delegate = new NAryTreeHybridILPDelegate(algorithm, tree, 0, node2Cost, 1, false);
		} else {
			delegate = new NAryTreeEmptyDelegate(algorithm, tree, 0, node2Cost, 1);
		}
		memEffAlg = new MemoryEfficientAStarAlgorithm<>(delegate);

	}

	public Trace getTrace(String[] events) {
		LinearTrace trace = new LinearTrace("", events.length);
		for (int i = 0; i < events.length; i++) {
			trace.set(i, algorithm.getIndexOf(classes.getByIdentity(events[i])));
		}
		return trace;
	}

	public TreeRecord replayTrace(Trace trace) throws AStarException {
		delegate.setPushDownUnderAND(false);
		NAryTreeHead initialHead = new NAryTreeHead(delegate, trace);
		MemoryEfficient<NAryTreeHead, ?> thread = new StubbornNAryTreeAStarThread.MemoryEfficient<>(tree, memEffAlg,
				initialHead, trace, Integer.MAX_VALUE);
		thread.setQueueingModel(QueueingModel.DEPTHFIRST);
		thread.setType(Type.PLAIN);
		thread.setASynchronousMoveSorting(ASynchronousMoveSorting.LOGMOVEFIRST);

		TreeRecord rec = (TreeRecord) thread.getOptimalRecord(CANCELLER, timeLimit);
		reliable = thread.wasReliable();
		return rec;
	}

	public int getCacheSize() {
		return memEffAlg.getStatespace().size();
	}

	public NAryTreeHistoryAwareMovementSequence<?> createMovements(TreeRecord rec, Trace trace) {
		// The true alignment can be reconstructed from rec
		LinkedList<MoveImpl<NAryTreeLabel, NAryTreeNode>> list = new LinkedList<MoveImpl<NAryTreeLabel, NAryTreeNode>>();
		while (rec.getPredecessor() != null) {
			MoveImpl<NAryTreeLabel, NAryTreeNode> move;
			if (rec.getModelMove() == AStarThread.NOMOVE) {
				int cost = delegate.getCostFor(AStarThread.NOMOVE, trace.get(rec.getMovedEvent()));
				//				cost /= delegate.getScaling();
				move = new MoveImpl<>(new NAryTreeLabel((short) trace.get(rec.getMovedEvent())), null, cost);
			} else if (rec.getMovedEvent() == AStarThread.NOMOVE) {
				int cost = delegate.getCostFor(rec.getModelMove(), AStarThread.NOMOVE);
				//				cost /= delegate.getScaling();
				move = new MoveImpl<>(null, new NAryTreeNode(rec.getModelMove()), cost);
			} else {
				int cost = delegate.getCostFor(rec.getModelMove(), trace.get(rec.getMovedEvent()));
				//				cost /= delegate.getScaling();
				move = new MoveImpl<>(new NAryTreeLabel((short) trace.get(rec.getMovedEvent())),
						new NAryTreeNode(rec.getModelMove()), cost);
			}
			//			// internal moves
			//			int[] im = rec.getInternalMoves();
			//			for (int i = im.length; i-- > 0;) {
			//				list.add(0, new Move<>((NAryTreeLabel) null, new NAryTreeNode(im[i]), 0));
			//			}
			list.add(0, move);

			rec = rec.getPredecessor();
		}

		return new NAryTreeHistoryAwareMovementSequence<>(null, list);

	}

	public boolean wasReliable() {
		return reliable;
	}

	public void deleteLPs() {
		if (delegate instanceof AbstractNAryTreeLPDelegate) {
			((AbstractNAryTreeLPDelegate<?>) delegate).deleteLPs();
		}
	}
}
