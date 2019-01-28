import java.util.*;
import java.io.*;
import ilog.concert.*;
import ilog.cplex.*;


public class Hidato{
  private int size;
  private int[][] indices;
  private ArrayList<ArrayList<Integer>> available;
  
  public static void main (String[] args){
    Hidato hidato = new  Hidato(6); 
    hidato.solve("Hidato.txt");
  }
  
  // constructor
  public Hidato(int size){
    this.size = size;
    this.indices = new int[size][size];
    this.available = new ArrayList<ArrayList<Integer>>();
    
    // the cells are indexed from 0 to 35
    int index = 0;
    for(int i=0; i<size; i++){
      for(int j=0; j<size; j++){
        indices[i][j] = index;
        index++;
      }
    }
    
    // consider a cell: a value is chosen for it
    // create the list of (position) indices of available cells to fill in number (value+1)
    for(int i=0; i<size; i++){
      for(int j=0; j<size; j++){
        ArrayList<Integer> temp = new ArrayList<Integer>();
        if (j>0) temp.add(indices[i][j-1]);
        if (j<5) temp.add(indices[i][j+1]);
        if (i>0){
          temp.add(indices[i-1][j]);
          if (j>0) temp.add(indices[i-1][j-1]);
          if (j<5) temp.add(indices[i-1][j+1]);
        }
        if (i<5){
          temp.add(indices[i+1][j]);
          if (j>0) temp.add(indices[i+1][j-1]);
          if (j<5) temp.add(indices[i+1][j+1]);
        }
        available.add(temp);
      }
    }
  }
  
  // solve by IloCplex  
  public void solve(String filename){
    try {
      IloCplex cplex = new IloCplex();
      int n = this.size*this.size;
      
      // define a matrix of binary variables size 36x36 [value][cell position]
      // for example: if cellVars[0][3] = 1 then value 0 is chosen for position 3
      // meaning number 1 is filled in cell [0][3]
      IloNumVar[][] cellVars = new IloNumVar[n][n];
      for (int i = 0; i < n; i++) {
        cellVars[i] = cplex.numVarArray(n, 0, 1,  IloNumVarType.Int);
      }
      
      // constraint: only one position is chosen for each value
      for(int i=0; i < n; i++){
        IloLinearNumExpr expr1 = cplex.linearNumExpr(); 
        for(int j=0; j<n; j++){
          expr1.addTerm(1, cellVars[i][j]);
        }
        cplex.addEq(expr1, 1);
      }
      
      // constraint: only one value is chosen for each position
      for(int j=0; j<n; j++){
        IloLinearNumExpr expr2 = cplex.linearNumExpr(); 
        for(int i=0; i<n; i++){
          expr2.addTerm(1,cellVars[i][j]);
        }
        cplex.addEq(expr2, 1);
      }
      
      // update constraint for given numbers in the grid
      updateHints(filename, cplex, cellVars);
      
      
      // consider filling in values from 0-34 (excluding the last value)
      for(int i=0; i<n-1; i++){   
        for(int j=0; j<n; j++){
          IloLinearNumExpr expr3 = cplex.linearNumExpr();
          ArrayList<Integer> nextCells = available.get(j);
          for(int cell:nextCells){
            expr3.addTerm(1, cellVars[i+1][cell]);
          }
          
          // if value i is filled in position j, then value (i+1) has to be filled
          // in a position within the list of next available positions of j
          cplex.addLe(cellVars[i][j], expr3);
        }
      }
      
      // solve the program
      cplex.solve();
      
      // output solution
      for (int i = 0; i < this.size; i++) {
        for (int j = 0; j < this.size; j++) {
          for(int value = 0; value < n; value++){
            if(cplex.getValue(cellVars[value][indices[i][j]])==1)        
              System.out.print((value+1)+"\t");
          }
        }
        System.out.println();
      }
      cplex.end(); 
    }
    catch (IloException exc) {
      exc.printStackTrace();
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
  
  // read in the given cells 
  public void updateHints(String filename, IloCplex cplex, IloNumVar[][] cellVars)
    throws java.io.FileNotFoundException, IloException{ 
    File file = new File(filename); 
    Scanner input = new Scanner (file); 
    
    int numberOfHints = input.nextInt();  
    for(int i=0; i<numberOfHints; i++){
      IloLinearNumExpr expr = cplex.linearNumExpr();
      int position = indices[input.nextInt()][input.nextInt()];
      
      expr.addTerm(1,cellVars[(input.nextInt()-1)][position]);
      cplex.addEq(expr,1);  
      // the variable for the given value and given position must be 1
    }
    
    input.close(); 
  } 
  
}

