import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Random;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import gurobi.*;

class Pod {
    private int resourceUsage;
    private int index;

    public Pod(int resourceUsage, int index) {
        this.resourceUsage = resourceUsage;
        this.index = index;
    }

    public int getResourceUsage() {
        return resourceUsage;
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

    public Node(int capacity, int index, double openingCost, double allocationCost) {
        this.capacity = capacity;
        this.pods = new ArrayList<>();
        this.index = index;
        this.openingCost = openingCost;
        this.allocationCost = allocationCost;
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

class KubeScheduler {
    List<Node> nodes;

    public KubeScheduler() {
        this.nodes = new ArrayList<>();
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public Node getRandomNode() {
        Random random = new Random();
        return nodes.get(random.nextInt(nodes.size()));
    }

    public Node schedulePod(Pod pod) {
        for (Node node : nodes) {
            if (node.canAllocatePod(pod)) {
                node.allocatePod(pod);
                return node;
            }
        }
        return null; // No node can accommodate the pod
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

        int[] tamanhosPods = {10, 50, 100, 200, 500, 1000, 5000, 10000};

        int[] tamanhosNodes = {5, 10, 20, 50, 100, 200};
      
        int numberExecutions = 5;
      
        FileWriter writerKubescheduler = new FileWriter(new File("kubescheduler.csv"));
        FileWriter writerFormulation = new FileWriter(new File("formulation.csv"));
        
      	writerKubescheduler.write("number of pods; number of nodes; solution cost; time (ms) \n");
        writerFormulation.write("number of pods; number of nodes; solution cost; time (ms) \n");

        for(int numPods : tamanhosPods) {
            for(int numNodes : tamanhosNodes) {
              
            				/* The only valid configuration when we consider 5 nodes is the one with 10 pods. */
                    if (numNodes == 5 && numPods != 10)
                        continue;

                    System.out.println("Total pods: " + numPods + " and Total Nodes: " + numNodes);

                    /* Create the data structures and generate random data. */
                    List<Node> nodes = new ArrayList<>(numNodes);
                    List<Pod> pods = new ArrayList<>(numPods);
                    HashMap<Pod, Node> allocation = new HashMap<>();
                    TreeSet<Node> openedNodes = new TreeSet<>();

                    int capacityMin = numPods / numNodes + 1; // Specify the minimum node capacity
                    int capacityMax = numPods * 2; // Specify the maximum node capacity

                    int resourceUsageMin = 1; // Specify the minimum pod size
                    int resourceUsageMax = 10; // Specify the maximum pod size

                    int openingCostInit = 1; // Specify the minimum cost per unit opening node 
                    int openingCostEnd = 4 * numNodes; // Specify the maximum cost per unit opening node 

                    int allocatingCostInit = 1; // Specify the minimum cost per unit allocation cost 
                    int allocatingCostEnd = 4 * numNodes; // Specify the maximum cost per unit allocation cost 

                    KubeScheduler kubeScheduler = new KubeScheduler();

                    /* Create nodes using random data. */
                    for (int i = 0; i < numNodes; i++) {
                        int capacity = ThreadLocalRandom.current().nextInt(capacityMin, capacityMax + 1);
                        int openingCost = ThreadLocalRandom.current().nextInt(openingCostInit, openingCostEnd + 1);
                        int allocationCost = ThreadLocalRandom.current().nextInt(allocatingCostInit, allocatingCostEnd + 1);
                        
                        Node node = new Node(capacity, i, openingCost, allocationCost);
                        nodes.add(node);    
                        kubeScheduler.addNode(node);
                    }
              
                    /* Create pods using random data. */
                    for (int j = 0; j < numPods; j++) {
                        int resourceUsage = ThreadLocalRandom.current().nextInt(resourceUsageMin, resourceUsageMax + 1);
                        Pod pod = new Pod(resourceUsage, j);
                        pods.add(pod);
                    }

              		/* Perform computational experiments regarding the Kubescheduler algorithm. */

                    long startTime = System.currentTimeMillis();
                    
                    for (int i = 0; i < numberExecutions; i++) {

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

                        if (tempNode != null)
                            totalCost += tempNode.getAllocationCost();
                    }

                    System.out.println("Total Cost: " + totalCost);  
                    System.out.println("Total time taken: " + elapsedTime + " ms");
              
              			writerKubescheduler.write(numPods + "; " + numNodes + "; " + totalCost + "; " + elapsedTime + "\n");
              
              
              			/* ============================================================================================================================================================ */
              
              			/* Perform computational experiments with respect to the Mixed Integer Programming formulation. */
              
                    try {
                      
                    double e[] = new double[numPods];
                      
                    for(int i = 0; i < numPods; i++)
                    		e[i] = 0.0;
                      
                    double gamma[] = new double[numNodes];
                      
                    for(int i = 0; i < numNodes; i++)
                    		gamma[i] = 0.0;
                      
      							/* Builds the alpha array containing nodes' opening costs. */
                    double alpha[] = new double[numNodes];
                      
                    for(int i = 0; i < numNodes; i++)
                    {
                        Node tempNode = nodes.get(i);
                        alpha[i] = tempNode.getOpeningCost();
                    }
                      
                    /* Builds the beta array containing pods' allocation costs. */
                    double beta[] = new double[numNodes];
                      
                    for(int i = 0; i < numNodes; i++)
                    {
                      	Node tempNode = nodes.get(i);
                      	beta[i] = tempNode.getAllocationCost();
                    }

                    /* Builds the capacity array containing nodes' capacities. */
                    double U[] = new double[numNodes];
                      
                    for(int i = 0; i < numNodes; i++)
                    {
                    		Node tempNode = nodes.get(i);
                    		U[i] = tempNode.getCapacity();
                    }

                    /* Builds the usage array containing pods' resource usages. */
                    double u[] = new double[numPods];
                    for(int j = 0; j < numPods; j++)
                    {
                            Pod tempPod = pods.get(j);
                    		u[j] = tempPod.getResourceUsage();
                    }

                    /* Creates the model. */
                    GRBEnv env = new GRBEnv();
                    GRBModel model = new GRBModel(env);
                    model.set(GRB.StringAttr.ModelName, "nodePodsAllocation");

                    //restricao 5, para falar que x é binario
                    GRBVar[] x = new GRBVar[numNodes];
                      
                    for (int i = 0; i < numNodes; ++i)
                    {
                    		x[i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x_" + i);
                    }

                    // Se um pod j é atendido por um node i
                    GRBVar[][] y = new GRBVar[numNodes][numPods];

                    //restricao 6, para falar que y é binario
                    for (int i = 0; i < numNodes; ++i)
                    {
                      	for (int j = 0; j < numPods; ++j)
                      	{
                      			y[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y_" + i + "," + j);
                      	}
                    }

                    // MINIMIZAR A FUNÇÃO OBJETIVO
                    model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

                      
                    // Montar expressão linear da fun obj
                    GRBLinExpr funcaoObjetivo = new GRBLinExpr();

                    for(int i = 0; i < numNodes; i++)
                    {
                       funcaoObjetivo.addTerm(alpha[i], x[i]);
                      
                       for(int j = 0; j < numPods; j++)
                       {
                          funcaoObjetivo.addTerm(beta[i], y[i][j]);
                          funcaoObjetivo.addTerm(gamma[i] * e[j], y[i][j]);
                       }
                    }
                      
                    model.setObjective(funcaoObjetivo, GRB.MINIMIZE);

                    // Criação da restrição 1

                    GRBLinExpr somatorio = new GRBLinExpr();

                    for (int i = 0; i < numNodes; ++i)
                    { 
                    		somatorio.addTerm(1.0, x[i]);
                    }

                    model.addConstr(somatorio, GRB.GREATER_EQUAL, 1, "MinimoNodes");

                    // Criação da restrição 2

                    for (int i = 0; i < numNodes; ++i)
                    {
                    		for (int j = 0; j < numPods; ++j)
                      	{ 
                        		model.addConstr(y[i][j], GRB.LESS_EQUAL, x[i], "AlocacaoNoAberto_" + i + "," + j); 
                      	}
                    }

                    // Criação da restrição 3

                    GRBLinExpr somatorio_Y;

                    for (int j = 0; j < numPods; ++j)
                    {
                    		somatorio_Y = new GRBLinExpr();

                    		for (int i = 0; i < numNodes; ++i)
                    		{
                    				somatorio_Y.addTerm(1.0, y[i][j]);
                       	}

                    		model.addConstr(somatorio_Y, GRB.EQUAL, 1, "AtendimentoPod_" + j);
                    }

                    // Criação da restrição 4

                    GRBLinExpr somatorio_U;

                    GRBLinExpr capacidadeParaNoAberto;

                    for (int i = 0; i < numNodes; ++i)
                    {
                      somatorio_U = new GRBLinExpr();

                      capacidadeParaNoAberto = new GRBLinExpr();

                    		for (int j = 0; j < numPods; ++j)
                       	{
                       		somatorio_U.addTerm(u[j], y[i][j]);
                       	}

                       capacidadeParaNoAberto.addTerm(U[i], x[i]);

                       model.addConstr(somatorio_U, GRB.LESS_EQUAL, capacidadeParaNoAberto, "CapacidadeNo_" + i);
                    }

                    // // resto do código anterior. faz sentido?
                    // for (int i = 0; i < nNodes; ++i) {
                    //   x[i].set(GRB.DoubleAttr.Start, 1.0);
                    // }

                    // // custo fixo. faz sentido essa parte?
                    // System.out.println("iniciando:");
                    // double maxFixo = -GRB.INFINITY;
                    // for (int i = 0; i < nNodes; ++i) {
                    //   if (CustoFixo[i] > maxFixo) {
                    //     maxFixo = CustoFixo[i];
                    //   }
                    // }
                    // for (int i = 0; i < nPods; ++i) {
                    //   if (CustoFixo[i] == maxFixo) {
                    //     x[i].set(GRB.DoubleAttr.Start, 0.0);
                    //     System.out.println("Fechando Node " + i + "\n");
                    //     break;
                    //   }
                    // }

                    // Resolver 'root relaxation'
                    //model.set(GRB.IntParam.Method, GRB.METHOD_BARRIER);

                    startTime = System.currentTimeMillis();
                    
                    for (int i = 0; i < numberExecutions; i++)
                    { 
                    		// Resolve
                    		model.optimize();
                    }

                    // Schedule pods and measure the time taken
                    endTime = System.currentTimeMillis();

                    elapsedTime = (endTime - startTime) / numberExecutions;

                    // Imprime solução

                    System.out.println("Solution Cost: " + model.get(GRB.DoubleAttr.ObjVal));  
                    System.out.println("Total time taken: " + elapsedTime + " ms");
                      
                    writerFormulation.write(numPods + "; " + numNodes + "; " + model.get(GRB.DoubleAttr.ObjVal) + "; " + elapsedTime + "\n");

                    /*System.out.println("SOLUÇÃO:");

                    for (int i = 0; i < numNodes; ++i) {

                      System.out.println("x_" + i + " = " + x[i].get(GRB.DoubleAttr.X));

                      if (x[i].get(GRB.DoubleAttr.X) == 1.0) {
                        System.out.println("Node " + i + " aberto!");
                      } else {
                        System.out.println("Node " + i + " fechado!");
                      }
                    }

                    for (int i = 0; i < numNodes; ++i) {

                      for (int j = 0; j < numPods; ++j) {

                        System.out.println("y_" + i + "," + j + " = " + y[i][j].get(GRB.DoubleAttr.X));

                        if (y[i][j].get(GRB.DoubleAttr.X) == 1.0) {
                          System.out.println("Pod " + j + " alocado ao node " + i + ".");
                        }   
                      }
                    }*/

                    // Dispose no modelo e ambiente

                    model.dispose();
                    env.dispose();

                  } catch (GRBException e) {
                    System.out.println("Error code: " + e.getErrorCode() + ". " +
                        e.getMessage());
            			}
              
              		writerKubescheduler.flush();
                  writerFormulation.flush();
            }
        }
      
      	writerKubescheduler.close();
      	writerFormulation.close();
    }
}
