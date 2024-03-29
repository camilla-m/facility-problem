//FORMULATION COM T

import gurobi.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

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
    List<Pod> pendingPods;

    public KubeScheduler() {
        this.nodes = new ArrayList<>();
        this.pendingPods = new ArrayList<>();
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

        //FileWriter writerKubescheduler = new FileWriter(new File("kubescheduler.csv"));
        FileWriter writerFormulation = new FileWriter(new File("formulation.csv"));
        //FileWriter writerPodsPending = new FileWriter(new File("podspending.csv"));

        //writerKubescheduler.write("number of pods; number of nodes; solution cost; time (ms) \n");
        writerFormulation.write("time slot; number of pods; used Nodes; solution cost; time (ms) \n");
        //writerPodsPending.write("number of pods; number of nodes; slot of time; pending pods \n");

        //TODO timeslot
        //TODO investigar qual o pod que ele coloca na lista de pending e pq sempre 1
        //TODO talvez tirar o for
        //sortear tamanho de pod e tamanho de nós
        //entrar num for para cada timestep
        //criar aleatoriamente pods e nós
        //criar 4 metodos - recebe inteiro, x e aumenta quantidade de nos - somando x
        //mexer nos pós - desvio padrao

        int indexPod =  0;
        int indexNode =  0;

        int numPods = 100;
        int numNodes = 50;

        System.out.println("Number of pods: " + numPods + " and Number of Nodes: " + numNodes);

        KubeScheduler kubeScheduler = new KubeScheduler();

        //kubeScheduler.addNode(node);

        Instance instance = new Instance(numPods, numNodes);

        instance.createPods();

        instance.createNodes();

        /* Perform computational experiments with respect to the time horizon. */

        //long startTime = System.currentTimeMillis();

        int totalTime = 20;

        int timeSlot = 0;

        int usedNodes = 0;

        int pendingPods = 0;

        TreeSet<Node> openedNodes = new TreeSet<>();

        while (timeSlot < totalTime) {

            List<Node> nodes = instance.getNodes();
            List<Pod> pods = instance.getPods();

            numNodes = nodes.size();
            numPods = pods.size();

            //allocation.clear();
            //openedNodes.clear();

            //boolean isNodeAvailable = ThreadLocalRandom.current().nextBoolean();



            //for(Node node : nodes)
            //    node.clear();

            //TODO
            /*if(isNodeAvailable) {
                if(!pendingPodsList.isEmpty())
                    pods.addAll(pendingPodsList);
                for (Pod pod : pods) {
                    Node allocatedNode = kubeScheduler.schedulePod(pod);
                    allocation.put(pod, allocatedNode);
                    openedNodes.add(allocatedNode);
                }
            } else {
                for (Pod pod : pods) {
                    pendingPodsList.clear();
                    pendingPodsList.add(pod);
                    pendingPods = pendingPodsList.size();
                }
            }*/

            // Schedule pods and measure the time taken
            //long endTime = System.currentTimeMillis();

            //long elapsedTime = (endTime - startTime) / numberExecutions;

            // Print the pods allocated to each node
            // for (Node node : kubeScheduler.nodes) {
            //     System.out.println("Node " + node.getIndex() + " with capacity " + node.getCapacity() + " has pods with sizes: " + node.getPods());
            // }

            // Calculate quality score
            //double totalCost = 0.0;

            /* Sums the opening cost for all opened nodes. */
            //Iterator<Node> iterator = openedNodes.iterator();

            //while (iterator.hasNext()) {
            //    Node tempNode = iterator.next();
            //    totalCost += tempNode.getOpeningCost();
            //}

            /* Sums the allocation cost for each allocation performed involving a pod and a node. */
            //for (Pod tempPod : pods) {
            //    Node tempNode = allocation.get(tempPod);

            //    if (tempNode != null) {
            //        totalCost += tempNode.getAllocationCost();
            //        totalCost += tempNode.getErrorPenalization() * tempPod.getErrors();
            //    }
            //}

            //System.out.println("Total Cost: " + totalCost);
            //System.out.println("Total time taken: " + elapsedTime + " ms");

            //writerKubescheduler.write(numPods + "; " + numNodes + "; " + totalCost + "; " + elapsedTime + "\n");


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
                model.set(GRB.DoubleParam.MIPGap, 0.03);

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

                long startTime = System.currentTimeMillis();

                for (int i = 0; i < numberExecutions; i++)
                {
                    // Resolve
                    model.optimize();
                }

                // Schedule pods and measure the time taken
                long endTime = System.currentTimeMillis();

                long elapsedTime = (endTime - startTime) / numberExecutions;

                // Imprime solução

                System.out.println("Solution Cost: " + model.get(GRB.DoubleAttr.ObjVal));
                System.out.println("Total time taken: " + elapsedTime + " ms");

                for (int i = 0; i < numNodes; ++i) {
                    if (x[i].get(GRB.DoubleAttr.X) == 1.0) {
                        usedNodes++;
                    }
                }

                writerFormulation.write(timeSlot + "; " + numPods + "; " + usedNodes + "; " + model.get(GRB.DoubleAttr.ObjVal) + "; " + elapsedTime + "\n");

                    /*System.out.println("SOLUÇÃO:");



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

                instance.modifyNumberPods();

                timeSlot++;
                usedNodes = 0;

            } catch (GRBException e) {
                System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
            }

            //writerKubescheduler.flush();
            writerFormulation.flush();
            //writerPodsPending.flush();
        }


        //writerKubescheduler.close();
        writerFormulation.close();
        //writerPodsPending.close();
    }
}