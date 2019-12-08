package de.tsl2.nano.gp;

import static de.tsl2.nano.core.util.CLI.NC;
import static de.tsl2.nano.core.util.CLI.tag;
import static de.tsl2.nano.core.util.CLI.Color.GREEN;
import static de.tsl2.nano.core.util.CLI.Color.LIGHT_BLUE;
import static de.tsl2.nano.core.util.CLI.Color.RED;
import static de.tsl2.nano.core.util.MainUtil.log;
import static de.tsl2.nano.core.util.MainUtil.logn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import de.tsl2.nano.core.util.MainUtil;

/**
 * the evolutational algorithm tries to find a solution, defined and measured up
 * by a given fitness function, through randomized mutation of
 * object-properties.<p/>
 * for convenience, the object properties are simplified to a sequence of numbers. the
 * genom class holds the mutable sequence of long - so the fitness function has to 
 * transform the numbers into its calculating objects (vectors, matrices, object-ids).  
 */
public class EvolutionalAlgorithm {
    static final String BADEST_FITNESS = "badestFitness";
    static final String FITNESS_FUNCTION = "fitnessFunction";
    static final String GENETIC_RANGE_LOW = "geneticRangeLow";
    static final String GENETIC_RANGE_HIGH = "geneticRangeHigh";
    static final String FINAL_FITNESS = "finalFitness";
    static final String MAX_GENERATION = "maxGeneration";
    static final String MAX_SEQUENCE = "maxSequence";
    static final String MAX_POPULATION = "maxPopulation";
    
    SortedSet<Genom> creatures;
    Function<Long, Double> fitnessFunction;
    int generations;
    int population;
    int mutations;
    Properties conf;
    private long start;

    public EvolutionalAlgorithm(String[] args) {
        this(MainUtil.toProperties("", args, EvolutionalAlgorithm.class, true, getArgNames()));
    }

    public EvolutionalAlgorithm(Properties conf) {
        this.conf = conf;
        creatures = new TreeSet<>();
        if (isModeAllParallel())
            creatures = Collections.synchronizedSortedSet(creatures);
    }

    public static final void main(String[] args) {
        new EvolutionalAlgorithm(args).run();
    }

    static String[] getArgNames() {
        return new String[]{          
        "geneticRangeLow : {@java.lang.Long}[default:1]", 
        "geneticRangeHigh: {@java.lang.Long}[default:10]", 
        "maxSequence     : {@java.lang.Long}[default:10]", 
        "maxGeneration   : {@java.lang.Long}[default:1000]", 
        "maxPopulation   : {@java.lang.Long}[default:1000]", 
        "populationMode  : {MODE_ALL_PARALLEL, MODE_LEADER_RECURSIVE}[default: MODE_LEADER_RECURSIVE]", 
        "badestFitness   : {@java.lang.Double}[default: 99999]",
        "finalFitness    : {@java.lang.Double:0..1}[default: 0.1d] break condition => fitness will be used as solution",
        "shrinkFactor    : {@java.lang.Double:0..1}[default: 0.5d] propability to shrink a genom",
        "growFactor      : {@java.lang.Double:0..1}[default: 0.5d] propability to grow a genom",
        "fitnessFunction : {@java.util.function.Function}[default: de.tsl2.nano.gp.PolyglottFitnessFunction]",
        "-debug          : extended console output"};
    }

    public void run() {
        start = System.currentTimeMillis();
        creatures.add(new Genom(this, Arrays.asList(randomBase())));
        long maxGenerations = (long) get(MAX_GENERATION);
        double finalFitness = (double) get(FINAL_FITNESS);
        for (int i = 0; i < maxGenerations; i++) {
            if (nextGeneration() <= finalFitness) {
                logn(tag("\nSUCCESS!", GREEN));
                break;
            }
        }
        if (generations == maxGenerations)
            logn(tag("\nFAILED!", RED));
        Genom best = creatures.last();
        logn("\n=============================================================================");
        logc("creatures            : " + creatures.size());
        logc("generations evaluated: " + generations);
        logc("duration (sec)       : " + (System.currentTimeMillis() - start) / 1000);
        logc("best creature is     : " + best);
        logn("=============================================================================");
    }

    void logc(Object txt) {
        logn(txt, ":", LIGHT_BLUE, GREEN);
    }

    double nextGeneration() {
        generations++;
        population = creatures.size();
        mutations = 0;
        dieUnfitest();
        if (isModeAllParallel()) {
            Collection<Genom> children = new ArrayList<>(creatures.size());
            creatures.stream().forEach(g -> populate(children, g, false));
            creatures.addAll(children);
            return leader().fitness();
        } else
            return populate(creatures, leader(), true);
    }

