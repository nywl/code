import java.io.*;
import java.math.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import SVM.*;

//运行时会向t-verifier\Data\MAData\Feature\missingconditioncandidates、t-verifier\Data\MAData\Feature\group.txt、data\combinedgroup.txt中
//分别写入各au对应的高频词(MyAtom中selectTopKWords函数中的写语句控制是否每次把结果写入文本，该结果在MissingConditionIdentify中计算项与实体在freebase中距离用的)、初试分组结果、最终选取的分组
public class TestAll 
{
	public static void main(String[] args) throws IOException
	{
		TestAll t = new TestAll();		
		t.Test();
	}
	
	String weight_file; //十叠用于读取权重
	String result_file = "data\\group.txt";
	String training_file = "data\\training.txt";
	String MTS_result_file = "data\\MTS.txt";
	String combinedgroup_file = "data\\combinedgroup.txt";
	
	private String answer_file = "data\\data.txt";
	private String group_file = "data\\data group identification.txt";
	
	ArrayList<ArrayList<String>> all_correct_answer = new ArrayList<ArrayList<String>>();
	ArrayList<ArrayList<String>> all_correct_group = new ArrayList<ArrayList<String>>();
	
	int top_k = 15;//结果中抽取前top_k个高频词
	
	public int[] Test(int i) throws IOException
	{	
		File test_data = new File("data\\alter\\" + i + ".txt");
		MyAtom atom = new MyAtom(test_data,all_correct_group.get(i-1),all_correct_answer.get(i-1));
		int[] count = atom.verifyByGroup(i); //统计该问题的不同答案top5的词中正确分组词的位置
		for(int k = 0; k < top_k; k ++)
			System.out.print(count[k] + " , ");
		return count;
	}
	
	
	
