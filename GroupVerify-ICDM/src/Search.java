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
	
	private boolean download_enable = false; //是否下载
	private boolean extract_enable = false;  //是否从源码中提取title及snippet
	private boolean write_result = false;  //是否将处理好的结果写到本地文件中
	
	private String source_path = "data\\source_pages"; //未处理的html源码
	private String result_path = "data\\extracted_results"; //已处理过的搜索结果
	
	
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
	//从文件中读取doubt unit 和topic unit
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
				Pattern pattern = Pattern.compile("\\[(\\s|\\S)+?\\]"); //[]内为doubt unit
				Matcher match = pattern.matcher(line);
				String doubt_unit = null,topic_unit = null;
				if(match.find())
				    doubt_unit = match.group(0);
				else
					System.exit(0);
				String copy = line.substring(0);
				topic_unit = copy.replace(doubt_unit,"");  //剩余部分为topic unit
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
	//处理url，避免中文的不识别问题
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
	        //设置代理服务器   	                                           
	        System.getProperties().put("proxySet", "true");   
	        System.getProperties().put("proxyHost", proxy);   
	        System.getProperties().put("proxyPort", proxyPort);	                                         
	    } 
	
	
	public void Download(String url, String write_file)
	//下载搜索页面的html源码
	{
		try
		{
			
			setProxyServer("127.0.0.1", "8087");
			URL target_page = new URL(url);
			BufferedReader reader = new BufferedReader(new InputStreamReader(target_page.openStream(), "utf-8"));
			Random r = new Random();
			int ran = r.nextInt(10);//0~10间的随机数
			ran = (ran+10)*1000;
			Thread.sleep(ran);//括号里面的数代表毫秒，例如9000代表9000毫秒，也就是9秒
			String line;
			StringBuffer search_result = new StringBuffer();
			line = reader.readLine();
			while(line!=null)
			{
				search_result.append(line.trim() + "\n");
				line = reader.readLine();
			}
			reader.close();
			WriteContent(search_result.toString(), write_file, false); //将下载的源码写入本地文件


		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private String ReadFile(String topic, boolean extract)
	//读取与topic相关的所有文件
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
	//从html源码中提取出title和snippet
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
	//如果extract = false，即直接从已有的处理结果中获得title 和snippet
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
		GetKeyWords();//得到keyword_list，即搜索语句，有DU和AU组成
		String keywords;
	    Iterator<Alternate> it = all_alternate.iterator();
		while(!keyword_list.isEmpty())
		{
			keywords = keyword_list.remove(0);
			Alternate alter = it.next();
			ArrayList<Result> result = new ArrayList<Result>();
			
			if(download_enable)  //下载前num个页面，共获得10*num个结果
			{
			    for(int i = 1; i <= num; i ++) 
			    {
				    String start = Integer.toString((i - 1) * 10 + 1);
			        //search_url = "http://search.yahoo.com/search?p=" + keywords + "&fr=sfp&xargs=0&pstart=1&b=" + start + "&ei=utf-8";
				    search_url = "http://search.yahoo.com/search;_ylt=A0SO8zEBhlZSWhAAkp1XNyoA?p=" + keywords + "&xargs=0&pstart=1&b=" + start + "&xa=Ij53dIyPzIyB6.X4tcCOpg--,1381488643";
			        Download(search_url, source_path + "\\" + alter.topic_unit + "_" + alter.alter_unit + "_" + Integer.toString(i) + ".txt" );
			        result = GetResults(alter.topic_unit.substring(1) + "_" + alter.alter_unit + "_" + Integer.toString(i), extract_enable); //提取出title和snippet
		            alter.srr = result;  
		            
		            if(write_result) //将该alter unit的相关信息写入本地文件
		            	WriteResult(alter);
			    }
			}
			result = GetResults(alter.topic_unit.substring(1) + "_" + alter.alter_unit, extract_enable); //提取出title和snippet
            alter.srr = result;  
            
            if(write_result) //将该alter unit的相关信息写入本地文件
            	WriteResult(alter);
			
		}
		return all_alternate;
	}
	
	private void WriteResult(Alternate alter)
	//将alter unit的相关信息(title和snippet)写入本地
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
	//将制定内容写入file_path
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
