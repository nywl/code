import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;



public class LatentConditionPosition {
	public static void main(String arg[]) throws IOException
	{
		LatentConditionPosition t= new LatentConditionPosition();
		t.FindLatentCondition();		
		System.out.print("OK");
	}
	public void FindLatentCondition() throws IOException
	{
		String extracted_pages="data\\extracted_results";
		String latent_path="data\\group.txt";
		String position_result = "data\\position.txt";//��ȱʧ�����ھ�����λ�õ�ͳ�ƽ��
		File file = new File(extracted_pages);
		File[] all = file.listFiles();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(latent_path)));
		String line = br.readLine();
		ArrayList<String> group = new ArrayList<String>();
		while(line!=null)
		{
			group.add(line.toLowerCase());
			line = br.readLine();
		}
		
		String id = new String();//����ͬһ��ȱʧ��������Ӧ�������п��ܶ��alternative statements����group.txt������ͬ��id�ţ�����id����ͬ�ľ��ӣ����ȱʧ�����ڸ����ӵ���������������au֮������������λ�ã�Ȼ��ͳ�Ƹ����λ���������ȡ����������Ϊ���λ��
		String latent_condition=new String();
		String alter_unit = new String();
		String topic_unit = new String();
		for(int i = 0; i < group.size(); i++)
  		{
  			String[] temp = group.get(i).split("\t");
  			latent_condition = temp[2].trim();
  			alter_unit = temp[3].trim();
  			topic_unit = temp[1].replace(alter_unit, "").trim();
  			//�ҵ�alternative statement��Ӧ��extracted pages
  			for(int index = 0; index < all.length; index++)
  			{
  				//System.out.println(all[index].getName());
  				String[] alter_statement=all[index].getName().replace(".txt", "").split("_");
  				if(topic_unit.equalsIgnoreCase(alter_statement[0].trim()) && alter_unit.equalsIgnoreCase(alter_statement[1].trim()))
  				{
  					FindPosition(all[index],position_result,latent_condition,alter_statement[0].trim(),alter_statement[1].trim());
  					break;
  				}  		  		
  			}
  		}
		//��ȡposition.txt�ı��е���Ϣ������ͳ����Ϣȷ��Ǳ��������topic unit�е�λ��
		insertLatent(position_result, group);
	}
	
	private void insertLatent(String path, ArrayList<String> group) throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		String line = br.readLine();
		while(line!=null)
		{
			String[] temp = line.toLowerCase().split(";");
			String position_flag = temp[0].trim();//��ʶȱʧ�������ĳ���ǰ�滹�Ǻ���
			String[] position_flag_temp = position_flag.split("is after|is before|is the same of");
			String latent = position_flag_temp[0].trim();
			String position = temp[1].trim();
			String topic_unit = temp[2].trim();
			ArrayList<String> alter = new ArrayList<String>();//�洢����ͬһ���ͬһǱ�������Ĳ�ͬalter
			alter.add(temp[3].trim());
			if((line=br.readLine())!=null)
			{
				String[] temp1 = line.toLowerCase().split(";");
				String position_flag1 = temp1[0].trim();
				String[] position_flag1_temp = position_flag1.split("is after|is before|is the same of");
				String latent1 = position_flag1_temp[0].trim();
				String topic_unit1 = temp1[2].trim();
				//��ѭ����������ͬһ��topic statement�ľ������ͬһǱ�������������д���ȷ����Ǳ��������λ��
				while(latent.equalsIgnoreCase(latent1) && topic_unit.equalsIgnoreCase(topic_unit1))
				{
					alter.add(temp1[3].trim());
					String position1 = temp1[1].trim();
					if(Math.abs(Integer.parseInt(position))<Math.abs(Integer.parseInt(position1)))
					{
						//position_flag = temp1[0].replaceAll("<b>|</b>", "").trim();
						position = position1;
						topic_unit = topic_unit1;
						position_flag = position_flag1;
					}
					if((line=br.readLine())!=null)
					{
						temp1 = line.toLowerCase().split(";");
						position_flag1_temp = temp1[0].split("is after|is before|is the same of");
						topic_unit1 = temp1[2].trim();
						latent1 = position_flag1_temp[0].trim();
						position_flag1 = temp1[0].trim();
					}
					else
					{
						break;
					}
				}
				expandToFinalStatement(position_flag, topic_unit, alter, group);
			}
			else
			{
				break;
			}
			
		}
	}
	private void expandToFinalStatement(String position_flag, String topic_unit, ArrayList<String> alter, ArrayList<String> group)
	{				
		if(position_flag.contains("is after"))
		{
			String[] temp = position_flag.toLowerCase().split("is after");
			temp[1] = temp[1].replaceAll("<b>|</b>", "");
			String expanded_term = temp[1].trim() + " " + temp[0].trim();
			ArrayList<String> statement = findStatement(group, topic_unit, position_flag);
			for(int i = 0;i < statement.size();i++)
			{
				System.out.println(statement.get(i).replace(temp[1].trim().toLowerCase(), expanded_term));
			}			
		}
		else if(position_flag.contains("is before"))
		{
			String[] temp = position_flag.toLowerCase().split("is before");
			temp[1] = temp[1].replaceAll("<b>|</b>", "");
			String expanded_term = temp[0].trim() + " " + temp[1].trim();
			ArrayList<String> statement = findStatement(group, topic_unit, position_flag);
			for(int i = 0;i < statement.size();i++)
			{
				System.out.println(statement.get(i).replace(temp[1].trim().toLowerCase(), expanded_term));
			}	
		}
		else if(position_flag.contains("is the same of"))
		{
			String[] temp = position_flag.toLowerCase().split("is the same of");
			temp[1] = temp[1].replaceAll("<b>|</b>", "");
			ArrayList<String> statement = findStatement(group, topic_unit, position_flag);
			for(int i = 0;i < statement.size();i++)
			{
				System.out.println(statement.get(i) + " " + temp[0].trim());
			}	
		}

	}
	private ArrayList<String> findStatement(ArrayList<String> group, String topic_unit, String position_flag)
	{
		String latent = null;
		if(position_flag.contains("is after"))
		{
			String[] temp = position_flag.split("is after");
			latent = temp[0].trim();
		}
		else if(position_flag.contains("is before"))
		{
			String[] temp = position_flag.split("is before");
			latent = temp[0].trim();
		}
		else if(position_flag.contains("is the same of"))
		{
			String[] temp = position_flag.split("is the same of");
			latent = temp[0].trim();
		}
		ArrayList<String> statement = new ArrayList<String>();
		for(int i = 0; i < group.size(); i++)
		{
			if(topic_unit.contains("  "))
			{
				String[] topics = topic_unit.split("  ");
				if(group.get(i).contains(topics[0]) && group.get(i).contains(topics[1]) && group.get(i).contains("\t"+latent+"\t"))
				{
					String[] temp = group.get(i).split("\t");
					statement.add(temp[1].trim());

				}
			}
			else
			{
				if(group.get(i).contains(topic_unit) && group.get(i).contains("\t"+latent+"\t"))
				{
					String[] temp = group.get(i).split("\t");
					statement.add(temp[1].trim());

				}
			}
			
		}
		return statement;
	}
	
	private void FindPosition(File path, String position_result, String term, String topic_unit, String alter_unit) throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(position_result,true)));
		String line=br.readLine();
		ArrayList<String> conditions=new ArrayList();
		Nearestterm nearest=new Nearestterm();//�洢�丽��Ǳ���������ִ���������
		ArrayList<Nearestterm> nearest_unit=new ArrayList();//�洢�ı���ÿ����Ǳ�������������topic unit
		
		while(line!=null)
		{
			line=line.replaceAll("&#39;", "\'");
			line=line.replaceAll(",+",",");
			line=line.replaceAll("\\.+",".");
			String[] lines=line.split(",|\\.|:|;");			
			for(int n=0;n<lines.length;n++)
			{
				lines[n]=lines[n].replaceAll("' s|'s|��s|\\(|\\)", " ");
				String[] terms=lines[n].split(" +");
				boolean bold=false;//����ĳЩ���ż����Ӵֵ�����ܽ���һ��<b></b>��ע�����ź�ʶ���Ƿ������������trueΪ�ǵģ����ܽ�ͨ���Ƿ����<b>��</b>���ж��Ƿ�Ӵ�
				for(int i=0;i<terms.length;i++)
				{
					if(term.trim().split(" ").length>1)//Ǳ�����������Ƕ�����ʵ�����Ҫ���������ļ������Ƿ��Ǳ���������
					{
						String[] sub_term = term.trim().split(" ");
						int j = 0;
						for(; j < sub_term.length; j++)
						{
							if(terms[i].trim().equalsIgnoreCase(sub_term[j]))
							{
								i++;
								if(i>=terms.length)
								{
									i = i-(j+1);
									break;
								}
							}
							else
							{
								i = i-j;
								break;
							}
						}
						if(j==sub_term.length)
						{
							conditions.add(term);
						}
							
					}
					if(terms[i].trim().contains("<b>")&&!terms[i].trim().equals("<b>"))
					{
						if(!terms[i].trim().contains("</b>"))
						{
							bold=true;
						}
						conditions.add(terms[i].replaceAll("<b>|</b>", "").trim());				
					}
					else if(terms[i].trim().contains("</b>")&&!terms[i].trim().equals("</b>"))
					{
						if(bold==true)
						{
							bold=false;
						}
						conditions.add(terms[i].replaceAll("<b>|</b>", "").trim());
					}
					else if(terms[i].trim().equals("</b>"))
					{
						bold=false;
					}
					else if(bold==true&&!terms[i].trim().equals("<b>")&&!terms[i].trim().equals("</b>"))
					{
						conditions.add(terms[i].trim());
					}
					else if(isSynonym(terms[i].trim(),term))
					{
						conditions.add(terms[i].trim());
					}
				}
				//�ж�conditions�б��е����ΪǱ�ڷ���ʣ��������������ʴ�list��ȡ����
				for(int i=0;i<conditions.size();i++)
				{				
					if(isSynonym(conditions.get(i),term))
					{
						if(i!=0)
						{
							if(!isSynonym(conditions.get(i-1),term))
							{
								boolean b=false;//Ϊfalse��ʾҪ��ӵ�����list�в�����
								Nearestterm unit=new Nearestterm();
								unit.s=conditions.get(i-1);
								//unit.count=1;
								for(int j=0;j<nearest_unit.size();j++)
								{
									if(nearest_unit.get(j).s.equalsIgnoreCase(unit.s))
									{
										b=true;
										//nearest_unit.get(j).count++;
										nearest_unit.get(j).left_position=nearest_unit.get(j).left_position+1;
										break;
									}
								}
								if(!b)
								{
									nearest_unit.add(unit);
								}							
							}
						}
						if(i+1<conditions.size())
						{						
							if(!isSynonym(conditions.get(i+1),term))
							{
								boolean b=false;//Ϊfalse��ʾҪ��ӵ�����list�в�����
								Nearestterm unit=new Nearestterm();
								unit.s=conditions.get(i+1);
								//unit.count=1;
								for(int j=0;j<nearest_unit.size();j++)
								{
									if(nearest_unit.get(j).s.equalsIgnoreCase(unit.s))
									{
										b=true;
										//nearest_unit.get(j).count++;
										nearest_unit.get(j).right_position=nearest_unit.get(j).right_position+1;
										break;
									}
								}
								if(!b)
								{
									nearest_unit.add(unit);
								}
							}
						}
					}
				}
				
			}			
			line=br.readLine();
		}
		br.close();
		for(int i=0;i<nearest_unit.size();i++)
		{
			if(nearest.s==null||Math.max(nearest.left_position, nearest.right_position)<Math.max(nearest_unit.get(i).left_position, nearest_unit.get(i).right_position))
			{
				nearest.s=nearest_unit.get(i).s;
				//nearest.count=nearest_unit.get(i).count;
				nearest.left_position=nearest_unit.get(i).left_position;
				nearest.right_position=nearest_unit.get(i).right_position;
			}
			
		}
		if(nearest.left_position>nearest.right_position)
		{
			String pos=term+" is after "+nearest.s+";"+nearest.left_position+";"+topic_unit+";"+alter_unit;
			//System.out.println(pos);
			bw.write(pos + "\n");
		}
		else if(nearest.right_position>nearest.left_position)
		{
			String pos=term+" is before "+nearest.s+";"+nearest.right_position+";"+topic_unit+";"+alter_unit;
			//System.out.println(pos);
			bw.write(pos + "\n");
		}
		else
		{
			String pos=term+" is the same of "+nearest.s+";"+nearest.left_position+";"+topic_unit+";"+alter_unit;
			//System.out.println(pos);
			bw.write(pos + "\n");
		}
		bw.flush();
		bw.close();
	}
	
	private boolean isSynonym(String word1, String word2)
	//ȷ�����������Ƿ���ͬһ���ʻ����ǽ����
	{
	 System.setProperty("wordnet.database.dir", "E:\\WordNet\\dict");
	 WordNetDatabase database = WordNetDatabase.getFileInstance();
	 
	 if(word1.equalsIgnoreCase(word2))
			return true;
    	
		Synset[] synsets = database.getSynsets(word1);
		Synset[] synsets2 = database.getSynsets(word2);
		for(int i = 0; i < synsets.length; i ++)
		    for(int j = 0; j < synsets2.length; j ++)
		    	if(synsets[i].equals(synsets2[j]))
		    		return true;
		if((word1.contentEquals("1") ||word1.equalsIgnoreCase("I")) && word2.equalsIgnoreCase("first"))
			return true;
		if(word1.equalsIgnoreCase("first") && (word2.contentEquals("1") ||word2.equalsIgnoreCase("I")))
			return true;
		return false;
	}		
}

class Nearestterm
{
	String s= null;
	//int count=0;
	int left_position=0;//������ʾǱ���������ڸ���ǰ�滹�Ǻ��棬��������Ǳ�������������һ�Σ���һ��ǰ�����һ�����һ���������Ϊ����Ǳ�����������ǰ�棬Ϊ���ź��棬Ϊ0���Ƕ�����
	int right_position=0;
}
