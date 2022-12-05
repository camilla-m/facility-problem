/* Copyright 2022, Gurobi Optimization, LLC */

/* Facility location: a company currently ships its product from 5 Pods
   to 4 Nodes. It is considering closing some Pods to reduce
   costs. What Pod(s) should the company close, in order to minimize
   transportation and fixed costs?

   Based on an example from Frontline Systems:
   http://www.solver.com/disfacility.htm
   Used with permission.
 */

import gurobi.*;

public class Facility {

  public static void main(String[] args) {
    try {

      //PENDENTE: ARRUMAR VALORES DOS PARÂMETROS

      double ListaErros[] = new double[] { 1, 2, 3, 4 };

      // Fixed costs: faz sentido para a gente?
      double CustoFixo[] =
          new double[] { 12000, 15000, 17000, 13000, 16000 };

      double CustoOperacional = 21;

      double Beneficio = 3;

      double Punicao = 1;

      // Pods e Nodes
      int nPods = 3;
      int nNodes = 5;

      // U
      double U[] = new double[] { 20, 30, 40, 50 };
      // u
      double u[] = new double[] { 1, 2, 3, 4 };

      // Modelo
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env);
      model.set(GRB.StringAttr.ModelName, "nodePodsAllocation");

      //restricao 5, para falar que x é binario
      GRBVar[] x = new GRBVar[nNodes];
      for (int i = 0; i < nNodes; ++i) {
        x[i] = model.addVar(0, 1, -CustoOperacional, GRB.BINARY, "x_" + i);
      }

      // Onde um pod é atendido por um node 
      GRBVar[][] y = new GRBVar[nNodes][nPods];

      //restricao 6, para falar que y é binario
      for (int i = 0; i < nNodes; ++i) {
        for (int j = 0; j < nPods; ++j) {
          y[i][j] = model.addVar(0, 1, Beneficio, GRB.BINARY, "y_" + i + "," + j);
        }
      }

      // MAXIMIZAR A FUNÇÃO OBJETIVA
      model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);

      GRBLinExpr sum = new GRBLinExpr();

      for (int i = 0; i < nNodes; ++i) { 
        sum.addTerm(1.0, x[i]);
      }

      model.addConstr(sum, GRB.GREATER_EQUAL, 1, "MinimoNodes"); //restrição 1

      for (int i = 0; i < nNodes; ++i) {
        for (int j = 0; j < nPods; ++j) { 
          model.addConstr(y[i][j], GRB.LESS_EQUAL, x[i], "AlocacaoNoAberto_" + i + "," + j); // restrição 2
        }
      }

      //restrição 3
      GRBLinExpr somatorio_Y;

      for (int j = 0; j < nPods; ++j) {
      
        somatorio_Y = new GRBLinExpr();
      
         for (int i = 0; i < nNodes; ++i) {
          somatorio_Y.addTerm(1.0, y[i][j]);
         }
      
            model.addConstr(somatorio_Y, GRB.EQUAL, 1, "Todo pod tem que ser atendido por um, e somente um, nó.");
      }

      // restrição 4 - entender melhor
      GRBLinExpr somatorio_U;

      GRBLinExpr capacidadeParaNoAberto;

      for (int i = 0; i < nNodes; ++i) {
      
        somatorio_U = new GRBLinExpr();

        capacidadeParaNoAberto = new GRBLinExpr();
      
         for (int j = 0; j < nPods; ++j) {
          somatorio_U.addTerm(u[i], y[i][j]);
         }

         capacidadeParaNoAberto.addTerm(U[i], x[i]);
      
         model.addConstr(somatorio_U, GRB.LESS_EQUAL, capacidadeParaNoAberto, "Os recursos utilizados pelos pods alocados a um nó não podem ultrapassar a capacidade máxima deste.");
      }

      // nó fechado
      for (int j = 0; j < nPods; ++j) { 
        GRBLinExpr ptot = new GRBLinExpr();
        for (int i = 0; i < nNodes; ++i) { //restricao 1
          ptot.addTerm(1.0, y[i][j]);     
          GRBLinExpr limit = new GRBLinExpr();
          limit.addTerm(u[i], x[i]);
          model.addConstr(ptot, GRB.LESS_EQUAL, limit, "u" + i);
        }
      }

    // U restrições
      for (int i = 0; i < nNodes; ++i) {
        GRBLinExpr dtot = new GRBLinExpr();
          for (int j = 0; j < nPods; ++j) {
           dtot.addTerm(1.0, y[i][j]);
        }
      model.addConstr(dtot, GRB.EQUAL, 1, "U" + i);
    }

      // resto do código anterior. faz sentido?
      for (int i = 0; i < nNodes; ++i) {
        x[i].set(GRB.DoubleAttr.Start, 1.0);
      }

      // custo fixo. faz sentido essa parte?
      System.out.println("iniciando:");
      double maxFixo = -GRB.INFINITY;
      for (int i = 0; i < nNodes; ++i) {
        if (CustoFixo[i] > maxFixo) {
          maxFixo = CustoFixo[i];
        }
      }
      for (int i = 0; i < nPods; ++i) {
        if (CustoFixo[i] == maxFixo) {
          x[i].set(GRB.DoubleAttr.Start, 0.0);
          System.out.println("Fechando Node " + i + "\n");
          break;
        }
      }

      // Resolver 'root relaxation'
      model.set(GRB.IntParam.Method, GRB.METHOD_BARRIER);

      // Resolve
      model.optimize();

      // printa solução
      System.out.println("\nTOTAL DE CUSTOS: " + model.get(GRB.DoubleAttr.ObjVal));
      System.out.println("SOLUCAO:");
      for (int i = 0; i < nNodes; ++i) {
        if (x[i].get(GRB.DoubleAttr.X) > 0.99) {
          System.out.println("Nó " + i + " i:");
          for (int j = 0; j < nNodes; ++j) {
            if (y[i][j].get(GRB.DoubleAttr.X) > 0.0001) {
              System.out.println("  deploya " +
                  y[i][i].get(GRB.DoubleAttr.X) +
                  " unidades de pods para Node " + i);
            }
          }
        } else {
          System.out.println("Node " + i + " fechado!");
        }
      }

      // Dispose of model and environment
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    }
  }
}