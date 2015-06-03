//遗传算法计算权重
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Gene 
{
	
	final int GENE_SIZE = 5;//对应training.txt文件中列数
	final int POPULATION = 30;
	final int DATA_SIZE = 5950; //此处固定了训练集，在十叠中，DATA_SIZE需要动态确定,training.txt中总条数
	
	double genes[][] = new double[POPULATION][GENE_SIZE];
	double data[][] = new double[DATA_SIZE][GENE_SIZE];
	
	//double best_weight[] = new double[GENE_SIZE];
	//double[] best_weight = {0.7287673269106896,0.9145847097250356 , 0.8858597357910205 , 0.08251696113850604 , 0.8949482670886394,-1.17828661453704944};
	double[] best_weight = {0.5780469817793109, 0.6637852900038861, 0.08304357505077102, 0.8949482670886394, -1.17828661453704944};
	double best_fitness = 0;
	Random random = new Random();
	
	public static void main(String[] args)
	{
		Gene ga = new Gene();
		ga.GA();
	}
	
	public void GA()
	{
		getTrainingData();
		System.out.println(calcFitness(best_weight, true));
		initGene();
		for(int i = 0; i < 50000; i ++) //计算次数，此处可改
		{
			double[] fitness = calcFitness();
			getBest(fitness);
			double[][] next = reproduceNextGen(fitness);
			mutateNextGen(next);
			getNextGen(next);
		}
		System.out.println("zuijia"+best_fitness);
		calcFitness(best_weight, true);
		for(int i = 0; i < GENE_SIZE; i ++)
			System.out.print(best_weight[i] + " , ");
	}
	
	public void getTrainingData()
	{
		String data_file = "data\\training.txt";
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(data_file)));
			String line;
			int index = 0;
			while((line = reader.readLine()) != null)
			{
				String[] split = line.split(" ");
				for(int i = 0; i < split.length; i ++)
					data[index][i] = Double.parseDouble(split[i]);
				index ++;
				System.out.println(index);
			}
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void initGene()
	{
		for(int i = 0; i < POPULATION; i ++)
		    for(int j = 0; j < GENE_SIZE; j ++)
		        genes[i][j] = random.nextDouble();
	}
	
	public double[] calcFitness()
	{
		double[] fitness = new double[POPULATION];
		for(int i = 0; i < POPULATION; i ++)
			fitness[i] = calcFitness(genes[i], false);
		return fitness;
	}
	
	private double calcFitness(double[] gene, boolean best)
	{
		double[] result = new double[DATA_SIZE];
		for(int i = 0; i < DATA_SIZE; i ++)
		{
			double res = 0;
			//for(int j = 0; j < GENE_SIZE - 1; j ++)
				//res += data[i][j] * gene[j];
			res = (data[i][0]-data[i][0]*data[i][1])*gene[0]+data[i][1]*gene[1]+data[i][2]*gene[2];
			res += gene[GENE_SIZE - 1];
			result[i] = res;
		}
		
		int[] sort = sortData(result);
		int i ;
		for(i = sort.length - 1; i >= 0; i --)
			if(data[sort[i]][GENE_SIZE - 1] > 0)
				break;
		if(best)
		{
			double offset = (result[sort[i]] + result[sort[i + 1]])/2;
		    System.out.println(offset);
		}
		double fitness = calcRank(sort);
		return fitness;
	}
	
	private int[] sortData(double[] result)
	{
		int[] sort = new int[DATA_SIZE];
		int[] selected = new int[DATA_SIZE];
		for(int i = 0; i < DATA_SIZE; i ++)
		{
			double max = -100; int pos = -1;
			for(int j = 0; j < DATA_SIZE; j ++)
				if(selected[j] == 0)
				    if(result[j] > max)
				    {
				    	max = result[j];
				    	pos = j;
				    }
			selected[pos] = 1;
			sort[i] = pos;
		}
		return sort;		
	}
	
	private double calcRank(int[] sort)
	{
		int correct_sum = 0, wrong_sum = 0;
		int correct_count = 0, wrong_count = 0;
		for(int i = 0; i < DATA_SIZE; i ++)
		{
			if(data[sort[i]][GENE_SIZE - 1] > 0)
			{
				correct_sum += i;
				correct_count ++;
			}
			else
			{
				wrong_sum += i;
				wrong_count ++;
			}
		}
		double correct_average = (double)correct_sum/(double)correct_count;
		double wrong_average = (double)wrong_sum/(double)wrong_count;
		double fitness = wrong_average - correct_average;
		if(fitness < 0)
			fitness = 0;
		return fitness;
	}

	public void getBest(double[] fitness)
	{
		double max = 0;
		int pos = -1;
		for(int i = 0; i < POPULATION; i ++)
		    if(fitness[i] > max)
		    {
		    	max = fitness[i];
		    	pos = i;
		    }
		if(max > best_fitness)
		{
			best_fitness = max;
			for(int i = 0; i < GENE_SIZE; i ++)
				best_weight[i] = genes[pos][i];
		}
	}
	
	public double[][] reproduceNextGen(double[] fitness)
	{
		double[][] next_gene = new double[POPULATION][GENE_SIZE];
		double[] accumulate = getAccumulate(fitness);
		for(int i = 0; i < POPULATION; i += 2)
		{
			int parent1 = selectParent(accumulate);
			int parent2 = selectParent(accumulate);
			crossover(next_gene, i, parent1, parent2);
		}
		return next_gene;
	}
	
	private double[] getAccumulate(double[] fitness)
	{
		double sum = getSum(fitness);
		double[] accumulate = new double[POPULATION];
		double acc = 0;
		for(int i = 0; i < POPULATION; i ++)
		{
			acc += fitness[i]/sum;
			accumulate[i] = acc;
		}
		return accumulate;
	}

	private double getSum(double[] fitness)
	{
		double sum = 0;
		for(int i = 0; i < POPULATION; i ++)
			sum += fitness[i];
		return sum;
	}
	
    private int selectParent(double[] acc)
    {
    	double r = random.nextDouble();
    	for(int i = 0; i < POPULATION; i ++)
    	    if(r < acc[i])
    	    	return i;
    	return POPULATION - 1;
    }
    
    private void crossover(double[][] next, int pos, int parent1, int parent2)
    {
    	double[] gene1 = new double[GENE_SIZE];
    	double[] gene2 = new double[GENE_SIZE];
    	copyGene(genes[parent1], gene1); copyGene(genes[parent2], gene2);
    	double prob = random.nextDouble();
    	if(prob < 0.75)
    	    for(int i = 0; i < GENE_SIZE; i ++)
    	    {
    	    	int select = random.nextInt(2);
    	    	if(select == 0)
    	    	{
    	    		next[pos][i] = gene1[i];
    	    		next[pos+1][i] = gene2[i];
    	    	}
    	    	else
    	    	{
    	    		next[pos][i] = gene2[i];
    	    		next[pos+1][i] = gene1[i];
    	    	}
    	    }
    	else
    	{
    		copyGene(gene1, next[pos]);
    		copyGene(gene2, next[pos+1]);
    	}    	
    }
    
    private void copyGene(double[] gene, double[] copy)
    {
    	for(int i = 0; i < GENE_SIZE; i ++)
    		copy[i] = gene[i];
    }

    private void getNextGen(double[][] next)
    {
    	for(int i = 0; i < POPULATION; i ++)
    		copyGene(next[i], genes[i]);
    }
    
    public void mutateNextGen(double[][] next)
    {
    	for(int i = 0; i < POPULATION; i ++)
    	{
    		double mutate_prob = random.nextDouble();
    		if(mutate_prob < 0.3)
    		{
    			for(int j = 0; j < GENE_SIZE; j ++)
    			{
    				int inc_dec_prob = random.nextInt(2);
    				if(inc_dec_prob == 0)
    					next[i][j] += 0.00001;
    				else
    					next[i][j] -= 0.00001;
    			}
    		}
    	}
    }
}
