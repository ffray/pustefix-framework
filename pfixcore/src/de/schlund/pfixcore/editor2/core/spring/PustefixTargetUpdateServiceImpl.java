/*
 * This file is part of PFIXCORE.
 *
 * PFIXCORE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * PFIXCORE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with PFIXCORE; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package de.schlund.pfixcore.editor2.core.spring;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;

import de.schlund.pfixxml.targets.Target;
import de.schlund.pfixxml.targets.TargetGenerationException;

/**
 * Implementation of PageUpdateService using a Thread started upon construction.
 * This service generates all targets registered by using
 * {@link #registerTargetForInitialUpdate(Target)} in a background loop. After
 * the loop has completed the first time, there is a wait time of one second
 * between the generation of each target. Targets registered by using
 * {@link #registerTargetForUpdate(Target)} are generated with a higher priority
 * and without the wait time, but after being generated once, they are
 * automatically removed from the queue.
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class PustefixTargetUpdateServiceImpl implements
        PustefixTargetUpdateService, Runnable {
    private ArrayList lowPriorityQueue;

    private ArrayList highPriorityQueue;

    private HashSet targetList;

    private Object lock;

    private boolean firstRunDone;

    public PustefixTargetUpdateServiceImpl() {
        this.lowPriorityQueue = new ArrayList();
        this.highPriorityQueue = new ArrayList();
        this.lock = new Object();
        this.targetList = new HashSet();
        this.firstRunDone = false;
        Thread thread = new Thread(this);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    public void registerTargetForUpdate(Target target) {
        if (target == null) {
            String msg = "Received null pointer as target!";
            Logger.getLogger(this.getClass()).warn(msg);
            return;
        }
        synchronized (this.lock) {
            this.highPriorityQueue.add(target);
            this.lock.notify();
        }
    }

    public void registerTargetForInitialUpdate(Target target) {
        if (target == null) {
            String msg = "Received null pointer as target!";
            Logger.getLogger(this.getClass()).warn(msg);
            return;
        }
        synchronized (this.lock) {
            if (!this.targetList.contains(target)) {
                this.targetList.add(target);
                this.lowPriorityQueue.add(target);
                this.firstRunDone = false;
                this.lock.notify();
            }
        }
    }

    public void run() {
        while (true) {
            ArrayList lowCopy;
            ArrayList highCopy;
            synchronized (this.lock) {
                lowCopy = (ArrayList) this.lowPriorityQueue.clone();
                highCopy = (ArrayList) this.highPriorityQueue.clone();
                this.highPriorityQueue.clear();
            }
            while (!highCopy.isEmpty()) {
                Target target = (Target) highCopy.get(0);
                try {
                    target.getValue();
                } catch (TargetGenerationException e) {
                    String msg = "Generation of target "
                            + target.getTargetKey() + " failed!";
                    Logger.getLogger(this.getClass()).warn(msg);
                }
                highCopy.remove(0);
                synchronized (this.lock) {

                }
            }
            while (!lowCopy.isEmpty()) {
                Target target = (Target) lowCopy.get(0);
                try {
                    target.getValue();
                } catch (TargetGenerationException e) {
                    String msg = "Generation of target "
                            + target.getTargetKey() + " failed!";
                    Logger.getLogger(this.getClass()).warn(msg);
                }
                lowCopy.remove(0);
                synchronized (this.lock) {
                    this.lowPriorityQueue.remove(0);
                    if (!this.highPriorityQueue.isEmpty()) {
                        break;
                    }
                    if (this.firstRunDone) {
                        try {
                            // Wait a second to make sure
                            // background generation loop
                            // does not consume to much
                            // CPU time
                            this.lock.wait(1000);
                        } catch (InterruptedException e) {
                            // Ignore interruption and continue
                        }
                    }
                }
            }

            synchronized (this.lock) {
                if (this.lowPriorityQueue.isEmpty()) {
                    this.lowPriorityQueue.addAll(this.targetList);
                    this.firstRunDone = true;
                }
                if (this.highPriorityQueue.isEmpty()
                        && this.lowPriorityQueue.isEmpty()) {
                    try {
                        this.lock.wait();
                    } catch (InterruptedException e) {
                        // Ignore exception
                    }
                }
            }
        }
    }

}
