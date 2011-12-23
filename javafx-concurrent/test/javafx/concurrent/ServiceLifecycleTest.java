/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.concurrent;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javafx.event.EventHandler;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests various rules regarding the lifecycle of a Service.
 */
public class ServiceLifecycleTest extends ServiceTestBase {
    /**
     * The ManualExecutor is used so that there is some time period between
     * when something is scheduled and when it actually runs, such that the
     * test code has to manually tell it that it can now run.
     */
    private ManualExecutor executor;

    /**
     * The task to run, which has methods on it to allow me to manually
     * put it into a passing, failed, or whatnot state.
     */
    private ManualTask task;

    @Override protected AbstractService setupService() {
        service = new AbstractService() {
            @Override protected AbstractTask createTestTask() {
                return task = new ManualTask();
            }
        };
        return service;
    }

    @Override protected Executor createExecutor() {
        return executor = new ManualExecutor(super.createExecutor());
    }

    /**
     * This class will schedule the task, and then you can execute
     * it manually by calling executeScheduled. In this way I can
     * test when a Service is scheduled but not yet started.
     */
    private final class ManualExecutor implements Executor {
        private Runnable scheduled;
        private Executor wrapped;
        
        ManualExecutor(Executor wrapped) {
            this.wrapped = wrapped;
        }

        @Override public void execute(Runnable command) {
            this.scheduled = command;
        }

        public void executeScheduled() {
            wrapped.execute(scheduled);
            // I need to wait until the next "Sentinel" runnable
            // on the queue, which the Task will post when it begins
            // execution.
            handleEvents();
        }
    }

    private final class ManualTask extends AbstractTask {
        private AtomicBoolean finish = new AtomicBoolean(false);
        private AtomicReference<Exception> exception = new AtomicReference<Exception>();

        @Override protected String call() throws Exception {
            runLater(new Sentinel());
            while (!finish.get()) {
                Exception e = exception.get();
                if (e != null) throw e;
            }
            return "Done";
        }

        public void progress(long done, long max) {
            updateProgress(done, max);
        }

        public void message(String msg) {
            updateMessage(msg);
        }

        public void title(String t) {
            updateTitle(t);
        }

        public void fail(Exception e) {
            exception.set(e);
            handleEvents();
        }

        public void complete() {
            finish.set(true);
            handleEvents();
        }
    }

    /************************************************************************
     * Tests while in the ready state                                       *
     ***********************************************************************/

    @Test public void callingStartInReadyStateSchedulesJob() {
        service.start();
        assertSame(service.currentTask, executor.scheduled);
    }

    @Test public void callingStartInReadyMovesToScheduledState() {
        service.start();
        assertSame(Worker.State.SCHEDULED, service.getState());
        assertSame(Worker.State.SCHEDULED, service.stateProperty().get());
    }

    @Test public void callingRestartInReadyStateSchedulesJob() {
        service.restart();
        assertSame(service.currentTask, executor.scheduled);
    }

    @Test public void callingRestartInReadyMovesToScheduledState() {
        service.restart();
        assertSame(Worker.State.SCHEDULED, service.getState());
        assertSame(Worker.State.SCHEDULED, service.stateProperty().get());
    }

    @Test public void callingCancelInReadyStateMovesToCancelledState() {
        service.cancel();
        assertSame(Worker.State.CANCELLED, service.getState());
        assertSame(Worker.State.CANCELLED, service.stateProperty().get());
    }

    @Test public void callingResetInReadyStateHasNoEffect() {
        service.reset();
        assertSame(Worker.State.READY, service.getState());
        assertSame(Worker.State.READY, service.stateProperty().get());
    }

    /************************************************************************
     * Tests while in the scheduled state                                   *
     ***********************************************************************/

    @Test(expected = IllegalStateException.class)
    public void callingStartInScheduledStateIsISE() {
        service.start();
        service.start();
    }

    @Test public void callingCancelInScheduledStateResultsInCancelledState() {
        service.start();
        service.cancel();
        assertSame(Worker.State.CANCELLED, service.getState());
        assertSame(Worker.State.CANCELLED, service.stateProperty().get());
    }

