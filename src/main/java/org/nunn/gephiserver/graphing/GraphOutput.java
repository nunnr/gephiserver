package org.nunn.gephiserver.graphing;

public class GraphOutput<OT> {
	
	private final OT output;
	
	public GraphOutput(OT output) {
		this.output = output;
	}

	public OT getOutput() {
		return output;
	}
	
}