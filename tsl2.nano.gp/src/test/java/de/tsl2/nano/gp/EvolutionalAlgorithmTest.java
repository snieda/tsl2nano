package de.tsl2.nano.gp;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.tsl2.nano.core.util.MainUtil;

public class EvolutionalAlgorithmTest {

    @Test
    public void testMainHelp() {
        try {
            EvolutionalAlgorithm.main(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("syntax: " + EvolutionalAlgorithm.class.getSimpleName()));
        }

        try {
            EvolutionalAlgorithm.main(new String[] { "help" });
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("syntax: " + EvolutionalAlgorithm.class.getSimpleName()));
        }
    }

    @Test
    public void testMainRunningWithNoParameters() {
        try {
            EvolutionalAlgorithm.main(new String[] {});
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains(PolyglottFitnessFunction.EVO_FIT_SCRIPT));
        }
    }

    @Test
    public void testMainRunningWithNoDefaultsAndTestFitnessFunction() {
        String[] args = new String[] { "fitnessFunction=de.tsl2.nano.gp.TestFitnessFunction" };
        try {
            new EvolutionalAlgorithm(MainUtil.toProperties("", args, null, true, null)).run();
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be set!"));
        }
    }

    @Test
    public void testMainRunningWithLeader() {
        try {
            // System.setProperty("evolutionalalgorithm.fitnessfuntion.language", "python");
            System.setProperty("evolutionalalgorithm.fitnessfunction.script", "src/test/resources/fit.ts");
            String[] args = new String[] { "-debug1", "0", "100", "30", "1000", "1000", "MODE_LEADER_RECURSIVE", "99.0", "0.01",
                    "0.5", "0.5", "de.tsl2.nano.gp.PolyglottFitnessFunction" };
            EvolutionalAlgorithm.main(args);
        } finally {
            System.clearProperty("evolutionalalgorithm.fitnessfunction.script");
        }
    }

    @Test
    public void testMainRunningWithParallel() {
        // System.setProperty("evolutionalalgorithm.fitnessfuntion.language", "python");
        System.setProperty("evolutionalalgorithm.fitnessfunction.script", "src/test/resources/fit.ts");
        String[] args = new String[] { "-debug1", "0", "100", "30", "1000", "1000", "MODE_ALL_PARALLEL", "99.0", "0.01",
                "0.5", "0.5", "de.tsl2.nano.gp.PolyglottFitnessFunction" };
        EvolutionalAlgorithm.main(args);
    }

    @Test
    public void testMainRunningWithDefaultsAndTestFitnessFunction() {
        String[] args = new String[] { "fitnessFunction=de.tsl2.nano.gp.TestFitnessFunction" };
        EvolutionalAlgorithm ea = new EvolutionalAlgorithm(args);
        ea.run();

        assertTrue((double)ea.leader().fitness() <= (double)ea.get(EvolutionalAlgorithm.FINAL_FITNESS));
    }
}