    @Test public void callingRestartInScheduledStateShouldCancelAndReschedule() {
        service.start();
        service.restart();
        assertSame(Worker.State.SCHEDULED, service.getState());
        assertSame(Worker.State.SCHEDULED, service.stateProperty().get());
    }

    @Test(expected = IllegalStateException.class)
    public void callingResetInScheduledStateThrowsISE() {
        service.start();
        service.reset();
    }

    @Test public void stateChangesToRunningWhenExecutorExecutes() {
        service.start();
        executor.executeScheduled();
        assertSame(Worker.State.RUNNING, service.getState());
        assertSame(Worker.State.RUNNING, service.stateProperty().get());
    }

    @Test public void exceptionShouldBeNullInScheduledState() {
        service.start();
        assertNull(service.getException());
        assertNull(service.exceptionProperty().get());
    }

    @Test public void valueShouldBeNullInScheduledState() {
        service.start();
        assertNull(service.getValue());
        assertNull(service.valueProperty().get());
    }

    @Test public void runningShouldBeTrueInScheduledState() {
        service.start();
        assertTrue(service.isRunning());
        assertTrue(service.runningProperty().get());
    }

    @Test public void runningPropertyNotificationInScheduledState() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.runningProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> o,
                                          Boolean oldValue, Boolean newValue) {
                passed.set(newValue);
            }
        });
        service.start();
        assertTrue(passed.get());
    }

    @Test public void workDoneShouldBeNegativeOneInitiallyInScheduledState() {
        service.start();
        assertEquals(-1, service.getWorkDone(), 0);
        assertEquals(-1, service.workDoneProperty().get(), 0);
    }

    @Test public void totalWorkShouldBeNegativeOneAtStartOfScheduledState() {
        service.start();
        assertEquals(-1, service.getTotalWork(), 0);
        assertEquals(-1, service.totalWorkProperty().get(), 0);
    }

    @Test public void progressShouldBeNegativeOneAtStartOfScheduledState() {
        service.start();
        assertEquals(-1, service.getProgress(), 0);
        assertEquals(-1, task.progressProperty().get(), 0);
    }

    @Test public void messageShouldBeEmptyStringWhenEnteringScheduledState() {
        service.start();
        assertEquals("", service.getMessage());
        assertEquals("", task.messageProperty().get());
    }

    @Test public void titleShouldBeEmptyStringAtStartOfScheduledState() {
        service.start();
        assertEquals("", service.getTitle());
        assertEquals("", task.titleProperty().get());
    }

    /************************************************************************
     * Tests while in the running state                                     *
     ***********************************************************************/

    @Test(expected = IllegalStateException.class)
    public void callingStartInRunningStateIsISE() {
        service.start();
        executor.executeScheduled();
        service.start();
    }

    @Test(expected = IllegalStateException.class)
    public void callingResetInRunningStateIsISE() {
        service.start();
        executor.executeScheduled();
        service.reset();
    }

    @Test public void callingRestartInRunningStateCancelsAndReschedules() {
        service.start();
        executor.executeScheduled();
        service.restart();
        assertSame(Worker.State.SCHEDULED, service.getState());
        assertSame(Worker.State.SCHEDULED, service.stateProperty().get());
    }

    @Test public void callingCancelInRunningStateResultsInCancelledState() {
        service.start();
        executor.executeScheduled();
        service.cancel();
        assertSame(Worker.State.CANCELLED, service.getState());
        assertSame(Worker.State.CANCELLED, service.stateProperty().get());
    }

    @Test public void exceptionShouldBeNullInRunningState() {
        service.start();
        executor.executeScheduled();
        assertNull(service.getException());
        assertNull(service.exceptionProperty().get());
    }

    @Test public void valueShouldBeNullInRunningState() {
        service.start();
        executor.executeScheduled();
        assertNull(service.getValue());
        assertNull(service.valueProperty().get());
    }

    @Test public void runningShouldBeTrueInRunningState() {
        service.start();
        executor.executeScheduled();
        assertTrue(service.isRunning());
        assertTrue(service.runningProperty().get());
    }

    @Test public void runningPropertyNotificationInRunningState() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.runningProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> o,
                                Boolean oldValue, Boolean newValue) {
                passed.set(newValue);
            }
        });
        service.start();
        executor.executeScheduled();
        assertTrue(passed.get());
    }

    @Test public void workDoneShouldBeNegativeOneInitiallyInRunningState() {
        service.start();
        executor.executeScheduled();
        assertEquals(-1, service.getWorkDone(), 0);
        assertEquals(-1, service.workDoneProperty().get(), 0);
    }

    @Test public void workDoneShouldAdvanceTo10() {
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        assertEquals(10, service.getWorkDone(), 0);
        assertEquals(10, service.workDoneProperty().get(), 0);
    }

    @Test public void workDonePropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.workDoneProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(ObservableValue<? extends Number> o,
                                Number oldValue, Number newValue) {
                passed.set(newValue.doubleValue() == 10);
            }
        });
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        assertTrue(passed.get());
    }

    @Test public void totalWorkShouldBeNegativeOneAtStartOfRunning() {
        service.start();
        executor.executeScheduled();
        assertEquals(-1, service.getTotalWork(), 0);
        assertEquals(-1, service.totalWorkProperty().get(), 0);
    }

    @Test public void totalWorkShouldBeTwenty() {
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        assertEquals(20, service.getTotalWork(), 0);
        assertEquals(20, service.totalWorkProperty().get(), 0);
    }

    @Test public void totalWorkPropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.totalWorkProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(ObservableValue<? extends Number> o,
                                Number oldValue, Number newValue) {
                passed.set(newValue.doubleValue() == 20);
            }
        });
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        assertTrue(passed.get());
    }

    @Test public void progressShouldBeNegativeOneAtStartOfRunningState() {
        service.start();
        executor.executeScheduled();
        assertEquals(-1, service.getProgress(), 0);
        assertEquals(-1, task.progressProperty().get(), 0);
    }

    @Test public void afterRunningProgressShouldBe_FiftyPercent() {
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        assertEquals(.5, service.getProgress(), 0);
        assertEquals(.5, task.progressProperty().get(), 0);
    }

    @Test public void progressPropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.start();
        task.progressProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(ObservableValue<? extends Number> o,
                                Number oldValue, Number newValue) {
                passed.set(newValue.doubleValue() == .5);
            }
        });
        executor.executeScheduled();
        task.progress(10, 20);
        assertTrue(passed.get());
    }

    @Test public void messageShouldBeEmptyStringWhenEnteringRunningState() {
        service.start();
        executor.executeScheduled();
        assertEquals("", service.getMessage());
        assertEquals("", task.messageProperty().get());
    }

    @Test public void messageShouldBeLastSetValue() {
        service.start();
        executor.executeScheduled();
        task.message("Running");
        assertEquals("Running", service.getMessage());
        assertEquals("Running", task.messageProperty().get());
    }

    @Test public void messagePropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.start();
        task.messageProperty().addListener(new ChangeListener<String>() {
            @Override public void changed(ObservableValue<? extends String> o,
                                String oldValue, String newValue) {
                passed.set("Running".equals(service.getMessage()));
            }
        });
        executor.executeScheduled();
        task.message("Running");
        assertTrue(passed.get());
    }

    @Test public void titleShouldBeEmptyStringAtStartOfRunningState() {
        service.start();
        executor.executeScheduled();
        assertEquals("", service.getTitle());
        assertEquals("", task.titleProperty().get());
    }

    @Test public void titleShouldBeLastSetValue() {
        service.start();
        executor.executeScheduled();
        task.title("Title");
        assertEquals("Title", service.getTitle());
        assertEquals("Title", task.titleProperty().get());
    }

    @Test public void titlePropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.start();
        task.titleProperty().addListener(new ChangeListener<String>() {
            @Override public void changed(ObservableValue<? extends String> o,
                                String oldValue, String newValue) {
                passed.set("Title".equals(service.getTitle()));
            }
        });
        executor.executeScheduled();
        task.title("Title");
        assertTrue(passed.get());
    }

    /************************************************************************
     * Throw an exception in the running state                              *
     ***********************************************************************/

    @Test(expected = IllegalStateException.class)
    public void callingStartInFailedStateIsISE() {
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("anything"));
        service.start();
    }

    @Test public void callingResetInFailedStateResetsStateToREADY() {
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("anything"));
        service.reset();

        assertSame(Worker.State.READY, service.getState());
        assertSame(Worker.State.READY, service.stateProperty().get());
    }

    @Test public void callingResetInFailedStateResetsValueToNull() {
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("anything"));
        service.reset();

        assertNull(service.getValue());
        assertNull(service.valueProperty().get());
    }

    @Test public void callingResetInFailedStateResetsExceptionToNull() {
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("anything"));
        service.reset();

        assertNull(service.getException());
        assertNull(service.exceptionProperty().get());
    }

    @Test public void callingResetInFailedStateResetsWorkDoneToNegativeOne() {
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        task.fail(new Exception("anything"));
        service.reset();

        assertEquals(-1, service.getWorkDone(), 0);
        assertEquals(-1, service.workDoneProperty().get(), 0);
    }

    @Test public void callingResetInFailedStateResetsTotalWorkToNegativeOne() {
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        task.fail(new Exception("anything"));
        service.reset();

        assertEquals(-1, service.getTotalWork(), 0);
        assertEquals(-1, service.totalWorkProperty().get(), 0);
    }

    @Test public void callingResetInFailedStateResetsProgressToNegativeOne() {
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        task.fail(new Exception("anything"));
        service.reset();

        assertEquals(-1, service.getProgress(), 0);
        assertEquals(-1, service.progressProperty().get(), 0);
    }

    @Test public void callingResetInFailedStateResetsRunningToFalse() {
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("anything"));
        service.reset();

        assertFalse(service.isRunning());
        assertFalse(service.runningProperty().get());
    }

    @Test public void callingResetInFailedStateResetsMessageToEmptyString() {
        service.start();
        executor.executeScheduled();
        task.message("Message");
        task.fail(new Exception("anything"));
        service.reset();

        assertEquals("", service.getMessage());
        assertEquals("", service.messageProperty().get());
    }

    @Test public void callingResetInFailedStateResetsTitleToEmptyString() {
        service.start();
        executor.executeScheduled();
        task.title("Title");
        task.fail(new Exception("anything"));
        service.reset();

        assertEquals("", service.getTitle());
        assertEquals("", service.titleProperty().get());
    }

    @Test public void callingRestartInFailedStateReschedules() {
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("anything"));
        service.restart();
        assertSame(Worker.State.SCHEDULED, service.getState());
        assertSame(Worker.State.SCHEDULED, service.stateProperty().get());
    }

    @Test public void callingCancelInFailedStateResultsInNoChange() {
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("anything"));
        service.cancel();
        assertSame(Worker.State.FAILED, service.getState());
        assertSame(Worker.State.FAILED, service.stateProperty().get());
    }

    /************************************************************************
     * Proper Completion of a task                              *
     ***********************************************************************/

    @Test(expected = IllegalStateException.class)
    public void callingStartInSucceededStateIsISE() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.start();
    }

    @Test public void callingResetInSucceededStateResetsStateToREADY() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertSame(Worker.State.READY, service.getState());
        assertSame(Worker.State.READY, service.stateProperty().get());
    }

    @Test public void callingResetInSucceededStateResetsValueToNull() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertNull(service.getValue());
        assertNull(service.valueProperty().get());
    }

    @Test public void callingResetInSucceededStateResetsExceptionToNull() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertNull(service.getException());
        assertNull(service.exceptionProperty().get());
    }

    @Test public void callingResetInSucceededStateResetsWorkDoneToNegativeOne() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertEquals(-1, service.getWorkDone(), 0);
        assertEquals(-1, service.workDoneProperty().get(), 0);
    }

    @Test public void callingResetInSucceededStateResetsTotalWorkToNegativeOne() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertEquals(-1, service.getTotalWork(), 0);
        assertEquals(-1, service.totalWorkProperty().get(), 0);
    }

    @Test public void callingResetInSucceededStateResetsProgressToNegativeOne() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertEquals(-1, service.getProgress(), 0);
        assertEquals(-1, service.progressProperty().get(), 0);
    }

    @Test public void callingResetInSucceededStateResetsRunningToFalse() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertFalse(service.isRunning());
        assertFalse(service.runningProperty().get());
    }

    @Test public void callingResetInSucceededStateResetsMessageToEmptyString() {
        service.start();
        executor.executeScheduled();
        task.message("Message");
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertEquals("", service.getMessage());
        assertEquals("", service.messageProperty().get());
    }

    @Test public void callingResetInSucceededStateResetsTitleToEmptyString() {
        service.start();
        executor.executeScheduled();
        task.title("Title");
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertEquals("", service.getTitle());
        assertEquals("", service.titleProperty().get());
    }

    @Test public void callingRestartInSucceededStateReschedules() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.restart();
        assertSame(Worker.State.SCHEDULED, service.getState());
        assertSame(Worker.State.SCHEDULED, service.stateProperty().get());
    }

    @Test public void callingCancelInSucceededStateResultsInNoChange() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.cancel();
        assertSame(Worker.State.SUCCEEDED, service.getState());
        assertSame(Worker.State.SUCCEEDED, service.stateProperty().get());
    }

    /***************************************************************************
     *
     * Tests for onReady
     *
     **************************************************************************/

    @Test public void onReadyPropertyNameShouldMatchMethodName() {
        assertEquals("onReady", service.onReadyProperty().getName());
    }

    @Test public void onReadyBeanShouldMatchService() {
        assertSame(service, service.onReadyProperty().getBean());
    }

    @Test public void onReadyIsInitializedToNull() {
        assertNull(service.getOnReady());
        assertNull(service.onReadyProperty().get());
    }

