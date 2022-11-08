package org.processmining.onlineconformance.parameters;

public class IncrementalRevBasedReplayerParametersImpl<M, L, T> extends IncrementalReplayerParametersImpl<M, L, T> {

	private int lookBackWindow = Integer.MAX_VALUE;

	public int getLookBackWindow() {
		return lookBackWindow;
	}

	public void setLookBackWindow(int lookBackWindow) {
		this.lookBackWindow = lookBackWindow;
	}

}
