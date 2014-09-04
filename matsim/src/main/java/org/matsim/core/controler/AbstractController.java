/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.controler;

import org.apache.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioImpl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractController {

    class UnexpectedShutdownException extends Exception {
    }

    private static Logger log = Logger.getLogger(AbstractController.class);

    private OutputDirectoryHierarchy controlerIO;

    /**
     * This was  public in the design that I found. kai, jul'12
     */
    public final IterationStopWatch stopwatch = new IterationStopWatch();

    /*
     * Strings used to identify the operations in the IterationStopWatch.
     */
    public static final String OPERATION_ITERATION = "iteration";

    /**
     * This is deliberately not even protected.  kai, jul'12
     */
    ControlerListenerManager controlerListenerManager;

    // for tests
    protected volatile Throwable uncaughtException;

    private AtomicBoolean unexpectedShutdown = new AtomicBoolean(false);

    @Deprecated
    /*package*/ Integer thisIteration = null;


    protected AbstractController() {
        OutputDirectoryLogging.catchLogEntries();
        Gbl.printSystemInfo();
        Gbl.printBuildInfo();
        log.info("Used Controler-Class: " + this.getClass().getCanonicalName());
        this.controlerListenerManager = new ControlerListenerManager();
    }


    private void resetRandomNumbers(long seed, int iteration) {
        MatsimRandom.reset(seed + iteration);
        MatsimRandom.getRandom().nextDouble(); // draw one because of strange
        // "not-randomness" is the first
        // draw...
        // Fixme [kn] this should really be ten thousand draws instead of just
        // one
    }

    protected final void setupOutputDirectory(final String outputDirectory, String runId, final boolean overwriteFiles) {
        this.controlerIO = new OutputDirectoryHierarchy(outputDirectory, runId, overwriteFiles); // output dir needs to be before logging
        OutputDirectoryLogging.initLogging(this.getControlerIO()); // logging needs to be early
    }

    protected final void run(Config config) {
        UncaughtExceptionHandler previousDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                // We want to shut down when any Thread dies with an Exception.
                logMemorizeAndRequestShutdown(t, e);
            }
        });
        final Thread controllerThread = Thread.currentThread();
        Thread shutdownHook = new Thread() {
            public void run() {
                log.error("received unexpected shutdown request.");
                // Request shutdown from the controllerThread.
                unexpectedShutdown.set(true);
                try {
                    // Wait until it has shut down.
                    controllerThread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // The JVM will exit when this method returns.
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            loadCoreListeners();
            this.controlerListenerManager.fireControlerStartupEvent();
            checkConfigConsistencyAndWriteToLog(config, "config dump before iterations start");
            prepareForSim();
            doIterations(config);
        } catch (UnexpectedShutdownException e) {
            // Doesn't matter. Just shut down.
        } catch (RuntimeException e) {
            // Don't let it fall through to the UncaughtExceptionHandler. We want to first log the Exception,
            // then shut down.
            logMemorizeAndRequestShutdown(Thread.currentThread(), e);
        } finally {
            shutdown();
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            Thread.setDefaultUncaughtExceptionHandler(previousDefaultUncaughtExceptionHandler);
            // Propagate Exception in case Controler.run is called by someone who wants to catch
            // it. It is probably not strictly correct to wrap the exception here.
            // But otherwise, this method would have to declare "throws Throwable".
            // In theory, a run method for test cases would probably need to
            // be different from the run method of the "MATSim platform" which
            // takes control of the JVM by installing hooks and exception
            // handlers.
            if (uncaughtException != null) {
                throw new RuntimeException(uncaughtException);
            }
        }
    }

    private void logMemorizeAndRequestShutdown(Thread t, Throwable e) {
        log.error("Getting uncaught Exception in Thread " + t.getName(), e);
        uncaughtException = e;
        unexpectedShutdown.set(true);
    }

    protected abstract void loadCoreListeners();

    protected abstract void runMobSim(int iteration);

    protected abstract void prepareForSim();

    /**
     * Stopping criterion for iterations.  Design thoughts:<ul>
     * <li> AbstractController only controls process, not content.  Stopping iterations controls process based on content.
     * All such coupling methods are abstract; thus this one has to be abstract, too.
     * <li> One can see this confirmed in the KnSimplifiedControler use case, where the function is delegated to a static
     * method in the SimplifiedControllerUtils class ... as with all other abstract methods.
     * </ul>
     */
    protected abstract boolean continueIterations(int iteration);

    private void doIterations(Config config) throws UnexpectedShutdownException {
        for (int iteration = config.controler().getFirstIteration(); continueIterations(iteration); iteration++) {
            iteration(config, iteration);
        }
    }

    private void shutdown() {
        log.info("S H U T D O W N   ---   start shutdown.");
        this.controlerListenerManager.fireControlerShutdownEvent(unexpectedShutdown.get());
        if (this.uncaughtException != null) {
            log.error("Shutdown possibly caused by the following Exception:", this.uncaughtException);
        }
        if (this.unexpectedShutdown.get()) {
            log.error("ERROR --- MATSim unexpectedly terminated. Please check the output or the logfile with warnings and errors for hints.");
            log.error("ERROR --- results should not be used for further analysis.");
        }
        log.info("S H U T D O W N   ---   shutdown completed.");
        OutputDirectoryLogging.closeOutputDirLogging();
    }

    final String DIVIDER = "###################################################";
    final String MARKER = "### ";

    private void iteration(final Config config, final int iteration) throws UnexpectedShutdownException {
        this.thisIteration = iteration;
        this.stopwatch.beginIteration(iteration);

        log.info(DIVIDER);
        log.info(MARKER + "ITERATION " + iteration + " BEGINS");
        this.getControlerIO().createIterationDirectory(iteration);
        resetRandomNumbers(config.global().getRandomSeed(), iteration);

        iterationStep("iterationStartsListeners", new Runnable() {
            @Override
            public void run() {
                controlerListenerManager.fireControlerIterationStartsEvent(iteration);
            }
        });

        if (iteration > config.controler().getFirstIteration()) {
            iterationStep("replanning", new Runnable() {
                @Override
                public void run() {
                    controlerListenerManager.fireControlerReplanningEvent(iteration);
                }
            });
        }

        mobsim(config, iteration);

        iterationStep("scoring", new Runnable() {
            @Override
            public void run() {
                log.info(MARKER + "ITERATION " + iteration + " fires scoring event");
                controlerListenerManager.fireControlerScoringEvent(iteration);
            }
        });

        iterationStep("iterationEndsListeners", new Runnable() {
            @Override
            public void run() {
                log.info(MARKER + "ITERATION " + iteration + " fires iteration end event");
                controlerListenerManager.fireControlerIterationEndsEvent(iteration);
            }
        });

        this.stopwatch.endIteration();
        this.stopwatch.writeTextFile(this.getControlerIO().getOutputFilename("stopwatch"));
        if (config.controler().isCreateGraphs()) {
            this.stopwatch.writeGraphFile(this.getControlerIO().getOutputFilename("stopwatch"));
        }
        log.info(MARKER + "ITERATION " + iteration + " ENDS");
        log.info(DIVIDER);
    }

    private void mobsim(final Config config, final int iteration) throws UnexpectedShutdownException {
        // ControlerListeners may create managed resources in
        // beforeMobsim which need to be cleaned up in afterMobsim.
        // Hence the finally block.
        // For instance, ParallelEventsManagerImpl leaves Threads waiting if we don't do this
        // and an Exception occurs in the Mobsim.
        try {
            iterationStep("beforeMobsimListeners", new Runnable() {
                @Override
                public void run() {
                    controlerListenerManager.fireControlerBeforeMobsimEvent(iteration);
                }
            });

            iterationStep("mobsim", new Runnable() {
                @Override
                public void run() {
                    resetRandomNumbers(config.global().getRandomSeed(), iteration);
                    runMobSim(iteration);
                }
            });
        } finally {
            iterationStep("afterMobsimListeners", new Runnable() {
                @Override
                public void run() {
                    log.info(MARKER + "ITERATION " + iteration + " fires after mobsim event");
                    controlerListenerManager.fireControlerAfterMobsimEvent(iteration);
                }
            });
        }
    }

    private void iterationStep(String iterationStepName, Runnable iterationStep) throws UnexpectedShutdownException {
        this.stopwatch.beginOperation(iterationStepName);
        iterationStep.run();
        this.stopwatch.endOperation(iterationStepName);
        if (this.unexpectedShutdown.get()) {
            throw new UnexpectedShutdownException();
        }
    }


    /**
     * Design decisions:
     * <ul>
     * <li>I extracted this method since it is now called <i>twice</i>: once
     * directly after reading, and once before the iterations start. The second
     * call seems more important, but I wanted to leave the first one there in
     * case the program fails before that config dump. Might be put into the
     * "unexpected shutdown hook" instead. kai, dec'10
     *
     * Removed the first call for now, because I am now also checking for
     * consistency with loaded controler modules. If still desired, we can
     * put it in the shutdown hook.. michaz aug'14
     *
     * </ul>
     *
     * @param config  TODO
     * @param message the message that is written just before the config dump
     */
    protected static final void checkConfigConsistencyAndWriteToLog(Config config,
                                                                    final String message) {
        log.info(message);
        String newline = System.getProperty("line.separator");// use native line endings for logfile
        StringWriter writer = new StringWriter();
        new ConfigWriter(config).writeStream(new PrintWriter(writer), newline);
        log.info(newline + newline + writer.getBuffer().toString());
        log.info("Complete config dump done.");
        log.info("Checking consistency of config...");
        config.checkConsistency();
        log.info("Checking consistency of config done.");
    }

    /**
     * Design comments:<ul>
     * <li> This is such that ControlerListenerManager does not need to be exposed.  One may decide otherwise ...  kai, jul'12
     * </ul>
     */
    public final void addControlerListener(ControlerListener l) {
        this.controlerListenerManager.addControlerListener(l);
    }

    protected final void addCoreControlerListener(ControlerListener l) {
        this.controlerListenerManager.addCoreControlerListener(l);
    }


    public final OutputDirectoryHierarchy getControlerIO() {
        return controlerIO;
    }


}
