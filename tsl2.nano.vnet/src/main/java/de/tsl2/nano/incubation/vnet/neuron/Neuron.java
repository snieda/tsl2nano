/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 11.11.2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.vnet.neuron;

/**
 * 
 * @author ts
 * @version $Revision$
 */
public class Neuron<T> {
    Core core;
    T kernel = null;
    float feeding = 0;
    float linking = 0;
    float threshold = 0;
    float inhibition = 0;

    float mempot = 0;
    int output = 0;

    public static boolean stopFiringThreads = false; //stops all firing threads
    public static final int FIRE = 1;
    public static final int QUIET = 0;

    /**
     * constructor
     * 
     * @param kernel
     */
    public Neuron(T kernel) {
        super();
        this.kernel = kernel;
        core = new Core();
    }

    public int feedSignal(float signal) {
        return feedSignal(signal, true);
    }
    
    public int feedSignal(float signal, boolean fireWithOutput) {
        feeding = signal * core.Fv;
        mempot += feeding;
//    		Console.printDebug(toStringDescription());
        if (mempot >= threshold + core.To) {
            return fire(fireWithOutput);
        }
        return output = QUIET;
    }

    int fire(boolean withOutput) {
        output = FIRE;
        threshold += output * core.Tv;
        if (withOutput) {
            System.out.print(toString() + " ");
        }
//    // Gib Signal an umliegende Neuronen (in eigenem Thread) weiter
//    		if (!stopFiringThreads)
//    		    new Thread(this, kernel.toString()).start();
        return FIRE;
    }

    public T getKernel() {
        return kernel;
    }
    
    @Override
    public String toString() {
        return kernel + " {M:" + mempot + ", O:" + output + "}";
    }
    
    class Core {
        String name = null;
        int minSize = 10; //for memory allocation to improve performance
        int minLink = 10; //for memory allocation to improve performance
        float linkPower = 0.5f; //default linkpower on creation
        float Fv, /* amplify the feeding */
        Ft, /* decrease the feeding with e-function */
        Tv, /* parameters for threshold */
        Tt, To, /* Offset */
        Lv, /* for linking */
        Lt, Gv, /* for global inihibition */
        Gt, LL, /* lateral linking */
        NF, /* factor for noise */
        DL, /* delay */
        NR; /* noise ratio */
    }
}