//    @Test public void onScheduledFilterCalledBefore_onScheduled() {
//        final AtomicBoolean filterCalled = new AtomicBoolean(false);
//        final AtomicBoolean filterCalledFirst = new AtomicBoolean(false);
//        service.addEventFilter(WorkerStateEvent.WORKER_STATE_SCHEDULED, new EventHandler<WorkerStateEvent>() {
//            @Override public void handle(WorkerStateEvent workerStateEvent) {
//                filterCalled.set(true);
//            }
//        });
//        service.setOnScheduled(new EventHandler<WorkerStateEvent>() {
//            @Override public void handle(WorkerStateEvent workerStateEvent) {
//                filterCalledFirst.set(filterCalled.get());
//            }
//        });
//
//        // Transition to Scheduled state
//        service.start();
//        executor.executeScheduled();
//        // Events should have happened
//        assertTrue(filterCalledFirst.get());
//    }
//
//    @Test public void scheduledCalledAfterHandler() {
//        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
//        service.setOnScheduled(new EventHandler<WorkerStateEvent>() {
//            @Override public void handle(WorkerStateEvent workerStateEvent) {
//                handlerCalled.set(true);
//            }
//        });
//
//        // Transition to Scheduled state
//        service.start();
//        executor.executeScheduled();
//        // Events should have happened
//        assertTrue(handlerCalled.get() && service.currentTask.scheduledSemaphore.getQueueLength() == 0);
//    }
//
//    @Test public void scheduledCalledAfterHandlerEvenIfConsumed() {
//        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
//        service.setOnScheduled(new EventHandler<WorkerStateEvent>() {
//            @Override public void handle(WorkerStateEvent workerStateEvent) {
//                handlerCalled.set(true);
//                workerStateEvent.consume();
//            }
//        });
//
//        // Transition to Scheduled state
//        service.start();
//        executor.executeScheduled();
//        // Events should have happened
//        assertTrue(handlerCalled.get() && service.currentTask.scheduledSemaphore.getQueueLength() == 0);
//    }

    /***************************************************************************
     *
     * Tests for onScheduled
     *
     **************************************************************************/

    @Test public void onScheduledPropertyNameShouldMatchMethodName() {
        assertEquals("onScheduled", service.onScheduledProperty().getName());
    }

    @Test public void onScheduledBeanShouldMatchService() {
        assertSame(service, service.onScheduledProperty().getBean());
    }

    @Test public void onScheduledIsInitializedToNull() {
        assertNull(service.getOnScheduled());
        assertNull(service.onScheduledProperty().get());
    }

    @Test public void onScheduledFilterCalledBefore_onScheduled() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean filterCalledFirst = new AtomicBoolean(false);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_SCHEDULED, new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                filterCalled.set(true);
            }
        });
        service.setOnScheduled(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                filterCalledFirst.set(filterCalled.get());
            }
        });

        // Transition to Scheduled state
        service.start();
        executor.executeScheduled();
        // Events should have happened
        assertTrue(filterCalledFirst.get());
    }

    @Test public void scheduledCalledAfterHandler() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnScheduled(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                handlerCalled.set(true);
            }
        });

        // Transition to Scheduled state
        service.start();
        executor.executeScheduled();
        // Events should have happened
        assertTrue(handlerCalled.get() && service.currentTask.scheduledSemaphore.getQueueLength() == 0);
    }

    @Test public void scheduledCalledAfterHandlerEvenIfConsumed() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnScheduled(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                handlerCalled.set(true);
                workerStateEvent.consume();
            }
        });

        // Transition to Scheduled state
        service.start();
        executor.executeScheduled();
        // Events should have happened
        assertTrue(handlerCalled.get() && service.currentTask.scheduledSemaphore.getQueueLength() == 0);
    }

    /***************************************************************************
     *
     * Tests for onRunning
     *
     **************************************************************************/

    @Test public void onRunningPropertyNameShouldMatchMethodName() {
        assertEquals("onRunning", service.onRunningProperty().getName());
    }

    @Test public void onRunningBeanShouldMatchService() {
        assertSame(service, service.onRunningProperty().getBean());
    }

    @Test public void onRunningIsInitializedToNull() {
        assertNull(service.getOnRunning());
        assertNull(service.onRunningProperty().get());
    }

    @Test public void onRunningFilterCalledBefore_onRunning() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean filterCalledFirst = new AtomicBoolean(false);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_RUNNING, new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                filterCalled.set(true);
            }
        });
        service.setOnRunning(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                filterCalledFirst.set(filterCalled.get());
            }
        });

        // Transition to Running state
        service.start();
        executor.executeScheduled();
        // Events should have happened
        assertTrue(filterCalledFirst.get());
    }

    @Test public void runningCalledAfterHandler() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnRunning(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                handlerCalled.set(true);
            }
        });

        // Transition to Running state
        service.start();
        executor.executeScheduled();
        // Events should have happened
        assertTrue(handlerCalled.get() && service.currentTask.runningSemaphore.getQueueLength() == 0);
    }

    @Test public void runningCalledAfterHandlerEvenIfConsumed() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnRunning(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                handlerCalled.set(true);
                workerStateEvent.consume();
            }
        });

        // Transition to Running state
        service.start();
        executor.executeScheduled();
        // Events should have happened
        assertTrue(handlerCalled.get() && service.currentTask.runningSemaphore.getQueueLength() == 0);
    }

    /***************************************************************************
     *
     * Tests for onSucceeded
     *
     **************************************************************************/

    @Test public void onSucceededPropertyNameShouldMatchMethodName() {
        assertEquals("onSucceeded", service.onSucceededProperty().getName());
    }

    @Test public void onSucceededBeanShouldMatchService() {
        assertSame(service, service.onSucceededProperty().getBean());
    }

    @Test public void onSucceededIsInitializedToNull() {
        assertNull(service.getOnSucceeded());
        assertNull(service.onSucceededProperty().get());
    }

    @Test public void onSucceededFilterCalledBefore_onSucceeded() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean filterCalledFirst = new AtomicBoolean(false);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                filterCalled.set(true);
            }
        });
        service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                filterCalledFirst.set(filterCalled.get());
            }
        });

        // Transition to Succeeded state
        service.start();
        executor.executeScheduled();
        task.complete();
        // Events should have happened
        assertTrue(filterCalledFirst.get());
    }

    @Test public void succeededCalledAfterHandler() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                handlerCalled.set(true);
            }
        });

        // Transition to Succeeded state
        service.start();
        executor.executeScheduled();
        task.complete();
        // Events should have happened
        assertTrue(handlerCalled.get() && service.currentTask.succeededSemaphore.getQueueLength() == 0);
    }

    @Test public void succeededCalledAfterHandlerEvenIfConsumed() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                handlerCalled.set(true);
                workerStateEvent.consume();
            }
        });

        // Transition to Succeeded state
        service.start();
        executor.executeScheduled();
        task.complete();
        // Events should have happened
        assertTrue(handlerCalled.get() && service.currentTask.succeededSemaphore.getQueueLength() == 0);
    }

    /***************************************************************************
     *
     * Tests for onCancelled
     *
     **************************************************************************/

    @Test public void onCancelledPropertyNameShouldMatchMethodName() {
        assertEquals("onCancelled", service.onCancelledProperty().getName());
    }

    @Test public void onCancelledBeanShouldMatchService() {
        assertSame(service, service.onCancelledProperty().getBean());
    }

    @Test public void onCancelledIsInitializedToNull() {
        assertNull(service.getOnCancelled());
        assertNull(service.onCancelledProperty().get());
    }

    @Test public void onCancelledFilterCalledBefore_onCancelled() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean filterCalledFirst = new AtomicBoolean(false);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_CANCELLED, new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                filterCalled.set(true);
            }
        });
        service.setOnCancelled(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                filterCalledFirst.set(filterCalled.get());
            }
        });

        // Transition to Cancelled state
        service.start();
        executor.executeScheduled();
        task.cancel();
        // Events should have happened
        assertTrue(filterCalledFirst.get());
    }

    @Test public void cancelledCalledAfterHandler() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnCancelled(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                handlerCalled.set(true);
            }
        });

        // Transition to Cancelled state
        service.start();
        executor.executeScheduled();
        task.cancel();
        // Events should have happened
        assertTrue(handlerCalled.get() && service.currentTask.cancelledSemaphore.getQueueLength() == 0);
    }

    @Test public void cancelledCalledAfterHandlerEvenIfConsumed() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnCancelled(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                handlerCalled.set(true);
                workerStateEvent.consume();
            }
        });

        // Transition to Cancelled state
        service.start();
        executor.executeScheduled();
        task.cancel();
        // Events should have happened
        assertTrue(handlerCalled.get() && service.currentTask.cancelledSemaphore.getQueueLength() == 0);
    }

    /***************************************************************************
     *
     * Tests for onFailed
     *
     **************************************************************************/

    @Test public void onFailedPropertyNameShouldMatchMethodName() {
        assertEquals("onFailed", service.onFailedProperty().getName());
    }

    @Test public void onFailedBeanShouldMatchService() {
        assertSame(service, service.onFailedProperty().getBean());
    }

    @Test public void onFailedIsInitializedToNull() {
        assertNull(service.getOnFailed());
        assertNull(service.onFailedProperty().get());
    }

    @Test public void onFailedFilterCalledBefore_onFailed() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean filterCalledFirst = new AtomicBoolean(false);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                filterCalled.set(true);
            }
        });
        service.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                filterCalledFirst.set(filterCalled.get());
            }
        });

        // Transition to Succeeded state
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("The End"));
        // Events should have happened
        assertTrue(filterCalledFirst.get());
    }

    @Test public void failedCalledAfterHandler() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                handlerCalled.set(true);
            }
        });

        // Transition to Succeeded state
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("Quit Now"));
        // Events should have happened
        assertTrue(handlerCalled.get() && service.currentTask.failedSemaphore.getQueueLength() == 0);
    }

    @Test public void failedCalledAfterHandlerEvenIfConsumed() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent workerStateEvent) {
                handlerCalled.set(true);
            }
        });

        // Transition to Succeeded state
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("Quit Now"));
        // Events should have happened
        assertTrue(handlerCalled.get() && service.currentTask.failedSemaphore.getQueueLength() == 0);
    }
}