    private boolean isModeAllParallel() {
        return get("populationMode").equals("MODE_ALL_PARALLEL");
    }

    private void dieUnfitest() {
        while (creatures.size() > get(MAX_POPULATION, 1000l))
            if (!creatures.remove(creatures.first()))
                throw new IllegalStateException(creatures.last() + " is last but not found treeset element!");
    }

    private double populate(Collection<Genom> children, Genom creature, boolean recursive) {
        mutations++;
        if (!is("debug")) {
            long t = (System.currentTimeMillis() - start) / 1000;
            log(tag(GREEN) + "\r" + t + ": best fitness: " + creature.fitness() + " generations: " + generations + " population: " + population
                    + " mutations: " + mutations + " sequence: " + creature.sequence + NC);
        }
        if (creature.fitness() <= (double) get(FINAL_FITNESS))
            return creature.fitness();
        if (creature.generations * 10 > (long) get(MAX_GENERATION)) {
            creatures.remove(creature);
            return (double) get(BADEST_FITNESS);
        }
        if (mutations > population)
            return creature.fitness();
        children.add(creature.clone());
        return recursive ? populate(children, leader(), recursive) : creature.fitness();
    }

    double fitnessFunction(List<Long> sequence) {
        return (double) ((Function) get(FITNESS_FUNCTION)).apply(sequence.toArray(new Long[0]));
    }

    Long randomBase() {
        long high = (long) get(GENETIC_RANGE_HIGH);
        long low = (long) get(GENETIC_RANGE_LOW);
        return (long) (Math.random() * (high - low)) + low;
    }

    public Object get(String name) {
        Object v = get_(name);
        if (v == null)
            throw new IllegalArgumentException(name + " must be set!");
        return v;
    }
    public Object get_(String name) {
        return conf.get(name);
    }

    boolean is(String name) {
        return get_(name) != null ? (Boolean) get_(name) : false;
    }

    public <T> T get(String name, T defaultValue) {
        T v = (T) conf.get(name);
        if (v == null)
            v = defaultValue;
        return v;
    }

    Genom leader() {
        return creatures.last();
    }

}

/**
 * the genom is representated through a sequence of numbers. the numbers may be
 * interpreted by the fitness-function as real numbers, as vectors or matrices -
 * or as ids for special objects, the fitness-function is able to load.
 */
class Genom implements Comparable<Genom>, Cloneable {
    List<Long> sequence;
    int generations;
    private Double fitness;
    private EvolutionalAlgorithm algorithm;

    public Genom(EvolutionalAlgorithm algorithm, List<Long> sequence) {
        this.algorithm = algorithm;
        this.sequence = new ArrayList<Long>(sequence);
    }

    @Override
    public Genom clone() {
        generations++;
        Genom g = new Genom(algorithm, sequence);
        return g.mutate();
    }

    Genom mutate() {
        int mutationIndex = (int) (Math.random() * sequence.size());
        sequence.set(mutationIndex, algorithm.randomBase());
        boolean shrink = 0 < sequence.size() && Math.random() > algorithm.get("shrinkFactor", 0.5d);
        if (shrink)
            sequence.remove((int) (Math.random() * sequence.size()));
        boolean grow = sequence.size() == 0 || ((long) algorithm.get(algorithm.MAX_SEQUENCE) > sequence.size() && Math.random() > algorithm.get("growFactor", 0.5d));
        if (grow)
            sequence.add(algorithm.randomBase());
        if (algorithm.is("debug"))
            logn(algorithm.generations + " mutation: " + this);
        return this;
    }

    public Double fitness() {
        if (fitness == null)
            fitness = algorithm.fitnessFunction(sequence);
        return fitness;
    }

    public int compareTo(Genom g) {
        int f = g.fitness().compareTo(fitness());
        int s = Integer.valueOf(sequence.size()).compareTo(g.sequence.size());
        // in a treemap, the result of a compare seems to be stored as hash key, so the treemap is not able to find the object if the compareTo changes....
        int c = Integer.valueOf(g.generations).compareTo(generations);
        return f != 0 ? f : c != 0 ? c : s;// != 0 ? s : Integer.valueOf(hashCode()).compareTo(g.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Genom && compareTo((Genom) obj) == 0;
    }

    @Override
    public String toString() {
        return sequence + " [fitness: " + fitness() + ", generations: " + generations + "]";
    }
}
