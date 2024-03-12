import gurobi.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

class Pod {
    private int resourceUsage;
    private int index;
    private int errors;

    public Pod(int resourceUsage, int index, int errors) {
        this.resourceUsage = resourceUsage;
        this.index = index;
        this.errors = errors;
    }

    public int getResourceUsage() {
        return resourceUsage;
    }

    public int getErrors() {
        return errors;
    }

    public int getIndex() {
        return index;
    }
}

class Node implements Comparable<Node> {
    private int capacity;
    private List<Pod> pods;
    private int index;
    private double openingCost;
    private double allocationCost;
    private int errorPenalization;
    private boolean nodeAffinity;

    public Node(int capacity, int index, double openingCost, double allocationCost, int errorPenalization, boolean nodeAffinity) {
        this.capacity = capacity;
        this.pods = new ArrayList<>();
        this.index = index;
        this.openingCost = openingCost;
        this.allocationCost = allocationCost;
        this.errorPenalization = errorPenalization;
        this.nodeAffinity = nodeAffinity;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getOpeningCost() {
        return openingCost;
    }

    public double getAllocationCost() {
        return allocationCost;
    }

    public int getErrorPenalization() {
        return errorPenalization;
    }

    public boolean getNodeAffinity() {
        return nodeAffinity;
    }

    public boolean canAllocatePod(Pod pod) {
        int totalPodsSize = pods.stream().mapToInt(Pod::getResourceUsage).sum();
        return totalPodsSize + pod.getResourceUsage() <= capacity;
    }

    public void allocatePod(Pod pod) {
        pods.add(pod);
    }

    public List<Pod> getPods() {
        return pods;
    }

    public int getIndex() {
        return index;
    }

    public void clear() {
        pods.clear();
    }

    public int compareTo(Node nodeTemp) {
        if (this.index < nodeTemp.index)
            return -1;
        else if (this.index > nodeTemp.index)
            return 1;
        else
            return 0;
    }
}

class Instance {
    private int numPods;
    private int numNodes;

    /* Create the data structures and generate random data. */
    private List<Node> nodes = new ArrayList<>(numNodes);
    private List<Pod> pods = new ArrayList<>(numPods);

    long seed = 100;
    Random random = new Random(seed);

    public Instance(int numPods, int numNodes) {
        this.numPods = numPods;
        this.numNodes = numNodes;
    }

    public int getCapacity() {
        int capacityMin = numPods / numNodes + 1; // Specify the minimum node capacity
        int capacityMax = numPods * 2; // Specify the maximum node capacity

        return random.nextInt(capacityMin, capacityMax + 1);
    }

    public int getResourceUsage() {
        int resourceUsageMin = 1; // Specify the minimum pod size
        int resourceUsageMax = 10; // Specify the maximum pod size

        return random.nextInt(resourceUsageMin, resourceUsageMax + 1);
    }

    public int getOpeningCost() {
        int openingCostInit = 1; // Specify the minimum cost per unit opening node
        int openingCostEnd = 4 * numNodes; // Specify the maximum cost per unit opening node

        return random.nextInt(openingCostInit, openingCostEnd + 1);
    }

    public int getAllocationCost() {
        //TODO generate ao inves de get e privados
        int allocatingCostInit = 1; // Specify the minimum cost per unit allocation cost
        int allocatingCostEnd = 4 * numNodes; // Specify the maximum cost per unit allocation cost

        return random.nextInt(allocatingCostInit, allocatingCostEnd + 1);
    }

    public int getErrors() {
        int errorsInit = 1; // Specify the minimum number of errors per pod
        int errorsEnd = 20; // Specify the maximum number of errors per pod

        return random.nextInt(errorsInit, errorsEnd + 1);
    }

    public int getErrorPenalization() {
        int errorPenalizationInit = 1; // Specify the minimum error penalization per nodes
        int errorPenalizationEnd = 10; // Specify the maximium error penalization per nodes

        return random.nextInt(errorPenalizationInit, errorPenalizationEnd + 1);
    }

    public boolean getNodeAffinity() {
        return random.nextBoolean();
    }

    public void createNodes() {
        /* Create nodes using random data. */
        for (int i = 0; i < numNodes; i++) {
            Node node = new Node(getCapacity(), i, getOpeningCost(), getAllocationCost(), getErrorPenalization(), getNodeAffinity());
            nodes.add(node);

        }
    }