    public void Test() throws IOException
    {
    	/*String test_data = "data\\alter";
    	File file = new File(test_data);
    	File[] all = file.listFiles();
    	getCorrectGroup();
    	getCorrectAnswer();
    	int[] count = new int[top_k]; 
    	for(int index = 1; index <= all.length; index ++)
    	{
    		int[] c = Test(index);
    		for(int i = 0; i < top_k; i ++)
    			count[i] += c[i];
    	}
    	for(int i = 0; i < top_k; i ++)
    		System.out.println(count[i]); //输出所有问题中正确分组词出现的位置*/
    	missingTermResult();
    }
    
    
    private void missingTermResult() throws IOException
    {
    	ArrayList<ArrayList<String>> all_missing_terms = new ArrayList<ArrayList<String>>();
    	ArrayList<String> missing_terms = new ArrayList<String>();
    	BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(group_file)));
    	String line = br.readLine();
    	while(line!=null)
    	{
    		Pattern p = Pattern.compile("\\s*|\t|\r|\n"); 
            Matcher m = p.matcher(line); 
            if(m.replaceAll("").length()>0)
            {
            	if(line.equalsIgnoreCase("New England, Cambridge"))
            	{
            		String[] temp_missing_terms = line.split(", ");
            		missing_terms.add(temp_missing_terms[0].toLowerCase());
            		missing_terms.add(temp_missing_terms[1].toLowerCase());
            	}
            	else
            	{
            		missing_terms.add(line.toLowerCase());
            	}           	
            } 
            else
            {
            	all_missing_terms.add(missing_terms);
            	missing_terms = new ArrayList<String>();
            }

    		line = br.readLine();
    	}
    	all_missing_terms.add(missing_terms);
    	
    	String statement = new String();
    	br = new BufferedReader(new InputStreamReader(new FileInputStream(combinedgroup_file)));
    	int group_num = 1;
    	int TSC_num = 0, TSC_correct_num = 0;
    	int TA_num = 0, TA_correct_num = 0;
    	int temp_num = 0, temp_correct_num = 0;
    	line = br.readLine();
    	boolean next_statement = false;
    	while(line!=null)
    	{  		
    		if(next_statement)
    		{
    			String[] e = line.split(";");
    			if(!statement.equals(e[0]))
    			{
    				if(group_num > 15)
    				{
    					TA_correct_num += temp_correct_num;
    					TA_num += temp_num;
    				}
    				else
    				{
    					TSC_correct_num += temp_correct_num;
    					TSC_num += temp_num;
    				}
    				group_num++;
    			}
				temp_correct_num = 0;
				temp_num = 0;
    			next_statement = false;
    		}
    		
    		Pattern p = Pattern.compile("\\s*|\t|\r|\n"); 
            Matcher m = p.matcher(line); 
            if(m.replaceAll("").length()>0)
            {
            	String[] e = line.split(";");
            	statement = e[0];
            	if(all_missing_terms.get(group_num-1).contains(e[2].toLowerCase()))
            	{
            		temp_correct_num++;
            	}
            	temp_num++;
            }
            else
            {
            	next_statement = true;
            }
    		line = br.readLine();
    	}

    	TA_correct_num += temp_correct_num;
		TA_num += temp_num;
		
		System.out.println("TSC总个数：" + TSC_num + "TSC正确个数：" + TSC_correct_num + "TA总个数：" + TA_num + "TA正确个数：" + TA_correct_num);
		System.out.println("TSC准确率：" + MyMath.div(TSC_correct_num, TSC_num, 2) + ",TSC召回率：" + MyMath.div(TSC_correct_num, 44, 2) + ",TA准确率：" + MyMath.div(TA_correct_num, TA_num, 2) + ",TA召回率：" + MyMath.div(TA_correct_num, 31, 2));
    }
    
    private void getCorrectGroup()
	{
		try
		{
		    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(group_file)));
		    String line;
		    ArrayList<String> group = new ArrayList<String>();
		    while((line = reader.readLine()) != null)
		    {
		    	if(line.contentEquals(""))
		    	{
		    		all_correct_group.add(group);
		    		group = new ArrayList<String>();
		    	}
		    	else
		    	    group.add(line);
		    }
		    all_correct_group.add(group);
		    reader.close();
	
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private void getCorrectAnswer()
	{
		try
		{
		    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(answer_file)));
		    String line;
		    ArrayList<String> group = new ArrayList<String>();
		    while((line = reader.readLine()) != null)
		    {
		    	if(line.contentEquals(""))
		    	{
		    		all_correct_answer.add(group);
		    		group = new ArrayList<String>();
		    	}
		    	else
		    	{
		    		Pattern pattern = Pattern.compile("\\[(\\s|\\S)+?\\]");
		    		Matcher match = pattern.matcher(line);
		    		String answer = null;
		    		if(match.find())
		    		{
		    			answer = match.group(0);
		    			answer = answer.substring(1,answer.length()-1);
		    		}
		    		group.add(answer);
		    	}
		    }
		    reader.close();
		    all_correct_answer.add(group);
		    
		/*    Iterator<ArrayList<String>> it = all_correct_answer.iterator();
		    while(it.hasNext())
		    {
		    	Iterator<String> it_ans = it.next().iterator();
		    	while(it_ans.hasNext())
		    		System.out.print(it_ans.next() + ",");
		    	System.out.println();
		    }
		    */
	
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
/*	
	public void NoExtend() //不分组的情况下得到t-verifier需要的数据，之后用t-verifier的topk方法进行验证，用于对比实验
	{
		try
		{
			getCorrectAnswer();
			String alter_path = "t-verifier\\Data\\MAData\\Feature_v\\alter_units.txt";
			File alter_file = new File(alter_path);
			//File doubt_file = new File("t-verifier\\Data\\MAData\\doubtful_statement_www.txt");
			DataOutputStream writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(alter_file, true)));
			//DataOutputStream doubt_writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(doubt_file, true)));
		for(int i = 1; i <= 50; i ++)
		{
			File test_data = new File("data\\alter\\" + i + ".txt");
			Search search = new Search(test_data, false, false, false);
			ArrayList<Alternate> all_alternate = search.SearchKeyWord();
			
			Iterator<Alternate> it = all_alternate.iterator();
			while(it.hasNext())
			{
				Alternate alter = it.next();
				String content = i + "\t" + alter.statement + "\t" + alter.alter_unit + "\n";
				writer.write(content.getBytes());
			}
			
//			Alternate alter = all_alternate.get(0);
//			String content = i + "\t" + alter.statement;
//			content = content + "\t" + alter.alter_unit + "\t" + "T" + "\t";
//			Iterator<String> it_dif = all_correct_answer.get(i-1).iterator();
//			while(it_dif.hasNext())
//				content = content + it_dif.next() + "&";
//			content = content.substring(0, content.length()-1) + "\ta\n";
//			doubt_writer.write(content.getBytes());

			
		}
		writer.flush();
		writer.close();
		//doubt_writer.close();
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}*/
}
