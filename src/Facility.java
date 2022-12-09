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

      double e[] = new double[] { 1, 1, 1, 1 };

      double alpha = 5;

      double beta = 3;

      double gamma = 1;

      // Pods e Nodes
      int nPods = 6;
      int nNodes = 4;

      // U
      double U[] = new double[] { 10, 10, 10, 10 };
      // u
      double u[] = new double[] { 1, 1, 1, 1, 1, 1 };

      // Modelo
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env);
      model.set(GRB.StringAttr.ModelName, "nodePodsAllocation");

      //restricao 5, para falar que x é binario
      GRBVar[] x = new GRBVar[nNodes];
      for (int i = 0; i < nNodes; ++i) {
        x[i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x_" + i);
      }

      // Se um pod j é atendido por um node i
      GRBVar[][] y = new GRBVar[nNodes][nPods];

      //restricao 6, para falar que y é binario
      for (int i = 0; i < nNodes; ++i) {
        for (int j = 0; j < nPods; ++j) {
          y[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y_" + i + "," + j);
        }
      }

      // MAXIMIZAR A FUNÇÃO OBJETIVO
      model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);

      // Montar expressão linear da fun obj

      // Create variables
      GRBVar z = model . addVar (0.0 , 1.0 , 0.0 , GRB.BINARY , "z");

      // Set objective : maximize x + y + 2 z

      GRBLinExpr custoOperacional = new GRBLinExpr();

      GRBLinExpr beneficio = new GRBLinExpr();

      GRBLinExpr punicao = new GRBLinExpr();

      GRBExpr funObj = new GRBExpr() {};

      for (int i = 0; i < nNodes; ++i) { 
        custoOperacional.addTerm(-alpha, x[i]);
        for (int j = 0; j < nPods; ++j) {
          beneficio.addTerm(beta, y[i][j]);
          punicao.addTerm(e[j], y[i][j]);
          model.setObjective(custoOperacional + beneficio - punicao, GRB.MAXIMIZE);
        }
      }


      
      model.setObjective(objetivo, GRB.MAXIMIZE);

      // Criação da restrição 1

      GRBLinExpr somatorio = new GRBLinExpr();

      for (int i = 0; i < nNodes; ++i) { 
        somatorio.addTerm(1.0, x[i]);
      }

      model.addConstr(somatorio, GRB.GREATER_EQUAL, 1, "MinimoNodes");

      // Criação da restrição 2

      for (int i = 0; i < nNodes; ++i) {
        for (int j = 0; j < nPods; ++j) { 
          model.addConstr(y[i][j], GRB.LESS_EQUAL, x[i], "AlocacaoNoAberto_" + i + "," + j); 
        }
      }

      // Criação da restrição 3
      
      GRBLinExpr somatorio_Y;

      for (int j = 0; j < nPods; ++j) {
      
        somatorio_Y = new GRBLinExpr();
      
         for (int i = 0; i < nNodes; ++i) {
          somatorio_Y.addTerm(1.0, y[i][j]);
         }
      
            model.addConstr(somatorio_Y, GRB.EQUAL, 1, "AtendimentoPod_" + j);
      }

      // Criação da restrição 4

      GRBLinExpr somatorio_U;

      GRBLinExpr capacidadeParaNoAberto;

      for (int i = 0; i < nNodes; ++i) {
      
        somatorio_U = new GRBLinExpr();

        capacidadeParaNoAberto = new GRBLinExpr();
      
         for (int j = 0; j < nPods; ++j) {
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

      // Resolve
      model.optimize();

      // Imprime solução

      System.out.println("\nValor da função objetivo: " + model.get(GRB.DoubleAttr.ObjVal));

      System.out.println("SOLUÇÃO:");

      for (int i = 0; i < nNodes; ++i) {
        
        System.out.println("x_" + i + " = " + x[i].get(GRB.DoubleAttr.X));

        if (x[i].get(GRB.DoubleAttr.X) == 1.0) {
          System.out.println("Node " + i + " aberto!");
        } else {
          System.out.println("Node " + i + " fechado!");
        }
      }

      for (int i = 0; i < nNodes; ++i) {

        for (int j = 0; j < nPods; ++j) {

          System.out.println("y_" + i + j + " = " + y[i][j].get(GRB.DoubleAttr.X));

          if (y[i][j].get(GRB.DoubleAttr.X) == 1.0) {
            System.out.println("Pod " + j + " alocado ao node " + i + ".");
          }   
        }
      }

      // Dispose no modelo e ambiente
  
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    }
  }
}