import java.net.*;
import java.nio.Buffer;
import java.io.*;
import java.util.*;
import java.util.regex.*;

class Search 
{
	private File target_file;
	private String search_url;
	private ArrayList<String> keyword_list;
	public ArrayList<Alternate> all_alternate; 
	private int num = 3;
	private int result_num = 20;
	
	private boolean download_enable = false; //�Ƿ�����
	private boolean extract_enable = false;  //�Ƿ��Դ������ȡtitle��snippet
	private boolean write_result = false;  //�Ƿ񽫴���õĽ��д�������ļ���
	
	private String source_path = "data\\source_pages"; //δ�����htmlԴ��
	private String result_path = "data\\extracted_results"; //�Ѵ�������������
	
	
	public Search(File file_path, boolean d, boolean e, boolean w)
	{
		target_file = file_path;
		keyword_list = new ArrayList<String>();
		all_alternate = new ArrayList<Alternate>();
		download_enable = d;
		extract_enable = e;
		write_result = w;
	}
	
	public void GetKeyWords()
	//���ļ��ж�ȡdoubt unit ��topic unit
	{
		try
		{
			if(!target_file.exists())
			{
				System.out.println("The file does not exist!");
				System.exit(0);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(target_file)));
			String line;
			String[] keywords;
			String concat_keyword = "";
			int rank = 0;
			
			while((line = reader.readLine()) != null)
			{
				Pattern pattern = Pattern.compile("\\[(\\s|\\S)+?\\]"); //[]��Ϊdoubt unit
				Matcher match = pattern.matcher(line);
				String doubt_unit = null,topic_unit = null;
				if(match.find())
				    doubt_unit = match.group(0);
				else
					System.exit(0);
				String copy = line.substring(0);
				topic_unit = copy.replace(doubt_unit,"");  //ʣ�ಿ��Ϊtopic unit
				doubt_unit = doubt_unit.replace("[", "");
				doubt_unit = doubt_unit.replace("]", "");
				Alternate alter = new Alternate(doubt_unit, topic_unit, null);
				line = line.replace("[", "");
				line = line.replace("]", "");
				alter.statement = line;
				all_alternate.add(alter);
				rank ++;
				alter.rank = rank;
								
				keywords = line.trim().split("\\s+?");
				if(keywords.length > 0)	
					concat_keyword = TransformCode(keywords[0]);
				for(int i = 1; i < keywords.length; i ++)
					concat_keyword = concat_keyword + "+" + TransformCode(keywords[i]);
				keyword_list.add(concat_keyword);
			}
			reader.close();
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public String TransformCode(String target)
	//����url���������ĵĲ�ʶ������
	{
		try
		{
		    byte[] temp = target.getBytes("utf-8");
		    String hex = "0123456789ABCDEF";
		    StringBuffer result = new StringBuffer();
		    for(int i = 0; i < temp.length; i ++)
		    {
		    	result.append('%');
		    	result.append(hex.charAt((temp[i]&0xf0)>>4));
		    	result.append(hex.charAt(temp[i]&0x0f));
		    }
		    return result.toString();
		}catch(Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
	
	 public static void setProxyServer(String proxy, String proxyPort) {   
	        //���ô��������   	                                           
	        System.getProperties().put("proxySet", "true");   
	        System.getProperties().put("proxyHost", proxy);   
	        System.getProperties().put("proxyPort", proxyPort);	                                         
	    } 
	
	
	public void Download(String url, String write_file)
	//��������ҳ���htmlԴ��
	{
		try
		{
			
			setProxyServer("127.0.0.1", "8087");
			URL target_page = new URL(url);
			BufferedReader reader = new BufferedReader(new InputStreamReader(target_page.openStream(), "utf-8"));
			Random r = new Random();
			int ran = r.nextInt(10);//0~10��������
			ran = (ran+10)*1000;
			Thread.sleep(ran);//�����������������룬����9000����9000���룬Ҳ����9��
			String line;
			StringBuffer search_result = new StringBuffer();
			line = reader.readLine();
			while(line!=null)
			{
				search_result.append(line.trim() + "\n");
				line = reader.readLine();
			}
			reader.close();
			WriteContent(search_result.toString(), write_file, false); //�����ص�Դ��д�뱾���ļ�


		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private String ReadFile(String topic, boolean extract)
	//��ȡ��topic��ص������ļ�
	{
		String file;
		if(extract)
			file = source_path;
		else
			file = result_path;
		File source_file = new File(file);
		File[] source_pages = source_file.listFiles();
		try
		{
			StringBuffer buffer = new StringBuffer();
		    for(int i = 0; i < source_pages.length; i ++)
		    	if(source_pages[i].getName().contains(topic))
		    	{
		    		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(source_pages[i]), "utf-8"));
		    		String line;
		    		while((line = reader.readLine()) != null)
		    			buffer.append(line + "\n");
		    		reader.close();
		    	}
		    String content = buffer.toString();
		    content = content.replaceAll("&#39;s", "");
		    content = content.replaceAll("&nbsp", " ");
		    return content;
		    		
		}catch(Exception ex)
		{
			ex.printStackTrace();
			return "";
		}
	}
	
	public ArrayList<Result> GetResults(String topic)
	//��htmlԴ������ȡ��title��snippet
	{
		String search_result = ReadFile(topic, true);
		ArrayList<Result> results = new ArrayList<Result>();
		Pattern title_pattern = Pattern.compile("<a id=\"link-\\d([^>]+?)>([\\s|\\S]+?)</a>", Pattern.CASE_INSENSITIVE);
		Matcher title_matcher = title_pattern.matcher(search_result);
		Pattern snippet_pattern = Pattern.compile("class=\"abstr\">([\\s|\\S]+?)</div>", Pattern.CASE_INSENSITIVE);
		Matcher snippet_matcher = snippet_pattern.matcher(search_result);
		int i = 1;
		while(title_matcher.find() && snippet_matcher.find())
		{
			String title, snippet;
			String title_href = title_matcher.group(0);
			if (title_href.contains("amazon"))
			{
				if(title_matcher.find())
				{
					title = title_matcher.group(2);
		            snippet = snippet_matcher.group(1);
		            title = title.replace("&#39;s", "");
		            snippet = snippet.replace("&#39;s", "");
			        Result res = new Result(title, snippet);
			        results.add(res);
				}
			}
			else
			{
				title = title_matcher.group(2);
	            snippet = snippet_matcher.group(1);
	            title = title.replace("&#39;s", "");
	            snippet = snippet.replace("&#39;s", "");
		        Result res = new Result(title, snippet);
		        results.add(res);
			}
			i++;
			if(i==result_num)
			{
				break;
			}
		}
		return results;
	}
	
	private ArrayList<Result> GetResults(String topic, boolean extract)
	//���extract = false����ֱ�Ӵ����еĴ������л��title ��snippet
	{
		if(extract)
			return GetResults(topic);
		String search_result = ReadFile(topic, false);
		ArrayList<Result> result = new ArrayList<Result>();
		String[] results = search_result.replaceAll("&#39;s", "").split("\n");
		for(int i = 0; i < results.length; i += 2)
		{
			Result r = new Result(results[i], results[i+1]);
			result.add(r);	
					
		}
		return result;
	}
	
	public ArrayList<Alternate> SearchKeyWord()
	{
		GetKeyWords();//�õ�keyword_list����������䣬��DU��AU���
		String keywords;
	    Iterator<Alternate> it = all_alternate.iterator();
		while(!keyword_list.isEmpty())
		{
			keywords = keyword_list.remove(0);
			Alternate alter = it.next();
			ArrayList<Result> result = new ArrayList<Result>();
			
			if(download_enable)  //����ǰnum��ҳ�棬�����10*num�����
			{
			    for(int i = 1; i <= num; i ++) 
			    {
				    String start = Integer.toString((i - 1) * 10 + 1);
			        //search_url = "http://search.yahoo.com/search?p=" + keywords + "&fr=sfp&xargs=0&pstart=1&b=" + start + "&ei=utf-8";
				    search_url = "http://search.yahoo.com/search;_ylt=A0SO8zEBhlZSWhAAkp1XNyoA?p=" + keywords + "&xargs=0&pstart=1&b=" + start + "&xa=Ij53dIyPzIyB6.X4tcCOpg--,1381488643";
			        Download(search_url, source_path + "\\" + alter.topic_unit + "_" + alter.alter_unit + "_" + Integer.toString(i) + ".txt" );
			        result = GetResults(alter.topic_unit.substring(1) + "_" + alter.alter_unit + "_" + Integer.toString(i), extract_enable); //��ȡ��title��snippet
		            alter.srr = result;  
		            
		            if(write_result) //����alter unit�������Ϣд�뱾���ļ�
		            	WriteResult(alter);
			    }
			}
			result = GetResults(alter.topic_unit.substring(1) + "_" + alter.alter_unit, extract_enable); //��ȡ��title��snippet
            alter.srr = result;  
            
            if(write_result) //����alter unit�������Ϣд�뱾���ļ�
            	WriteResult(alter);
			
		}
		return all_alternate;
	}
	
	private void WriteResult(Alternate alter)
	//��alter unit�������Ϣ(title��snippet)д�뱾��
	{
		String file_path = result_path + "\\";
		if(alter.topic_unit.startsWith(" "))
			file_path += alter.topic_unit.substring(1) + "_" + alter.alter_unit + ".txt";
		else
			file_path += alter.topic_unit + "_" + alter.alter_unit + ".txt";
		Iterator<Result> itr = alter.srr.iterator();
        while(itr.hasNext())
        {
        	Result res = itr.next();
        	WriteContent(res.title + "\n", file_path, true);
        	WriteContent(res.snippet + "\n", file_path, true);
        }
	}
	
	private void WriteContent(String content, String file_path, boolean append)
	//���ƶ�����д��file_path
	{
		try
		{
			File file = new File(file_path);
			if(!file.exists())
				file.createNewFile();
			DataOutputStream writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file_path, append)));
			writer.write(content.getBytes("utf-8"));
			writer.flush();
			writer.close();
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	
}

class Result
{
	String title;
	String snippet;
	
	public Result(String t, String s)
	{
		title = t;
		snippet = s;
	}
}
