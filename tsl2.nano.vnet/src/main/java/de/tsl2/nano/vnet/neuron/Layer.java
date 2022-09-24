package de.tsl2.nano.vnet.neuron;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.tsl2.nano.core.util.ValueSet;
import de.tsl2.nano.structure.IConnection;
import de.tsl2.nano.vnet.Net;

/**
 * layer properties to be used by each neuron
 * @author ts
 */
public class Layer extends ValueSet<Layer.Parameter, Float> {
	private Net<VNeuron, Float> net;
	ArrayList<Neuron<?>> neurons;
	private static Layer defaultLayer;
	
    String name = null;
    int minSize = 10; //for memory allocation to improve performance
    int minLink = 10; //for memory allocation to improve performance
    float linkPower = 0.5f; //default linkpower on creation

    Map<Parameter, Float> values;
    enum Parameter { 
        Fv, /* amplify the feeding */
        Ft, /* decrease the feeding with e-function */
        Tv, /* parameters for threshold */
        Tt, To, /* Offset */
        Lv, /* for linking */
        Lt, Gv, /* for global inihibition */
        Gt, LL, /* lateral linking */
        NF, /* factor for noise */
        DL, /* delay */
        NR; /* noise ratio */
    };
    
	public Layer(Net<VNeuron, Float> net, String name, Float...values) {
		super(Parameter.class, values);
		this.net = net;
		this.name = name;
		calcTau();
	}

	void calcTau() {
		transform(Parameter.Ft, v -> calcTau(v));
		on(Parameter.Tt, (e, v) -> set(e, calcTau(v)));
		on(Parameter.Lt, (e, v) -> set(e, calcTau(v)));
		on(Parameter.Gt, (e, v) -> set(e, calcTau(v)));
	}

	private float calcTau(float tauValue) {
		return (float) Math.exp(-1 / tauValue);
	}

	public void timeStep() {
		neurons.forEach(n -> n.feedSignal(0));
	}
	
	public void notifyNeighbours(VNeuron n, float signal) {
		List<IConnection<VNeuron,Float>> connections = net.getNode(n).getConnections();
		//TODO: use parallel event handling
		connections.forEach(c -> c.getDestination().getCore().feedSignal(c.getDescriptor() * signal));
	}

	public static Layer getDefault(Net net) {
		if (defaultLayer == null) {
			defaultLayer = new Layer(net, "DEFAULT", 
					def(Parameter.Fv, 1.1f), 
					def(Parameter.Ft, 1.0f), 
					def(Parameter.Tv, 1.1f), 
					def(Parameter.Tt, 1.0f), 
					def(Parameter.To, 0.5f), 
					def(Parameter.Lv, 0.1f), 
					def(Parameter.Lt, 0.1f), 
					def(Parameter.Gv, 0.1f), 
					def(Parameter.Gt, 0.1f), 
					def(Parameter.LL, 0.1f), 
					def(Parameter.NF, 0.1f), 
					def(Parameter.DL, 0.1f), 
					def(Parameter.NR, 0.1f) );
		}
		return defaultLayer;
	}

}