    public void createPods() {
        /* Create pods using random data. */
        for (int j = 0; j < numPods; j++) {
            Pod pod = new Pod(getResourceUsage(), j, getErrors());
            pods.add(pod);
        }
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Pod> getPods() {
        return pods;
    }

    public void modifyNumberPods() {
        int minValue = (int) (numPods * 0.05);
        int maxValue = (int) (numPods * 0.30);

        int modification = random.nextInt(minValue, maxValue);
        boolean randomBoolean = random.nextBoolean();

        if(randomBoolean) {
            for(int i = 0; i < modification; i++) {
                Pod pod = new Pod(getResourceUsage(), numPods + i, getErrors());
                pods.add(pod);
            }

            numPods += modification;
        }
        else {
            for(int i = 0; i < modification; i++) {
                int randomIndex = random.nextInt(0, numPods);
                pods.remove(randomIndex);
                numPods--;
            }
        }
    }
}

class KubeScheduler {
    List<Node> nodes;
    public KubeScheduler(List<Node> nodes) {
        this.nodes = nodes;
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public Node schedulePod(Pod pod) {
        for (Node node : nodes) {
            if (node.canAllocatePod(pod)) {
                node.allocatePod(pod);
                return node;
            }
        }
        return null;
    }

    // public Node schedulePod(Pod pod) {
    //     Node randomNode = getRandomNode();
    //     while (randomNode != null && !randomNode.canAllocatePod(pod)) {
    //         randomNode = getRandomNode();
    //     }

    //     if (randomNode != null) {
    //         randomNode.allocatePod(pod);
    //     }
    //     return randomNode;
    // }
}


public class CustomMain {
    public static void main(String[] args) throws GRBException, IOException {

        int[] tamanhosPods = {50, 100, 200, 500, 1000, 5000, 10000};

        int[] tamanhosNodes = {10, 20, 50, 100, 200};

        int numberExecutions = 10;

        FileWriter writerKubescheduler = new FileWriter(new File("kubescheduler.csv"));
        //FileWriter writerFormulation = new FileWriter(new File("formulation.csv"));
        //FileWriter writerPodsPending = new FileWriter(new File("podspending.csv"));

        //writerKubescheduler.write("number of pods; number of nodes; solution cost; time (ms) \n");
        writerKubescheduler.write("time slot; number of pods; number of nodes; solution cost; time (ms) \n");

        int indexPod =  ThreadLocalRandom.current().nextInt(0, 6);
        int indexNode =  ThreadLocalRandom.current().nextInt(0, 4);

        int numPods = 5000;
        int numNodes = 100;

        System.out.println("Number of pods: " + numPods + " and Number of Nodes: " + numNodes);

        Instance instance = new Instance(numPods, numNodes);

        instance.createPods();

        instance.createNodes();

        /* Perform computational experiments with respect to the time horizon. */

        long startTime = System.currentTimeMillis();

        int totalTime = 20;

        int timeSlot = 0;

        int pendingPods = 0;

        while (timeSlot < totalTime) {

            List<Node> nodes = instance.getNodes();
            List<Pod> pods = instance.getPods();

            HashMap<Pod, Node> allocation = new HashMap<>();
            TreeSet<Node> openedNodes = new TreeSet<>();

            numNodes = nodes.size();
            numPods = pods.size();

            KubeScheduler kubeScheduler = new KubeScheduler(nodes);

            allocation.clear();
            openedNodes.clear();

            for(Node node : nodes)
                node.clear();

            for (Pod pod : pods) {
                Node allocatedNode = kubeScheduler.schedulePod(pod);

                if (allocatedNode != null) {

                    allocation.put(pod, allocatedNode);
                    openedNodes.add(allocatedNode);

                    //double cost = allocatedNode.getAllocationCost();

                    // System.out.println("Allocated pod " + pod.getIndex() + " with resource usage " + pod.getResourceUsage() +
                    //         " to node " + allocatedNode.getIndex() + " with capacity " + allocatedNode.getCapacity() +
                    //         " (Cost: " + cost + ")");
                } //else {
                //System.out.println("Unable to allocate pod " + pod.getIndex() + " with resource usage " + pod.getResourceUsage() + " to any node.");
                //}
            }


            // Schedule pods and measure the time taken
            long endTime = System.currentTimeMillis();

            long elapsedTime = (endTime - startTime) / numberExecutions;

            // Print the pods allocated to each node
            // for (Node node : kubeScheduler.nodes) {
            //     System.out.println("Node " + node.getIndex() + " with capacity " + node.getCapacity() + " has pods with sizes: " + node.getPods());
            // }

            // Calculate quality score
            double totalCost = 0.0;

            /* Sums the opening cost for all opened nodes. */
            Iterator<Node> iterator = openedNodes.iterator();
            while (iterator.hasNext()) {
                Node tempNode = iterator.next();
                totalCost += tempNode.getOpeningCost();
            }

            /* Sums the allocation cost for each allocation performed involving a pod and a node. */
            for (Pod tempPod : pods) {
                Node tempNode = allocation.get(tempPod);

                if (tempNode != null) {
                    totalCost += tempNode.getAllocationCost();
                    totalCost += tempNode.getErrorPenalization() * tempPod.getErrors();
                }
            }

            System.out.println("Total Cost: " + totalCost);
            System.out.println("Total time taken: " + elapsedTime + " ms");

            writerKubescheduler.write(timeSlot + "; " + numPods + "; " + numNodes + "; " + totalCost + "; " + elapsedTime + "\n");

            instance.modifyNumberPods();

            timeSlot++;

            writerKubescheduler.flush();
            //writerFormulation.flush();
            //writerPodsPending.flush();
        }


        writerKubescheduler.close();
        //writerFormulation.close();
        //writerPodsPending.close();
    }
}