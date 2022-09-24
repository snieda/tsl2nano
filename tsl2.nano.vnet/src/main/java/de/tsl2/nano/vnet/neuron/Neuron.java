/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 11.11.2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.vnet.neuron;

import static de.tsl2.nano.vnet.neuron.Layer.Parameter.*;

import java.io.Serializable;

/**
 * 
 * @author ts
 * @version $Revision$
 */
public class Neuron<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	Layer l;
    T kernel = null;
    float feeding = 0;
    float linking = 0;
    float threshold = 0;
    float inhibition = 0;

    float mempot = 0;
    int output = 0;

    public static final int FIRE = 1;
    public static final int QUIET = 0;

    public Neuron(T kernel, Layer layer) {
        this.kernel = kernel;
		this.l = layer;
    }

    public int feedSignal(float signal) {
        return feedSignal(signal, true);
    }
    
    public int feedSignal(float signal, boolean fireWithOutput) {
        feeding = pulse(feeding, signal, l.get(Fv), l.get(Ft));
        mempot = feeding;
        if (mempot >= threshold + l.get(To)) {
            return fire(fireWithOutput);
        }
        return output = QUIET;
    }

	private float pulse(float current, float signal, float amp, float tau) {
		return signal * amp + current * tau;
	}

    int fire(boolean withOutput) {
        output = FIRE;
        threshold = pulse(threshold, output, l.get(Tv), l.get(Tt));
        if (withOutput) {
            System.out.print(toString() + " ");
        }
        return FIRE;
    }

    public T getKernel() {
        return kernel;
    }
    
    @Override
    public String toString() {
        return kernel + " {M:" + mempot + ", T: " + threshold + ", O:" + output + "}";
    }
}
