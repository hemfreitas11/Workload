package me.bkrmt.bkcore.workload;

import java.util.LinkedList;

public class WorkloadTask implements Runnable {
    private final LinkedList<Workload> workloads = new LinkedList<>();

    public void addWorkload(final Workload workload) {
        this.workloads.add(workload);
    }

    @Override
    public void run() {
        this.workloads.removeIf(Workload::computeThenCheckForScheduling);
    }
}
