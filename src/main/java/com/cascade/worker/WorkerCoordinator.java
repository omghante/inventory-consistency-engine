package com.cascade.worker;

/**
 * WorkerCoordinator — Manages auto-scaling decisions based on queue depth.
 */
public class WorkerCoordinator {
    private final WorkerCluster cluster;
    private final int scaleUpThreshold;
    private final int scaleDownThreshold;
    private final int maxWorkers;

    public WorkerCoordinator(WorkerCluster cluster, int scaleUpThreshold, int scaleDownThreshold, int maxWorkers) {
        this.cluster = cluster; this.scaleUpThreshold = scaleUpThreshold;
        this.scaleDownThreshold = scaleDownThreshold; this.maxWorkers = maxWorkers;
    }

    public void evaluate(int currentQueueDepth) {
        if (currentQueueDepth >= scaleUpThreshold && cluster.getTotalCount() < maxWorkers) cluster.scaleUp();
        else if (currentQueueDepth <= scaleDownThreshold && cluster.getTotalCount() > 1) cluster.scaleDown();
    }
}
