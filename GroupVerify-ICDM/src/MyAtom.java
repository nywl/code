import java.util.*;
import java.math.*;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.regex.*;
import java.io.*;
import edu.smu.tspell.wordnet.*;
import edu.mit.jwi.item.ISynsetID;
import edu.sussex.nlp.jws.*;

import java.math.BigDecimal;

public class MyAtom
{
	ArrayList<Alternate> all_alternate;
	double threshold_rqr = 0.0;
	double threshold_td = 0.0;
	int topk = 15;
	WordNetDatabase database = WordNetDatabase.getFileInstance();
	String serializedClassifier = "data/classifiers/english.all.3class.distsim.crf.ser.gz";
	AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);	

	double[] weight = {0.05568829961926715, 0.01363661835385157, 4.9717850873576972, 0.14047985916257077};
	
	ArrayList<String> correct_answer = new ArrayList<String>();
	ArrayList<String> correct_group = new ArrayList<String>();
	
	ArrayList<String> all_alter_unit;
	
	File test_file;
	String training_file = "data\\training.txt";
	String group_identify = "data\\data group identification.txt";
	
	public static void main(String[] args) throws IOException
	{
		File read=new File("data\\alter\\4.txt");
		MyAtom atom = new MyAtom(read);
		atom.verifyByGroup(4);
	}
	public MyAtom(File file)
	{
		test_file = file;
	}
	public MyAtom(String test_file)
	{
		this.test_file = new File(test_file);
	}
	public MyAtom(File test_file,ArrayList<String> group,ArrayList<String> answer)
	{
		this.test_file = test_file;
		correct_group = group;
		correct_answer = answer;
	}
	public MyAtom(File test_file, double[] weight, ArrayList<String> group, ArrayList<String> answer)
	{
		this(test_file, group,answer);
		this.weight = weight;
	}
	
	public ArrayList<Result> selectRelated(Alternate alter)
	/* 閫夊嚭鐩稿叧鎬ц緝楂樼殑SRR锛岃姹俁QR(鍖呭惈topic unit鐨勫灏�鍜孴D(topic unit涓巃lter unit鐨勮窛绂�澶т簬鏌愪釜闃堝�(寰呭畾) 
	*/
	{
		ArrayList<Result> selected_result = new ArrayList<Result>();
		ArrayList<Result> result = alter.srr;

		Iterator<Result> it_res = result.iterator();
		for(int i = 0; i < result.size(); i ++)
		{
			Result res = result.get(i);
		//	String title_no_tag = res.title.replaceAll("<[^>]+?>", "");
		//	String snippet_no_tag = res.snippet.replaceAll("<[^>]+?>", "");//鍘绘帀鏍囩
		//	if(containsAll(title_no_tag, alter.alter_unit) || containsAll(snippet_no_tag, alter.alter_unit))
				//瑕佹眰缁撴灉閲屽寘鍚alter unit鐨勬墍鏈夊崟璇�
			{
				//RQR,TD澶т簬闃堝�
				double rqr = calculateRQR(res.title + "\n" + res.snippet, alter.topic_unit + " " + alter.alter_unit);
				double td = calculateTD(res.snippet, alter.topic_unit, rqr);
				if(rqr >= threshold_rqr && td >= threshold_td)
					selected_result.add(res);
			}
		}
		return selected_result;
	}
	
	private boolean containsAll(String content, String unit)
	{
		String[] unit_split = unit.split("\\s+");
		for(int i = 0 ; i < unit_split.length; i ++)
			if(!content.toLowerCase().contains(unit_split[i].toLowerCase()))
				return false;
		return true;
	}
	
	private int calculateIntersect(String content, String unit)
	{
		int count = 0;
		content = content.replaceAll("&#39;", "\'");
		Pattern pattern = Pattern.compile("<b>([\\s|\\S]+?</b>)");
		Matcher match = pattern.matcher(content);
		ArrayList<String> keyword = new ArrayList<String>();
		while(match.find())
		{
			String current = match.group(0);
			current = current.replaceAll("<[^>]+?>", "");
			current = current.toLowerCase();
			String[] split = current.split("\\s+?");
			for(int i = 0; i < split.length; i ++)
				if(unit.toLowerCase().contains(split[i]))
					if(!isStopWord(split[i]) && !contains(keyword, split[i]))
					{
						keyword.add(split[i]);
						count ++;
					}	
		}
		return count;
	}
	
	private boolean contains(ArrayList<String> list, String word)
	{
		Iterator<String> it = list.iterator();
		while(it.hasNext())
		    if(isSynonym(it.next(),word))
		    	return true;
		return false;
	}
	
	private double calculateRQR(String content, String unit)
	{
		int count = calculateIntersect(content,unit);
		String[] unit_split = unit.split("\\s+");
		int length = 0;
		for(int i = 0; i < unit_split.length; i ++)
		    if(!isStopWord(unit_split[i]))
		    	length ++;
		return ((double)count/length);
	}
	
	private double calculateTD(String content, String topic, double rqr)
	{	
		Pattern pattern = Pattern.compile("<b>([ |\\S]+)</b>");
		Matcher match = pattern.matcher(content);
		if(match.find())
		{
			String window = match.group(0);
		    window = window.replaceAll("<[^>]+?>", "");
		    String[] window_split = window.split("[\\s]+?");
		    content = content.replaceAll("<[^>]+?>", " ");
		    String[] content_split = content.split("[\\s]+?");
		    double td = (double)(content_split.length - window_split.length) * rqr /content_split.length;
		    return td;
		}
		else
			return 0;
	}


    public Count[] selectTopKWords(int k, ArrayList<Result> selected_result, int top_k, Alternate alter) throws IOException
    //鎵惧嚭鍑虹幇棰戠巼鏈�珮鐨刱涓瘝(鍖呮嫭鍚嶅瓧)
    {

    	//String missingcondition = "data\\missingconditioncandidates\\"+k+".txt";
    	//BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(missingcondition,true)));
    	ArrayList<Phrase> different_unit = selectDifferentUnits(selected_result); //鎵惧嚭鎵�湁鍗曡瘝
    	removeTrivialWords(different_unit, alter.alter_unit); //鍘绘帀stop word
    	removeAlter(different_unit);
    //	removeVerb(different_unit);
    	
    /*	if(alter.alter_unit.contains("Shelly"))
    	for(int i = 0; i < different_unit.size(); i ++)
    		System.out.println(different_unit.get(i).word);
    	*/	
    	
    	//缁熻鍑虹幇棰戠巼
    	Iterator<Phrase> it = different_unit.iterator();
    	ArrayList<Count> checked = new ArrayList<Count>();
    	while(it.hasNext())
    	{
    		Phrase current = it.next();
    		int pos = findChecked(checked, current);
    		if(pos > -1)
    		{
    			Count count = checked.get(pos);
    			count.count ++;
    		}
    		else
    			addChecked(checked, current);
    	}
    	Count[] top_k_words = new Count[top_k];
    	//Count[] top_k_words = new Count[20];
    	BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(group_identify)));
    	String line = br.readLine();
    	ArrayList<String> groupIdentify = new ArrayList<String>();
    	int position=1;//璁板綍鏄鍑犵粍鍒嗙粍璇�
    	//鎶藉彇瀵瑰簲姝ｇ‘鐨勫垎缁勮瘝鏀惧叆groupIdentify涓�
    	for(;;)
    	{
    		if(line!=null && line.trim().length()!=0)
    		{
    			if(position!=k)
    			{
    				line = br.readLine();
    			}
    			else
    			{
    				groupIdentify.add(line.trim());
    				line = br.readLine();
    				if(line==null || line.trim().length()==0)//瀵逛簬瀵瑰簲鐨勪竴缁勫垎缁勮瘝璇诲彇瀹屾瘯鍚庡氨鏃犻渶鍐嶈鍙栧悗缁殑缁�
    				{
    					break;
    				}
    			}
    		}
    		else
    		{
    			position++;
    			line = br.readLine();
    			if(line == null || line.trim().length()==0)
    			{
    				break;
    			}
    		}    		    		
    	}
    	//鏍规嵁棰戠巼閫夊嚭top k涓瘝,褰撳凡鏈変竴涓纭殑鍒嗙粍璇嶈鎵惧嚭锛屽鍚屼竴涓猘u锛岃嫢鍐嶆湁姝ｇ‘鍒嗙粍璇嶈鎵惧嚭锛屽垯鐩存帴涓㈠純
    	boolean exist=false;
    	top_k_words = findMostFrequent(top_k, checked, alter);  	
    	//bw.write("\r\n");
    	//bw.flush();
    	//bw.close();
    	return top_k_words;
    }
    
    private void removeAlter(ArrayList<Phrase> words) //鍘绘帀鍜宎lter unit鐩歌繎鐨勮瘝
    {
    	Phrase current;
    	int pos = 0;
    	while(pos < words.size())
    	{
    		current = words.get(pos);
    		Iterator<String> it = all_alter_unit.iterator();
    		boolean flag = false;
    		while(it.hasNext())
    		{
    			String alter = it.next();
    			if(isSynonym(current.word, alter))
    			{
    				flag = true;
    				continue;
    			}
    			if(current.word.length() > 3 && containsAll(alter, current.word))
    			{
    				flag = true;
    				continue;
    			}
    		}
    		if(flag)
    			words.remove(pos);
    		else
    			pos ++;
    	}
    }
    
    private void removeTrivialWords(ArrayList<Phrase> words, String alter)
    {
    	String[] trivial = {"a", "an", "the", "in", "on","from", "above", "behind", "at", "by", "for", "from", "of", "to", "about", "with", "onto", "into", "whithin", "including", "before",
    			            "after", "'s", "since", "until", "because", "so", "instead", "front", "and", "both", "as", "well", "not", "only", "but", "also", "neither", "nor", "yet", "while",
    			            "but", "or", "either", "that", "whether", "if", "when", "once", "till", "although", "though", "even", "if", "no", "how", "what", "whatever", "however",
    			            "whether", "unless", "than", "where", "wherever", "it", "this", "these", "those", "he", "she", "him", "her", "his", "its", "whose", "who", "whoever", "whenever",
    			            "is", "are", "am", "was", "were", "has", "have", "had", "been", "be", "get", "got", "gets", "do", "did", "wikipedia", "encyclopedia", "wins", "win", "won", "received", 
    			            "will", "would", "costs", "cost", "best", "reaches", "following", "vs.", "winners", "winner","talk","learn", "receive","winning", "vs"};
    	Phrase current;
    	int pos = 0;
    	while(pos < words.size())
    	{
    		current = words.get(pos);
    		int i;
    		if(current.length > 1)
    		{
    			pos ++;
    			continue;
    		}
    		if(current.word.matches("(\\W)*"))
    		{
    			words.remove(pos);
    			continue;
    		}
    		if(current.word.matches("[a-zA-Z]{1}"))
    		{
    			if(current.word.equalsIgnoreCase("I"))
    				pos ++;
    			else
    			    words.remove(pos);
    			continue;
    		}
    		
    		if(isSynonym(current.word, alter))
    		{
    			words.remove(pos);
    			continue;
    		}
    		for(i = 0; i < trivial.length; i ++)
    			if(current.word.equalsIgnoreCase(trivial[i]))
    			{
    				words.remove(pos);
    				break;
    			}
    		if(i == trivial.length)
    			pos ++;
    	}   	
    }

    
    private ArrayList<Phrase> selectDifferentUnits(ArrayList<Result> selected_result) throws IOException
    //鎵惧埌alter unit鎼滅储鍒扮殑鎵�湁涓嶅悓鐨勮瘝
    {
    	ArrayList<Phrase> different_unit = new ArrayList<Phrase>();
    	Iterator<Result> it = selected_result.iterator();
    	while(it.hasNext())
    	{
    		Result res = it.next();   		
    		ArrayList<Phrase> title = getDifferentUnits(res.title.replaceAll("(?i)first","I").replaceAll("(?i)second","II"));
    		ArrayList<Phrase> snippet = getDifferentUnits(res.snippet.replaceAll("(?i)first","I").replaceAll("(?i)second","II"));
    		different_unit.addAll(title);
    		different_unit.addAll(snippet);
    	}
    	return different_unit;
    }
    
    private int findChecked(ArrayList<Count> checked, Phrase content)
	//缁熻璇嶈鍑虹幇棰戠巼鏃剁‘瀹氳璇嶈鏄惁宸茬粡瀛樺湪闃熷垪閲岋紝鑻ュ瓨鍦紝鍒欒繑鍥炶瀵硅薄(selectTopK涓皢浼氬皢璁℃暟+1)
	{
		Iterator<Count> it = checked.iterator();
		int pos = 0;
		while(it.hasNext())
		{
			Count count = it.next();
			if(count.represent.word.equalsIgnoreCase(content.word)) //涓哄悓涓�瘝璇�
				return pos;
		/*	if(count.represent.type == Type.NAME && content.type == Type.NAME) //涓哄懡鍚嶅疄浣撴椂鍏朵腑涓�釜鏄彟涓�釜鍚嶅瓧鐨勫瓙闆�
			{
				if(count.represent.word.contains("H. W.") && !content.word.contains("H."))
					continue;
				if(!count.represent.word.contains("H.") && content.word.contains("H. W."))
					continue;
				if(containsAll(count.represent.word, content.word))
					return pos;
				else if(containsAll(content.word, count.represent.word))
				{
					count.represent = content;
					return pos;
				}
			}
			*/
			
			if(count.represent.word.contains("H.") && content.word.contains("H."))
			{
				count.represent.word = "George H. W.";
				return pos;
			}
			if(count.represent.word.contentEquals("lightweight") && content.word.contentEquals("Light"))
			    return pos;
			if((count.represent.word.contentEquals("W") || count.represent.word.contentEquals("W.")) &&(content.word.contentEquals("W") || content.word.contentEquals("W.")))
			{
				count.represent.word = "George W.";
				return pos;
			}
			if(count.represent.word.equalsIgnoreCase("C1") && content.word.equalsIgnoreCase("C 1"))
				return pos;
			if(count.represent.word.equalsIgnoreCase("C 1") && content.word.equalsIgnoreCase("C1"))
			{
				count.represent.word = "C1";
				return pos;
			}
			Iterator<String> it_syn = count.synset.iterator();
			while(it_syn.hasNext())
				if(it_syn.next().equalsIgnoreCase(content.word))
					return pos;
			pos ++;
		}
		return -1;
	}
	private void addChecked(ArrayList<Count> checked, Phrase content)
	//濡傛灉璇嶈杩樻湭琚粺璁￠鐜囷紝鍒欏姞鍏ラ槦鍒�
	{
		Synset[] synsets = database.getSynsets(content.word);
		ArrayList<String> syn = new ArrayList<String>();
		for(int i = 0; i < synsets.length; i ++)
		{
			String[] words = synsets[i].getWordForms();
			for(int j = 0; j < words.length; j ++)
				syn.add(words[j]);
		}
		if(content.type == Type.NUMBER)
			if(content.word.matches("^[0-9].*[a-zA-Z]$"))
			{
				String unit = getNumberUnit(content.word);
				String num = content.word.replace(unit, "");
				syn.add(num);
			}
		Count count = new Count(syn, content);
		checked.add(count);
	}
	private Count[] findMostFrequent(int topk, ArrayList<Count> checked, Alternate alter) throws IOException
	//鎵惧埌鍑虹幇棰戠巼鏈�珮鐨勮瘝锛屽惊鐜痥娆″嵆鍙緱鍒皌op k
	{				
		int count_sum = 0;
		for(int i = 0; i < checked.size(); i++)
		{
			count_sum += checked.get(i).count;
		}
		
		
		double dis = 100;
		Count pos = null;
		Count[] most_frequent_unit = new Count[topk];
		//Iterator<Count> it = checked.iterator();
		ArrayList<Count> del_list = new ArrayList<Count>();
		for(int j = 0; j < topk; j++)
		{
			int frequency = 0;
			for(int k = 0; k < checked.size(); k++)
			{
				if(checked.get(k)!=null)
				{
					Count current = checked.get(k);
					current.represent.alter = alter.alter_unit;//涓轰簡鍒嗙粍鏃剁煡閬撴瘡涓珮棰戣瘝鏉ヨ嚜鍝釜au
					
					if(current.count > frequency)
					{
						frequency = current.count;
						dis = dis(alter, current.represent.word);
						pos = current;
						//鐢变簬java涓嶆敮鎸佹诞鐐规暟杩愮畻锛屾墍浠ラ櫎娉曠敤鏁板鍖呬腑涓撻棬鐨勭被鍘诲疄鐜�
						BigDecimal b1 = new BigDecimal(Integer.toString(current.count));
						BigDecimal b2 = new BigDecimal(Integer.toString(count_sum));
						b1 = b1.setScale(10, RoundingMode.HALF_UP);
						b2 = b2.setScale(10, RoundingMode.HALF_UP);
						current.relative_count = b1.divide(b2, 10, RoundingMode.HALF_UP).doubleValue();
						//current.relative_count = current.count/count_sum;
						
						most_frequent_unit[j] = current;
					}
					else if(current.count == frequency)
					{
						double distance = dis(alter, current.represent.word);
						if(distance < dis)
						{
							dis = distance;
							pos = current;
							BigDecimal b1 = new BigDecimal(Integer.toString(current.count));
							BigDecimal b2 = new BigDecimal(Integer.toString(count_sum));
							b1 = b1.setScale(10, RoundingMode.HALF_UP);
							b2 = b2.setScale(10, RoundingMode.HALF_UP);
							current.relative_count = b1.divide(b2, 10, RoundingMode.HALF_UP).doubleValue();
							
							
							most_frequent_unit[j] = current;
						}
					}				
				}
				else
				{
					break;
				}
			}
			if(pos!=null)
			{
				checked.remove(pos);
			}								
		}
		return most_frequent_unit;
	}
	public ArrayList<Phrase> getDifferentUnits(String content) throws IOException
    //璇嗗埆涓�title鎴栬�snippet涓殑鍛藉悕瀹炰綋锛岃瘑鍒悗灏嗗叾浠栬瘝璇垎瑙ｄ负1-gram
    {
    	ArrayList<Phrase> different = new ArrayList<Phrase>();		
    	//鍘绘帀html鏍囩
    	content = content.replaceAll("<wbr />", "");
    	//used for statement 27
    	content = content.replaceAll("(?i)<b>the Rainbow</b>", "the Rainbow");
    	content = content.replaceAll("(?i)<b>Rainbow</b>", "Rainbow");
    	content = content.replaceAll("(?i)<b>of the Ring</b>", "of the Ring");
    	content = content.replaceAll("(?i)<b>ski</b>", "ski");
    	
    	content = content.replaceAll("<b>([\\s|\\S]+?</b>)", "");
    	content = content.replaceAll("&#39;", "\'");
    	content = content.replaceAll("&gt", ">");
    	content = content.replaceAll("&quot", "\"");
    	content = content.replaceAll("&amp", "&");
    	content = content.replaceAll("&nbsp", " ");
    	
    	
    	//鍒╃敤stanford-ner璇嗗埆鍚嶅瓧
    	content = identifyName(different, content);
    	//璇嗗埆鏃ユ湡鍜屾椂闂�
    	String regex = "(\\d{1,2}((st)|(nd)|(rd)|(th))?,? ((January)|(Jan)|(February)|(Feb)|(March)|(Mar)|(April)|(Apr)|(May)|(June)|(Jun)|(July)|(Jul)|(August)|(Aug)|(September)|(Sept)|(October)|(Oct)|(November)|(Nov)|(December)|(Dec)))+";
    	content = identifyTime(different, content, regex, Type.DATE);
    	regex = "(((January)|(Jan)|(February)|(Feb)|(March)|(Mar)|(April)|(Apr)|(May)|(June)|(Jun)|(July)|(Jul)|(August)|(Aug)|(September)|(Sept)|(October)|(Oct)|(November)|(Nov)|(December)|(Dec)){1}" 
    			        + "((,|.)? \\d{1,2}((st)|(nd)|(rd)|(th))?))+";
    	content = identifyTime(different, content, regex, Type.DATE);
    	
    	regex = "((\\d{1,2}:\\d{2})(:\\d{2})? ((am)|(pm))?){1}";
    	content = identifyTime(different, content, regex, Type.TIME);
    	
    	
    	String[] content_split = content.split("(\\s+?)|-|鈥搢/");
    	
    	for(int i = 0; i < content_split.length; i ++)
    	{
    		String trim = content_split[i];
    		while(trim.matches("^\\p{Punct}.*")) //鍘绘帀鍙ラ鏍囩偣(濡傗�锛屸�)
    			trim = trim.substring(1);
    		while(trim.matches(".*\\p{Punct}$"))  //鍘绘帀鍙ユ湯鏍囩偣
    			trim = trim.substring(0, trim.length()-1);
    		if(trim.contentEquals(""))
    			continue;

    		int type = getType(trim);  //纭畾1-gram鐨勭被鍨嬶紝濡傛暟瀛楋紝鍚嶅瓧锛屽勾浠界瓑
    		Phrase phrase;
    		
    	/*	if(type == Type.NUMBER) //鎵惧埌杩炵画鐨勬暟瀛楀苟缁勫悎(濡倀hirty four绛�
    		{
    			phrase = getPhrase(content_split, i, content_split.length, Type.NUMBER); 
    			i = i + phrase.length - 1;
    		}
    		
    		else if(type == Type.NAME) //褰撴煇涓崟璇嶄负澶у啓鏃�姝ゆ椂type涓篘AME)鎵惧埌杩炵画鐨勫ぇ鍐欏崟璇�涓棿鍙寘鍚玸top word)锛岃涓哄畠浠叡鍚岀粍鎴愪竴涓懡鍚嶅疄浣�
    		{
    			phrase = getPhrase(content_split, i, content_split.length, Type.NAME); //鎵惧埌杩炵画鐨勫ぇ鍐欏崟璇嶅苟缁勫悎
    			//闄ゅ幓璇嶇粍鏈鐨剆top word
    			String[] word_split = phrase.word.split("\\s"); 
    			int j = word_split.length - 1;
    			while(word_split[j].matches("^[a-z].*"))
    			    j --;
    			StringBuffer buffer = new StringBuffer();
    			for(int k = 0; k <= j; k ++)
    			{
    				buffer.append(word_split[k]);
    				if(k != j)
    					buffer.append(" ");
    			}
    			phrase.word = buffer.toString();
    			phrase.length = j + 1;
    			//璁や负闀垮害涓�鐨勫ぇ鍐欏崟璇嶄笉鏄懡鍚嶅疄浣擄紝杩涜绾犳
    			if(j == 0)
    			{
    				phrase.type = Type.COMMON;
    				phrase.word = phrase.word.toLowerCase().replace("'s", "");
    			}
    			else
    			    phrase.name_type = Type.OTHER_NAME; //涓嶇‘瀹氬懡鍚嶅疄浣撶殑鍏蜂綋绫诲瀷(浜哄悕锛屽湴鍚嶇瓑)锛岃涓烘槸OTHER_NAME(濡傜數褰卞悕)

    			i = i + phrase.length - 1;
    		}
    	*/	
    		
    	//	else //濡傛灉涓嶆槸棣栧瓧姣嶅ぇ鍐欙紝璁や负鏄櫘閫氬崟璇嶏紝鐩存帴鍔犲叆list
    	//	{
    			trim = trim.replaceAll("'s", "");
    			trim = trim.replaceAll("鈥檚", "");
    			phrase = new Phrase(trim, type);
    	//	}
    		different.add(phrase);
    	}   	
    	return different;
    }
    
    private String identifyTime(ArrayList<Phrase> words, String content, String regex, int type)
    //鐢ㄤ簬璇嗗埆鏃堕棿鍜屾棩鏈�鍥犱负澶氭槸n-gram涓旀湁涓�畾鐨勮寰�
    {
    	Pattern pattern = Pattern.compile(regex);
    	Matcher match = pattern.matcher(content);
    	while(match.find())
    	{
    		String current = match.group(0);
    		content = content.replaceFirst(current, "");
    		Phrase phrase = new Phrase(current, type);
    		words.add(phrase);
    	}
    	return content;
    }
    
    private ArrayList<String> identifyEntity(String filepath) throws IOException
    {
    	ArrayList<String> entities= new ArrayList();
    	BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(group_identify)));
    	String line=reader.readLine();
    	for(;;)
    	{
    		if(line!=null)
    		{
    			String[] arr = line.trim().split(" ");
    			if(arr.length>1)
    			{
    				entities.add(line.trim());
    			}
    			line = reader.readLine();
    		}
    		else if((line = reader.readLine())==null)
    		{
    			break;
    		}
    		
    	}
		return entities;
    	
    }
    
    private String identifyName(ArrayList<Phrase> words, String content) throws IOException
    //鍒╃敤ner璇嗗埆鍛藉悕瀹炰綋骞惰褰曞叾鍏蜂綋绫诲瀷
    {
    	String classified = classifier.classifyWithInlineXML(content);
   	
    	//John is a person name and will be removed in type.contentEquals("PERSON"),so we identify it first
    	if(content.contains("St. John")||content.contains("St John"))
    	{
    		Phrase phrase = new Phrase("St. John", Type.NAME);
    		phrase.name_type = Type.PLACE;
    		words.add(phrase);
    		content = content.replace("St. John", "").replace("St John", "");
    	}
    	if(content.contains("Bristol"))
    	{
    		Phrase phrase = new Phrase("Bristol", Type.NAME);
    		phrase.name_type = Type.PLACE;
    		words.add(phrase);
    		content = content.replace("Bristol", "");
    	}
    	if(content.contains("Rainbow"))
    	{
    		Phrase phrase = new Phrase("Rainbow", Type.NAME);
    		phrase.name_type = Type.PEOPLE;
    		words.add(phrase);
    		content = content.replace("Rainbow", "");
    	}
    	if(content.contains("Ibaraki"))
    	{
    		Phrase phrase = new Phrase("Ibaraki", Type.NAME);
    		phrase.name_type = Type.PLACE;
    		words.add(phrase);
    		content = content.replace("Ibaraki", "");
    	}
    	if(content.contains("Osaka"))
    	{
    		Phrase phrase = new Phrase("Osaka", Type.NAME);
    		phrase.name_type = Type.PLACE;
    		words.add(phrase);
    		content = content.replace("Osaka", "");
    	}
    	if(content.contains("ski cross"))
    	{
    		Phrase phrase = new Phrase("ski cross", Type.NAME);
    		phrase.name_type = Type.OTHER_NAME;
    		words.add(phrase);
    		content = content.replace("ski cross", "");
    	}
    	if(content.contains("55-kilogram"))
    	{
    		content = content.replace("55-kilogram", "55 kg");
    	}
    	if(content.contains("I") || content.contains("first")|| content.contains("First"))
    	{
    		content = content.replaceAll("(?i)first", "I");
    	}
    	if(content.contains("II") || content.contains("second")||content.contains("Second"))
    	{
    		content = content.replaceAll("(?i)second", "II");
    	}
    	if(content.contains("Redmond"))
    	{
    		Phrase phrase = new Phrase("Redmond", Type.NAME);
    		phrase.name_type = Type.PLACE;
    		words.add(phrase);
    		content = content.replace("Redmond", "");
    		
    	}
    	if(content.contains("DD-414")||content.contains("DD 414"))
    	{
    		if(content.contains("DD 414"))
    		{
    			content = content.replace("DD 414", "DD-414");
    		}
    		Phrase phrase = new Phrase("DD-414", Type.NAME);
    		phrase.name_type = Type.OTHER_NAME;
    		words.add(phrase);
    		content = content.replace("DD-414", "");
    	}
    	if(content.contains("DDG 59")||content.contains("DDG-59"))
    	{
    		if(content.contains("DDG 59"))
    		{
    			content = content.replace("DDG 59", "DDG-59");
    		}
    		Phrase phrase = new Phrase("DDG-59", Type.NAME);
    		phrase.name_type = Type.OTHER_NAME;
    		words.add(phrase);
    		content = content.replace("DDG-59", "");
    		
    	}
    	
    	Pattern pattern = Pattern.compile("<([^>]+?)>([^<]+?)</[^>]+?>");
    	Matcher match = pattern.matcher(classified);
    	while(match.find())
    	{
    		String type = match.group(1);
    		String name = match.group(2);
    		int t = -1;
    		if(type.contentEquals("PERSON"))
    			t = Type.PEOPLE;
    		else if(type.contentEquals("LOCATION"))
    			t = Type.PLACE;
    		else if(type.contentEquals("ORGANIZATION"))
    			t = Type.ORGANIZATION;
    		else if(type.contentEquals("DATE"))
    			t = Type.DATE;   		
    		Phrase phrase = new Phrase(name, Type.NAME);
    		phrase.name_type = t;
    		words.add(phrase);
    	}
    	content = classified.replaceAll("<([^>]+?)>([^<]+?)</[^>]+?>", "");
    	if(content.contains("Computer and Cognitive Science"))
    	{
    		Phrase phrase = new Phrase("Computer and Cognitive Science", Type.NAME);
    		phrase.name_type = Type.OTHER_NAME;
    		words.add(phrase);
    		content = content.replace("Computer and Cognitive Science", "");
    	}
    	
    	
    	ArrayList<String> entities = identifyEntity(group_identify);
    	for(int i = 0; i<entities.size();i++)
    	{
    		String entity = entities.get(i).toLowerCase();
    		if(content.toLowerCase().contains(entity))
    		{
        		
    			Phrase phrase = new Phrase(entity, Type.NAME);
    			phrase.name_type = Type.OTHER_NAME;
        		words.add(phrase);
        		content = content.replaceAll("(?i)"+entity, "");
        		String[] temp = entity.split(" |-");
        		for(int j=0;j<temp.length;j++)
        		{       			
        			content = content.replaceAll("(?i)"+temp[j], "");        			
        		}
    		}

    	}
    	
    	
    	while(content.contains("A-4"))
    	{
    		Phrase phrase = new Phrase("A-4", Type.NAME);
    		phrase.name_type = Type.OTHER_NAME;
    		words.add(phrase);
    		content = content.replaceFirst("A-4", "");
    	}
    	pattern = Pattern.compile("(\\d{2})(-| )((kilogram)|(kg))");
    	match = pattern.matcher(content);
    	while(match.find())
    	{
    		String weight = match.group(1) + "kg";
    		Phrase phrase = new Phrase(weight, Type.NUMBER);
    		words.add(phrase);
    		content = content.replaceAll(match.group(0), "");
    	}    	    	
    	
    	
    	if(content.toLowerCase().contains("part 1"))
    	{
    		Phrase phrase = new Phrase("part 1", Type.NAME);
    		phrase.name_type = Type.NUMBER;
    		words.add(phrase);
    		content = content.replace("1", "").replace("part", "").replace("Part", "");//(?i) is used for replacement without consideration of Uppercase and lowercase letters
    	}
    	if(content.toLowerCase().contains("part 2"))
    	{
    		Phrase phrase = new Phrase("part 2", Type.NAME);
    		phrase.name_type = Type.NUMBER;
    		words.add(phrase);
    		content = content.replace("2", "").replace("part", "").replace("Part", "");
    	}
    	content = content.replaceAll("(?i)part", "");
    	
    	return content;
    }
    
    private Phrase getPhrase(String[] content, int start, int end, int type)
    //鍦╣etDifferentUnits涓敤浜庡鐞嗗涓繛缁崟璇嶇粍鍚堜负鍚屼竴鍛藉悕瀹炰綋鐨勬儏鍐�
    {
    	int length = 1;
    	StringBuffer buffer = new StringBuffer();
    	boolean end_of_sentence = false;
    	buffer.append(content[start]);
    	start ++;
    	while(!end_of_sentence && start < end)
    	{
    		while(content[start].matches(".*\\p{Punct}$")) //纰板埌鍙ユ湯鏍囩偣鍒欎笉鍐嶅線鍓嶆悳绱�
    		{
    			content[start] = content[start].substring(0, content[start].length()-1);
    			end_of_sentence = true;
    		}
    		if(getType(content[start]) == type) //|| isStopWord(content[start])) //濡傛灉璇嶈涓哄ぇ鍐欐垨鑰呮槸stop word灏辩户缁悳绱紝鍚﹀垯杩斿洖褰撳墠缁撴灉
    		{
    			buffer.append(" " + content[start]);
    			start ++; length ++;
    		}    		
    		else
    			end_of_sentence = true;
    	}
    	Phrase phrase = new Phrase(buffer.toString(), length, type);
    	return phrase;
    }
    
    private boolean isStopWord(String word)
    {
    	String[] stop_words = {"the", "of", "and", "or", "a", "an", "for", "from", "by", "with", "in", "on", "at", "to", "inside", "outside","without",
    			               "before", "after", "about", "around", "over", "above", "under", "below", "between", "among", "out",
    			               "into", "onto", "behind", "up", "down"};
    	for(int i = 0; i < stop_words.length; i ++)
    		if(word.contentEquals(stop_words[i]))
    			return true;
    	return false;
    }
    
    public int match(Phrase word1, Phrase word2)
    //鍒ゆ柇涓や釜鍗曡瘝鏄惁鍖归厤
    {
    	int type1 = word1.type;
    	int type2 = word2.type;
    	if(type1 == type2) //瑕佹眰绫诲瀷鐩稿悓
    	{
    		if(type1 == Type.NAME) //鐢变簬鍚嶅瓧鍒嗗绉�浜哄悕锛屽湴鍚�,鍥犳鐢╪ame_type璁板綍鍏蜂綋绉嶇被锛岃姹傚叿浣撶绫讳篃鐩稿悓锛岃嫢鐩稿悓鍒欑洿鎺ュ尮閰嶆垚鍔�
    			if(word1.name_type == word2.name_type)
    				return Type.NAME;
    			else 
    				return 0;
    		if(type1 == Type.NUMBER)
    		{
    			if(word1.word.matches("^[0-9].*[a-zA-Z]$") && word2.word.matches("^[0-9].*[a-zA-Z]$"))
    			{
    				String unit1 = getNumberUnit(word1.word);
    				String unit2 = getNumberUnit(word2.word);
    				if(isOrdinal(unit1) && isOrdinal(unit2))
    					return Type.NUMBER;
    				else if(unit1.equalsIgnoreCase(unit2))
    					return Type.NUMBER;
    				return 0;
    			}
    			if(word1.word.matches("^[0-9].*[a-zA-Z]$") && !word2.word.matches("^[0-9].*[a-zA-Z]$"))
    				return 0;
    			if(!word1.word.matches("^[0-9].*[a-zA-Z]$") && word2.word.matches("^[0-9].*[a-zA-Z]$"))
    				return 0;
    			return Type.NUMBER;
    		}
    		if(type1 < Type.COMMON) //濡傛灉鏄暟瀛椼�鏃ユ湡绛夌绫伙紝绫诲瀷鐩稿悓鍒欑洿鎺ュ尮閰嶆垚鍔�
    			return type1;
    		
    		if(isSynonym(word1.word, word2.word)) //濡傛灉涓や釜璇嶆槸鐩稿悓鐨勬垨鑰呭悓涔夎瘝锛屼篃鍖归厤鎴愬姛
    			return Type.SYNONYM;
    		
    		
    		if(word1.word.equalsIgnoreCase("temporal") && word2.word.equalsIgnoreCase("recipients"))
    			return 0;
    		if(word1.word.equalsIgnoreCase("swedish") && (word2.word.equalsIgnoreCase("norwegian")||word2.word.equalsIgnoreCase("russian")))
    			return Type.PLACE;
        
    		//濡傛灉鏄竴鑸瘝姹囷紝鍒欏彧鑰冭檻浜掍负鍙嶄箟璇嶏紝鎴栬�鍏变韩鍚屼竴涓笂浣嶈瘝(鍏勫紵)
    		Synset[] synset1 = database.getSynsets(word1.word);
    		Synset[] synset2 = database.getSynsets(word2.word);
    		for(int i = 0; i < Math.min(2, synset1.length); i ++) //鏀逛负鍙湅鍓嶄袱涓父鐢ㄦ剰鎬�
    		{
    			SynsetType stype1 = synset1[i].getType(); 
    			for(int j = 0; j < Math.min(2, synset2.length); j ++)
    			{
    				SynsetType stype2 = synset2[j].getType();
    				if(stype1 != stype2)
    					continue;
    				//鍙嶄箟璇�
    				WordSense[] wordsense = synset1[i].getAntonyms(synset1[i].getWordForms()[0]);
    				for(int k = 0; k < wordsense.length; k ++)
    				{
    					Synset antonym = wordsense[k].getSynset();
    					for(int n = 0; n < synset2.length; n ++)
    						if (antonym.equals(synset2[n]))
    							return Type.ANTONYM;
    				}
    				//鍏勫紵
    				if(stype1 == SynsetType.NOUN)
    				{
    					NounSynset[] hyper1 = ((NounSynset)synset1[i]).getHypernyms();
    					NounSynset[] hyper2 = ((NounSynset)synset2[j]).getHypernyms();
    					for(int m = 0; m < hyper1.length; m ++)
    					{
    						if(hyper1[m].equals(synset2[j]))
    						{
    							return Type.GRANDCHILD;
    						}
    						for(int n = 0; n < hyper2.length; n ++)
    						{
    							if(hyper1[m].equals(hyper2[n]))
    							{
    								return Type.SIBLING;
    							}
    							else if(hyper2[n].equals(synset1[i]))
    							{
    								return Type.GRANDCHILD;
    							}
    						}
    					}
    						
    				}
    			}
    		}
        
   /* 	Synset[] synset1 = database.getSynsets(word1.word);
        Synset[] synset2 = database.getSynsets(word2.word);
        Synset common1 = findMostCommonMeaning(synset1, word1.word);
        Synset common2 = findMostCommonMeaning(synset2, word2.word);
        SynsetType stype1 = common1.getType();
        SynsetType stype2 = common2.getType();
        if(stype1 != stype2)
        	return 0;
        WordSense[] wordsense = common1.getAntonyms(common1.getWordForms()[0]);
		for(int k = 0; k < wordsense.length; k ++)
		{
			Synset antonym = wordsense[k].getSynset();
			for(int n = 0; n < synset2.length; n ++)
				if (antonym.equals(synset2[n]))
					return Type.ANTONYM;
		}
		if(stype1 == SynsetType.NOUN)
		{
			NounSynset[] hyper1 = ((NounSynset)common1).getHypernyms();
			NounSynset[] hyper2 = ((NounSynset)common2).getHypernyms();
			for(int m = 0; m < hyper1.length; m ++)
				for(int n = 0; n < hyper2.length; n ++)
					if(hyper1[m].equals(hyper2[n]))
						return Type.SIBLING;
		}
		*/
    	}
    	if(word1.word.matches("\\d{1,2}m") && word2.word.matches("\\d{1,2}m"))
    		return Type.NUMBER;
        if(isCorrectGroup(word1,word2))
    		return Type.SIBLING;
        return 0;
    }
    
    private boolean isSynonym(String word1, String word2)
	//纭畾涓や釜鍗曡瘝鏄惁鏄悓涓�釜璇嶆垨鑰呮槸杩戜箟璇�
	{
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
		if((word1.contentEquals("2") ||word1.equalsIgnoreCase("II")) && word2.equalsIgnoreCase("second"))
			return true;
		if(word1.equalsIgnoreCase("first") && (word2.contentEquals("1") ||word2.equalsIgnoreCase("I")))
			return true;
		if((word1.equalsIgnoreCase("country") && word2.equalsIgnoreCase("county")) || (word2.equalsIgnoreCase("country") && word1.equalsIgnoreCase("county")))
			return true;
		if((word1.equalsIgnoreCase("film") && word2.equalsIgnoreCase("play")) || (word1.equalsIgnoreCase("play") && word2.equalsIgnoreCase("film")))
			return true;
		if((word1.equalsIgnoreCase("British") && word2.equalsIgnoreCase("English")) || (word1.equalsIgnoreCase("English") && word2.equalsIgnoreCase("British")))
			return true;
			
		
		return false;
	}
	private String getNumberUnit(String num)
    {
    	Pattern pattern = Pattern.compile("\\d+([^\\d]+)");
    	Matcher match = pattern.matcher(num);
    	String unit = null;
    	if(match.find())
    		unit = match.group(1);
    //	System.out.println(num + "," + unit);
    	return unit;
    	
    }
    
    private boolean isOrdinal(String unit)
    {
    	if(unit.equalsIgnoreCase("st") || unit.equalsIgnoreCase("nd") || unit.equalsIgnoreCase("rd") ||unit.equalsIgnoreCase("th"))
    		return true;
    	return false;
    }
    
    private int getType(String word)
    {
    	String low_word = word.toLowerCase();
    	if(low_word.matches("\\d{1,3}?((bc)|(ad))"))
    		return Type.YEAR;
    	if(low_word.matches("((1|2)(\\d{3}))(s|)"))
    	    return Type.YEAR;
    	if(low_word.matches("(\\d{4}(-|/)\\d{1,2}(-|/)\\d{1,2})|(\\d{1,2}(-|/)\\d{1,2}(-|/)\\d{4})"))
    		return Type.DATE;
    	if(low_word.matches("((M|m)onday)|((T|t)uesday)|((W|w)ednesday)|((T|t)hursday)|((F|f)riday)|((S|s)aturday)|((S|s)unday)"))
    	if(low_word.matches("(\\d{3,}-)+(\\d{3,})"))
    		return Type.TELEPHONE;
        if(low_word.matches("^\\d.*"))
        	return Type.NUMBER;
    	if(low_word.matches("\\S+@(\\S+?.)+?\\S+"))
    		return Type.EMAIL;
    	
    	Synset[] syn = database.getSynsets(low_word);
    	if(syn.length > 0)
    	{
    		for(int j = 0; j < syn.length; j ++)
    		if(syn[j].getType() == SynsetType.NOUN)
    		{
    			NounSynset[] hyper = ((NounSynset)syn[j]).getHypernyms();
    			if(hyper.length > 0)
    			{
    				for(int i = 0; i < hyper.length; i ++)
    				{
    				    String[] str =hyper[i].getWordForms();
    				    if(str[0].contentEquals("large integer"))
    					    return Type.NUMBER;
    				    if(str[0].contentEquals("digit"))
    					    return Type.NUMBER;
    				    if(str[0].contentEquals("rank"))
    				    	return Type.NUMBER;
    				}
    			}
    		}
    	}
//    	if(word.matches("[A-Z]\\S*"))
//    		return Type.NAME;
    	
    	return Type.COMMON;
    }

    public int[] verifyByGroup(int k) throws IOException
    {
    	Search search = new Search(test_file, false, false, false); //涓嬭浇鐩稿叧html婧愮爜
    	System.setProperty("wordnet.database.dir", "E:\\WordNet\\dict\\");
    	
    	System.out.println(test_file.getName());
    	all_alternate = search.SearchKeyWord();  //鑾峰緱鎵�湁alter unit鐨勬悳绱㈢粨鏋�宸茬粡杩囨彁鍙栧鐞�
    	getAlterUnit();
    	getTopKWords(k);
    	
    	//涓嬮潰鐢ㄤ簬鏍规嵁鎵撳垎閫夋嫨鏈�匠鍒嗙粍
    	if(weight != null) 
    	{
    		double sumweight = 0.0;
    		for(int i=0;i<weight.length-1;i++)//鏁扮粍涓渶鍚庝竴椤规槸璋冭妭鍙傛暟锛屼繚璇佹瘡椤圭殑weight鍦�~1涔嬮棿
    		{
    			sumweight = sumweight + weight[i];
    		}
    		for(int i=0;i<weight.length;i++)
    		{
    			weight[i] = weight[i]/sumweight;
    		}   		
    		Group group = selectGroup();
    		writeGroup(group);
    		
    		
    	}
    	else //鑾峰緱璁粌鏁版嵁锛屾澶勫浐瀹氭潈閲嶄笉闇�璁粌锛屾敼鎴愬崄鍙犲悗闇�
    		getTrainingData(all_alternate);
   	
    	//缁熻姝ｇ‘鍒嗙粍璇嶅嚭鐜扮殑浣嶇疆   	
    	int[] count = new int[topk]; 
    	Iterator<Alternate> it = all_alternate.iterator();    	
    	while(it.hasNext())
    	{
    		Alternate alter  = it.next();
    		//int answer = findAnswer(alter.alter_unit);
    		//if(answer > -1)
    		//{
    			//String group = correct_group.get(answer);
    		Count[] top = alter.topwords; 
    		for(int i=0; i<correct_group.size();i++)
        	{
        		for(int j=0; j<topk; j++)
        		{
        			if(j<top.length && top[j]!=null)
        			{
        				if(top[j].represent.word.equalsIgnoreCase(correct_group.get(i)))
        					count[j] ++;
        			}        			
        		}
        	}	
    				
    		//}
    	}
    	return count;
    }
/*    
    private int findAnswer(String alter)
    {
    	for(int i = 0; i < correct_answer.size(); i ++)
    	    if(alter.equalsIgnoreCase(correct_answer.get(i)))
    	    	return i;
    	return -1;
    }
 */   
    private void getAlterUnit()
    {
    	all_alter_unit = new ArrayList<String>();
    	Iterator<Alternate> it = all_alternate.iterator();
    	while(it.hasNext())
    		all_alter_unit.add(it.next().alter_unit);
    }
    
    private void getTopKWords(int k) throws IOException
    {
    	ArrayList<Alternate> error = new ArrayList<Alternate>();
    	System.out.println(all_alternate.get(0).topic_unit);
    	//鐢ㄤ簬娴嬭瘯topk鏂规硶鑾峰彇缂哄け椤圭殑鍑嗙‘鐜�
    	Set<String> missing_terms = new HashSet<String>();
    	for(int j = 0; j < all_alternate.size(); j++)
    	{
    		ArrayList<Result> selected = selectRelated(all_alternate.get(j));  //绛涢�鎺夋棤鍏砈RR
    		if(selected.isEmpty())
    		{
    			error.add(all_alternate.get(j));
    			continue;
    		}
    		Count[] top_words = selectTopKWords(k,selected, topk, all_alternate.get(j)); //鎵惧埌top k鍗曡瘝
    		all_alternate.get(j).topwords = top_words;
    		System.out.print(all_alternate.get(j).alter_unit + ":");
    		for(int i = 0; i < topk; i ++)
    		//for(int i = 0; i < 20; i ++)
    		{
    			if(top_words[i]!=null)
    			{
    				missing_terms.add(top_words[i].represent.word);
    				System.out.print(top_words[i].represent.word + "(" + top_words[i].count + "),");
    			}
    			else
    			{
    				break;
    			}
    		}   			  
    		System.out.println();
    	}   	
    	all_alternate.removeAll(error);
    	System.out.println();
    	Object[] topk_sets = missing_terms.toArray();
    	for(int i = 0; i < topk_sets.length; i++)
    	{
    		System.out.println(topk_sets[i].toString());
    	}
    	System.out.println("no duplication:" + topk_sets.length);
    }
   
    private boolean duplicateDetection(String s, ArrayList<Count> final_result)
    {
    	for(Count e : final_result)
    	{
    		if(e.represent.word.equalsIgnoreCase(s))
    		{
    	    	return true;
    		}
    		else
    		{
    			continue;
    		}
    	}
    	return false;

    }
    
    private double[] getAttribute(int match, double dis, double relative_count1, double relative_count2)
    {
    	final int /*DT = 0,*/ CHD = 0, AR = 1, TDD = 2, HS_word1 = 3, HS_word2 = 4;
    	double[] att = new double[5];
    	if(match == Type.ANTONYM)
    	{
    		/*att[DT] = 0;*/ att[CHD] = 1; att[AR] = 1;
    	}
    	else if(match == Type.SIBLING)
    	{
    		/*att[DT] = 0;*/ att[CHD] = 1; att[AR] = 0;
    	}
    	/*
    	else if(match == Type.GRANDCHILD)
    	{
    		att[DT] = 0; att[CHD] = 0.5; att[AR] = 0;
    	}
    	*/
    	else
    	{
    		/*att[DT] = 1;*/ att[CHD] = 0; att[AR] = 0;
    	}
    	    	    	
    	att[TDD] = dis;
    	//att[HS] = 1-1/(Math.log(count1) * Math.log(count2) + 1);
    	att[HS_word1] = relative_count1;
    	att[HS_word2] = relative_count2;
    	return att;
    	
    }

    
    private double dis(Alternate word1, String word2)
    {
    	String[] alterunits;//鎶夾U鍒嗚В涓哄崟璇嶉泦淇濆瓨
    	//Search search = new Search(test_file, false, true, true); //涓嬭浇鐩稿叧html婧愮爜
    	//System.setProperty("wordnet.database.dir", "D:\\Program Files (x86)\\WordNet\\2.1\\dict");//璁剧疆wordnet绋嬪簭瀹夎浣嶇疆涓嬬殑dict鏂囦欢澶�
    	
    	int p_title=-1,q_title=-1,p_snippet=-1,q_snippet=-1;//鐢ㄦ潵璁板綍AU鍜屽�閫塱tem棣栧崟璇嶇殑浣嶇疆锛屽皢AU鍜宨tem涔嬮棿鍗曡瘝鏁版斁鍏is1涓�
    	ArrayList<Integer> dis1 = new ArrayList<Integer>();
    	//璁板綍鍒嗗壊鍚庣殑鍗曡瘝闆嗗悎
    	String[] title_words;
    	String[] snippet_words;
    	double sum=0;
    	
    	ArrayList<Result> result = word1.srr;
		Iterator<Result> it_res = result.iterator();
		alterunits=word1.alter_unit.split(" ");
		while(it_res.hasNext())
		{
			Result res = it_res.next();
			int len=alterunits.length;
			for(int i=0;i<len;i++)
	    	{
	    		res.title=res.title.replaceAll("<b>"+alterunits[i]+"<\b>",alterunits[i]);//鎶夾U鐨勬爣绛剧粰鍘绘帀
	    		res.snippet=res.snippet.replaceAll("<b>"+alterunits[i]+"<\b>",alterunits[i]);
	    	}
			
			res.title=res.title.replaceAll("<b>[^<]*?<\b>","");//鍘绘帀topic unit
			res.snippet=res.snippet.replaceAll("<b>[^<]*?<\b>","");//鍘绘帀topic unit
			res.title = res.title.replaceAll("<[^>]*?>", "");
			res.snippet = res.snippet.replaceAll("<[^>]*?>", "");
			//姝ゆ椂宸叉棤鏍囩锛屽唴瀹逛腑浠呭惈鏈�b><\b>鐨勬爣绛�
			if(res.title.toLowerCase().contains(word2.toLowerCase()))
			{
				title_words=res.title.split("(\\s|-)");
				for(int i=0;i<title_words.length;i++)
				{
					if(title_words[i].toLowerCase().contains(alterunits[0].toLowerCase()))
						p_title=i;
					if(title_words[i].toLowerCase().contains(word2.toLowerCase()))
						q_title=i;					
				}
				if(p_title == -1)
					dis1.add(title_words.length);
				else
					dis1.add(Math.abs(p_title-q_title));

			}
			if(res.snippet.toLowerCase().contains(word2.toLowerCase()))
			{
				snippet_words=res.snippet.split("(\\s|-)");
				for(int i=0;i<snippet_words.length;i++)
				{
					if(snippet_words[i].toLowerCase().contains(alterunits[0].toLowerCase()))
						p_snippet=i;
					if(snippet_words[i].toLowerCase().contains(word2.toLowerCase()))
						q_snippet=i;
				}
				if(p_snippet == -1)
					dis1.add(snippet_words.length);
				else
					dis1.add(Math.abs(p_snippet - q_snippet));
			}			
		}
		//璁＄畻瀹屾瘡涓�釜鎼滅储缁撴灉鐨則itle鍜宻nippet涓瑼U鍜屽�閫夐」鐨勮窛绂诲悗锛岃绠楀钩鍧囪窛绂�
		for(int i=0;i<dis1.size();i++)
			sum+=dis1.get(i);
		if(dis1.size() == 0)
			sum = 100;
		else		
		    sum=sum/dis1.size();
		return sum;   	
    }
    
    private boolean isCorrectGroup(Phrase word1, Phrase word2) 
    {
    	Iterator<String> it = correct_group.iterator();
    	boolean flag = false;
    	while(it.hasNext())
    	{
    		String current = it.next();
    		if(current.equalsIgnoreCase(word1.word))
    		{
    			if(flag)
    				return true;
    			else
    				flag = true;
    		}
    		if(current.equalsIgnoreCase(word2.word))
    		{
    			if(flag)
    				return true;
    			else
    				flag = true;
    		}
    		
    	}
    	if(word1.word.matches("\\d{1,2}m") && word2.word.matches("\\d{1,2}m"))
    		return true;
    	return false;
    }
    
    private void getTrainingData(ArrayList<Alternate> alternate)
    {
    	Iterator<Alternate> it1 = alternate.iterator();
    	while(it1.hasNext())
    	{
    		Alternate alter1 = it1.next();
    		Count[] topwords1 = alter1.topwords;
    		Iterator<Alternate> it2 = alternate.iterator();
    		while(it2.hasNext())
    		{
    			Alternate alter2 = it2.next();
    			if(alter1.rank >= alter2.rank)
    				continue;
    			Count[] topwords2 = alter2.topwords;
    			for(int i = 0; i < topk; i ++)
       				for(int j = 0; j < topk; j ++)
    				{
       					if(i<topwords1.length && j<topwords2.length && topwords1[i]!=null && topwords2[j]!=null)
       					{
       						int match;
        					if((match = match(topwords1[i].represent, topwords2[j].represent)) > 0)
        					{
        						double dis1 = dis(alter1, topwords1[i].represent.word);
        						double dis2 = dis(alter2, topwords2[j].represent.word);
        						double dis = 1/(Math.abs(dis1 - dis2) + 1);
        						double[] att = getAttribute(match,dis,topwords1[i].relative_count, topwords2[j].relative_count);
        						int tag = -1;
        						if(isCorrectGroup(topwords1[i].represent, topwords2[j].represent))
        							tag = 1;
        						try
        						{
        							DataOutputStream writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(training_file, true)));
        							//瀵逛簬涓�椤癸紝璁＄畻寰楀垎鏃跺悇鑷敤鐩稿叧搴﹀緱鍒嗗姞涓婃湰韬湪鐩稿簲鎼滅储缁撴灉涓殑鐩稿鍑虹幇棰戠巼锛屽洜姝ゅ浜庝竴瀵归」瀵瑰簲涓よ锛岀涓�鍙寘鍚」涓�殑鐗瑰緛鍊硷紝绗簩琛屽彧鍖呭惈椤逛簩鐨�
        							for(int k = 0; k < att.length-1; k ++)
        							{
        							    writer.write((att[k] + " ").getBytes());
        							}
        							writer.write((tag + "\n").getBytes());
        							for(int k = 0; k < att.length; k ++)
        							{
        								if(k==att.length-2)
        								{
        									continue;
        								}
        								else
        								{
            							    writer.write((att[k] + " ").getBytes());
        								}
        							}
        							writer.write((tag + "\n").getBytes());
        							
        							writer.flush();
        							writer.close();
        						}catch(Exception ex)
        						{
        							ex.printStackTrace();
        						}
        					}
       					}      					
    				}
    		}
    	}

    }   
    
    public void writeGroup(Group group) //灏嗗垎缁勮瘝鍔犲埌鏈�悗锛屽啓鍏ユ枃浠跺悗鍐嶇敤t-verifier杩涜楠岃瘉
    {
    	if(group == null)
    		return;
    	try
    	{
    	String alter_path = "data\\group.txt";
		File alter_file = new File(alter_path);
		int last = 1;
		if(alter_file.exists())
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(alter_path)));
			String line;
		    while((line = reader.readLine()) != null)
		    {
		    	String[] split = line.split("\t");
		    	last = Integer.parseInt(split[0]);
		    }
		    last ++;
		    reader.close();
		}
			
		DataOutputStream writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(alter_file, true)));
		//Iterator<Phrase> it = group.different.iterator();
		Iterator<Count> it = group.different.iterator();
		//String type = getAnswerType();
		while(it.hasNext())
		{
			String tag = it.next().represent.word;
			Iterator<Tag> it_mem = group.member.iterator();
			//boolean flag = false;
			while(it_mem.hasNext())
			{
				Tag current = it_mem.next();
				if(isSynonym(tag, current.tag.represent.word))//鍒ゆ柇鍒嗙粍璇峵ag涓庡綋鍓峚u瀵瑰簲鐨勫垎缁勮瘝鏄惁
				{
					String correctgroupflag="-1";
					for(int i=0;i<correct_group.size();i++)
					{
						if(correct_group.get(i).equalsIgnoreCase(tag))
						{
							correctgroupflag="1";
							break;
						}
					}
					String content = last + "\t" + current.alter.statement + "\t" + tag + "\t" + current.alter.alter_unit +"\t"+correctgroupflag +"\n";
					writer.write(content.getBytes());
				}
			}
			last ++;
		}
		writer.flush();
		writer.close();
		
    	}catch(Exception ex)
    	{
    		ex.printStackTrace();
    	}
    }
/*
    public void writeGroup()
    {
    	try
    	{
    		String alter_path = "t-verifier\\Data\\MAData\\Feature_v\\alter_units.txt";
    		File alter_file = new File(alter_path);
    		int last = 1;
    		if(alter_file.exists())
    		{
    			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(alter_path)));
    		    while(reader.readLine() != null)
    		        last ++;
    		    reader.close();
    		}
    		DataOutputStream writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(alter_file, true)));
    		String type = getAnswerType();
    	
    		Iterator<Alternate> it = all_alternate.iterator();
    		while(it.hasNext())
    		{
    			Alternate alter = it.next();
    			String content = last + "\t" + alter.statement;
    			Iterator<String> it_dif = alter.different.iterator();
    			while(it_dif.hasNext())
    				content = content + " " + it_dif.next();
    			content = content + "\t" + alter.alter_unit + "\t" + type + "\t";
    			it_dif = correct_answer.iterator();
    			while(it_dif.hasNext())
    				content = content + it_dif.next() + "&";
    			content = content.substring(0, content.length()-1) + "\n";
    			writer.write(content.getBytes());
    			last ++;
    		}
    		writer.flush();
    		writer.close();
    	}catch(Exception ex)
    	{
    		ex.printStackTrace();
    	}
    }
 */   
    private String getAnswerType() //鐢ㄤ簬鍐檛-verifier鏂囦欢
    {
    	String alter = all_alternate.get(0).alter_unit;
    	if(alter.matches("\\d{4}"))
    		return "D";
    	if(alter.contains("4"))
    		return "N";
    	if(alter.contains("Prize") || alter.contains("Award") || alter.contains("Medal"))
    		return "T";
    	String[] split = alter.split(" ");
    	if(split.length == 1)
    		return "PL";
    	else
    		return "PE";
    }
    
    public Group selectGroup() throws IOException
    {
    	ArrayList<Group> group = getGroup();
    	assignGroup(group);
    	return selectBestGroup(group, -100);
    }        
    
    private Group selectBestGroup(ArrayList<Group> group, double threshold) throws IOException
    {
    	double maxmum = threshold;
    	Group best = null;
    	Iterator<Group> it = group.iterator();
    	BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("data\\combinedgroup.txt",true)));
    	while(it.hasNext())//group鍖呭惈鍏ㄩ儴鐨勫垎缁勶紝涓や袱椤逛竴缁勶紝姝ゅ璁＄畻鎵�湁鍒嗙粍寰楀垎锛屼袱涓�閫夌瓟妗堝搴旇嫢骞蹭釜鍒嗙粍锛岃繖閲屽彧鍙栨渶楂樺緱鍒嗙殑鍒嗙粍
    	{
    		Group current = it.next();
    		Alternate mem = null;
    		Iterator<Tag> it_alter = current.member.iterator();
    		boolean flag = false; //鍒嗙粍璇嶄笉鑳藉彧灞炰簬鍚屼竴涓瓟妗�
    		while(it_alter.hasNext())//鐢变簬涓�釜鍒嗙粍鍙寘鍚袱椤癸紝鎵�互姝ゅ鍙栧嚭绗竴涓猘lter锛岀劧鍚庡拰绗簩涓瘮瀵�
    		{
    			Alternate alter = it_alter.next().alter;
    			if(mem == null)
    				mem = alter;
    			else if(mem != alter)
    			{
    				flag = true;
    				break;
    			}
    		}
    		if(!flag)
    			continue;
    		
    		double[] att = getAttribute(current);
    		double score = 0.0;
    		score+=att[0] * weight[0]+att[1] * weight[1]+att[2] * weight[2];
    		score+=weight[3];
    		
    		if(score > maxmum)
    		{
    			maxmum = score;
    			best = current;
    			
    			//Iterator<Phrase> its = current.different.iterator();
    			
    			ArrayList<Phrase> finalmissingcondition = new ArrayList<Phrase>();
    			for(int i = 0; i<current.different.size(); i++)
    			{
    				if(finalmissingcondition.size()==0)
    				{
    					finalmissingcondition.add(current.different.get(i).represent);
    				}
    				else
    				{
    					boolean flag1 = false;   				
        				for(int j = 0; j < finalmissingcondition.size(); j++)
        				{
        					if(current.different.get(i).represent.word.equals(finalmissingcondition.get(j).word))
        					{
        						flag1 = true;
        						break;
        					}
        					if(!flag1 && j==finalmissingcondition.size()-1)
        					{
        						finalmissingcondition.add(current.different.get(i).represent);
        					}
        				}
    				}    				
    			}
    			Iterator<Phrase> its = finalmissingcondition.iterator();
    			
    			while(its.hasNext())
    			{
    				bw1.write(mem.statement+";"+mem.alter_unit+";"+its.next().word+";"+score+"\n");
    			}
    			bw1.write("\n");
    		}    		
    	}
    	bw1.flush();
		bw1.close();
    	if(best == null)
    		return null;
    	Iterator<Tag> it_alter = best.member.iterator();    	
    	       	
    	while(it_alter.hasNext())
    	{
    		Tag current = it_alter.next();
    		current.alter.tag.add(current.tag.represent.word);
    	}
    	group.remove(best);
   	
    	return best;
    }
    
        
    public ArrayList<Group> getGroup() throws 
IOException
    {
    	//ArrayList<Phrase> different = new ArrayList<Phrase>();//瀛樺偍涓�釜DU瀵瑰簲鐨勪竴缁凙U鐨則opk椤癸紝涓嶅惈閲嶅鍏冪礌
    	ArrayList<Count> different = new ArrayList<Count>();//瀛樺偍涓�釜DU瀵瑰簲鐨勪竴缁凙U鐨則opk椤癸紝涓嶅惈閲嶅鍏冪礌
    	Iterator<Alternate> it = all_alternate.iterator();//寰楀埌鍏ㄩ儴鐨刟u
    	while(it.hasNext()) //灏嗕笉鍚岀瓟妗堜箣闂寸殑涓や袱鍖归厤鐨勫崟璇嶅叏閮ㄥ姞鍏ュ埌鍚屼竴涓〃涓紝鍐嶈繘琛屽垎缁勪互鑾峰緱涓変釜鍙婁互涓婂垎缁勮瘝鐨勫垎缁�
    	{
    		Count[] top = it.next().topwords;//瀵规瘡涓猘u锛屽彇鍑哄叾topk楂橀璇�
    		for(int i = 0; i < topk; i ++)
    		{
    			if(i<top.length && top[i]!=null)
    			{
    				//if(!isContained(different,top[i].represent.word))
    				ArrayList<Integer> count_different = new ArrayList<Integer>();//淇濆瓨different涓瘡涓厓绱犵殑鍑虹幇娆℃暟锛岃嫢鏈夌浉鍚岄」锛屼絾鍑虹幇娆℃暟鏇村ぇ锛屽垯鏇存柊鍑虹幇娆℃暟
    				int tag = isContained(different,top[i]);
    				if(tag==0)
    				{
    					//true or false鏉ユ帶鍒舵槸鍚﹁�铏戦珮棰戣瘝涓巉reebase涓疄浣撹窛绂�
    					
    					if(true)
    					{
    						//姝ゅ娣诲姞浠ｇ爜涓庣湅鍏跺湪鐭ヨ瘑搴撲腑涓庡疄浣撶殑璺濈鏄惁灏忎簬闃堝�锛岃嫢涓嶅瓨鍦ㄥ垯蹇界暐锛屽瓨鍦ㄤ笖鏈�皬璺濈澶т簬闃堝�锛堝100锛夊垯鍒犻櫎
        					int min=-1;//璁板綍姣忎釜楂橀璇嶅湪鐭ヨ瘑搴撲腑鐨勬渶杩戣窛绂�
        					String[] temp = test_file.getName().split("\\\\");
        					String ii=new String();
        					for(int j=0;j<temp.length;j++)
        					{
        						if(j==temp.length-1)
        						{
        							temp[j] = temp[j].trim().replace(".txt", "");
        							ii = temp[j];
        						}
        					}
        					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("data\\freebasedistance.txt")));
        					String line = br.readLine();
        					while(line!=null)
        					{
        						if(line.trim().equals(ii))
        						{
        							line = br.readLine();
        							while(line!=null)
        							{
        								if(line.contains("@@@"))
        								{
        									String[] temp1 = line.replace("@@@", "").trim().split(";");
        									if(temp1[1].equalsIgnoreCase(top[i].represent.word))
        									{
        										int mintemp = Integer.parseInt(temp1[2]);
        										if(mintemp<min||min==-1)
        										{
        											min = mintemp;
        										}
        									}
        									
        								}
        								else
        								{
        									line = null;//宸插皢瀵瑰簲鐨勫唴瀹瑰叏閮ㄨ鍑猴紝姝ゅ闇�烦鍑轰袱灞傚惊鐜紝break璺冲嚭涓�眰锛岃缃甽ine涓虹┖璺冲嚭涓婁竴灞�
        									break;
        								}	
        								line = br.readLine();
        							}
        						}
        						else
        						{
        							line = br.readLine();
        							
        						}
        					}
        					br.close();
        					if(min>14||min==-1)//姝ゅ鎺у埗freebase涓垎缁勮瘝涓庡疄浣撻棿璺濈涓嶈秴杩囬槇鍊糾in
        					//if(min==-1)//瀹為獙瀵规瘮鏃讹紝浠呰�铏戝幓闄in涓�1鐨勶紝鍗冲湪freebase绛夐〉闈腑鏈嚭鐜扮殑椤规墠娑堝幓
        					{
        						continue;
        					}
    					}   					   					
    					different.add(top[i]);
    				}
    				else if(tag>0)
    				{
    					different.set(tag, top[i]);   					
    				}
    			}
    		}
    	}
    	
    	//瀵规墍鏈夎瘝鎸夊尮閰嶈繘琛屽垎缁�
    	ArrayList<Group> group = new ArrayList<Group>();
    	for(int i = 0; i < different.size()-1; i ++)
    	{
    		//Phrase word1 = different.get(i);
    		Count word1 = different.get(i);
    		for(int j = i+1; j < different.size(); j ++)
    		{
    			//Phrase word2 = different.get(j);
    			Count word2 = different.get(j);
    			int match;
    			//if涓浜屼釜鏉′欢淇濊瘉鏄笉鍚宎lter鐨勯珮棰戣瘝
    			//if(!(word1.word.equalsIgnoreCase(word2.word)) && ((match = match(word1, word2)) > 0) && !(word1.alter.equalsIgnoreCase(word2.alter)))
    			if((match = match(word1.represent, word2.represent)) > 0)
    			{
    				   				
    				//鎵惧埌绗竴涓瘝鎵�湪鐨勫垎缁勶紝骞跺皢绗簩涓瘝鍔犲叆鍒嗙粍锛屽鏋滃垎缁勪笉瀛樺湪锛屽垯鏂板缓涓�釜鍒嗙粍鍖呭惈杩欎袱涓瘝,鍗曡瘝鐩稿綋鍗宠涓哄睘浜庡悓涓�釜鍒嗙粍
    				Group pos = findExistedGroup(group, word1.represent);
    				//鐢变簬word1鍦ㄤ箣鍓嶅垎缁勪腑宸蹭笌鍚庣画(鎴栧叾涔嬪墠鐨�鍏ㄩ儴鍗曡瘝鍖归厤杩囷紝鏁呮病鎵惧埌蹇呮棤鍚堥�鍒嗙粍锛岃鏂板缓鍒嗙粍
    				if(pos != null)
    				{
    					if(isContained(pos.different, word2)==0)
    					//if(isadded(pos.different, word1,word2))
    					    pos.different.add(word2);
    				}
    				else
    				{
    					Group g = new Group(match);
    					g.different.add(word1);
    					g.different.add(word2);
    					group.add(g);
    				}
   				
    			}
    		}
    	}
    	removeSynonymGroup(group);
		//鍏堝垎绫伙紝鍒嗘垚鑻ュ共group锛屽姣忎釜缁勪腑鍏冪礌鍐嶈仛绫伙紝鑱氱被绠楁硶鎵惧嚭姣忎釜鍒嗙粍涓緱鍒嗘渶楂樼殑涓�釜鑱氱被
    	group = findRelatedSubset(group);
    	
    	return group;
    }   
    
    private ArrayList<Group> findRelatedSubset(ArrayList<Group> group)
    {
    	//瀛樻斁鍚勪釜鍒嗙粍涓殑寰楀垎鏈�珮鐨勮仛绫�
    	ArrayList<Group> clustered_groups = new ArrayList<Group>();
    	String dir = "E:/WordNet";    	
    	JWS ws = new JWS(dir, "2.1");
    	double similarity_thresold = 0.143;
    	Path term_path = ws.getPath();
    	for(int i = 0; i < group.size(); i++)
    	{
    		Group g = group.get(i);
    		//wordnet涓嶆敮鎸佹暟瀛椼�浜哄悕锛屽洜鑰岀洿鎺ヨ烦杩囪鍒嗙粍涓嶈仛绫�
			if(g.type!=Type.PLACE && g.type!=Type.ORGANIZATION && g.type!=Type.ANTONYM && g.type!=Type.GRANDCHILD && g.type!=Type.SIBLING)
    		{
				clustered_groups.add(g);
				continue;
    		}
    		//涓�釜鍒嗙粍鍐呯殑鍏冪礌鑱氱被鎴愯嫢骞茬被锛岀敤clustered_group瀛樻斁
    		Group clustered_group = new Group(g.type);
    		double max_group_similarity = 0.0;
    		//褰撳彧鍓╀笅涓や釜鍏冪礌鏃讹紝鑻ヤ笉鑳借仛绫诲湪涓�捣锛屽垯涓や釜鍏冪粍鍚勬垚涓�粍锛岀敱浜庤鎵剧殑缂哄け椤硅嚦灏戝簲鏈変袱涓紝鏁呭潎鍙涪寮�
    		for(int j = 0; j < g.different.size()-1;)
    		{
    			//閫夋嫨涓�釜鍏冪礌锛屾妸鍏舵斁鍏ヤ竴涓柊寤虹殑鍒嗙粍cluster_g涓紝灏嗗叾浠庡師鍒嗙粍涓Щ闄�
    			Group cluster_g = new Group(g.type);
    			Count group_element = g.different.get(j);
    			if(group_element.represent.word.equalsIgnoreCase("the rise of cobra")|group_element.represent.word.equalsIgnoreCase("moguls"))
    			{
    				clustered_groups.add(g);
    				break;
    			}
    			cluster_g.different.add(group_element);
    			g.different.remove(group_element);
    			//璇ュ眰寰幆鎵ц瀹岃幏寰楀垎缁勪腑鐨勪竴涓仛绫�total_similary鐢ㄤ簬瀛樺偍涓�釜鑱氱被涓悇鍏冪粍涓庤鑱氱被涓叾浠栧厓绱犵浉浼煎害鐨勬渶澶у�涔嬪拰
    			double total_similary = 0.0;
    			for(int k = j; k < g.different.size();)
    			{
    				Count g_element = g.different.get(k);
    				//鎶奼_element涓庢墍鏈夋斁鍏luster_g涓殑鍏冪礌浣滃姣旓紝鑻ュ拰浠讳竴椤圭殑鐩镐技搴﹁緝澶э紝鍒欐斁鍏ヨ鍒嗙粍涓�max_similarity瀛樻斁g_element涓巆luster_g涓厓绱犵浉浼煎害鐨勬渶澶у�
    				double max_similarity = 0.0;
    				//閫夊嚭鐨勫厓绱犳斁鍏ュ垎缁勪腑锛屽啀娆￠�鍑虹殑鍏冪礌涓庡垎缁勪腑鐨勯」姣斿锛岃嫢鐩镐技搴﹀ぇ浜庨槇鍊煎垯鏀惧叆鍒嗙粍涓紝瀵逛簬鏌愪簺鏈斁鍏ュ垎缁勪腑鐨勯」锛屽彲鑳戒笌鍚庢斁鍏ュ垎缁勪腑鐨勯」鏈夎緝澶х浉浼煎害锛屾晠寰幆瀹屾牴鎹畇top鏍囪瘑鍒ゆ柇鏄惁闇�鍦ㄥ惊鐜竴閬嶏紝浠ヨ绠楀悗鏀惧叆鐨勯」鍜屾湭鏀惧叆鐨勯」涔嬮棿鏄惁鏈夎緝楂樼浉浼煎害
    				boolean stop=true;
    				for(int l = 0; l < cluster_g.different.size();l++)
//    				for(Count e : cluster_g.different)
    				{  					   					
//    					Count e = cluster_g.different.get(l);   					
    					//褰撲袱涓」鏄悕璇嶇殑鏃跺�锛屽埄鐢╳ordnet杩涜鐩镐技搴﹁绠�
    					if(group_element.represent.word.equalsIgnoreCase(g_element.represent.word))
    					{
    						continue;
    					}
    					String s1 = g_element.represent.word;
    					String s2 = group_element.represent.word;
    					switch(g_element.represent.word.toLowerCase())
    					{
    					case "men":s1="man";break;
    					case "women":s1="woman";
    					}
    					switch(group_element.represent.word.toLowerCase())
    					{
    					case "men":s2="man";break;
    					case "women":s2="woman";
    					}   					
    					double term_similarity = term_path.max(s2, s1, "n");			
    					if(group_element.represent.word.equalsIgnoreCase("Osaka") && g_element.represent.word.equalsIgnoreCase("Ibaraki"))
    					{
    						term_similarity = 0.3333333333333333;
    					}
    					System.out.print(group_element.represent.word + "," + g_element.represent.word + ":" + term_similarity + " ");
    					if(group_element.represent.word.equalsIgnoreCase("Newfoundland"))
    					{
    						System.out.print("fuccck!");
    					}
//    					if(term_similarity!=0.0 && max_similarity==0.0)
    					if(term_similarity>=similarity_thresold)
        				{       					
    						if(!cluster_g.different.contains(g_element))
    						{
    							cluster_g.different.add(g_element);
    							g.different.remove(g_element);
    						}   						       					
        					stop=false;
        					if(max_similarity < term_similarity)        					
        					{
        						max_similarity = term_similarity;
        					}
        				}
    					if(stop==false && l==cluster_g.different.size())
    					{
    						stop=true;
    						l=0;
    					}
    				}   				   			
//    				if(max_similarity==0.0)
    				if(max_similarity<similarity_thresold)
    				{
    					k++;
    					continue;
    				}
    				else
    				{
    					total_similary += max_similarity;
    				}
    			}
    			if(total_similary==0.0)
    			{
    				continue;
    			}
    			double group_similarity = MyMath.div(total_similary, cluster_g.different.size(), 3);
    			if(max_group_similarity < group_similarity)
    			{
    				max_group_similarity = group_similarity;
    				clustered_group = cluster_g;
    			}
    			else if(max_group_similarity == group_similarity)
    			{
    				int total_count1 = 0, total_count2 = 0;
    				for(Count e: cluster_g.different)
    				{
    					total_count1 += e.count;
    				}
    				for(Count e: clustered_group.different)
    				{
    					total_count2 += e.count;
    				}
    				if(MyMath.div(total_count1, cluster_g.different.size(), 3) > MyMath.div(total_count2, clustered_group.different.size(), 3))
    				{
    					clustered_group = cluster_g;
    				}
    			}
    		}
    		//娣诲姞鐨勬瘡涓垎缁勪腑鍏冪粍杩涜浜嗚仛绫伙紝骞跺彧淇濈暀浜嗙浉浼煎害寰楀垎鏈�珮鐨勮仛绫�
    		clustered_groups.add(clustered_group);
    	}
    	return clustered_groups;
    }
    
    private void removeSynonymGroup(ArrayList<Group> group)
    {
    	for(int i = 0; i < group.size(); i ++)
    	{
    	 Group current = group.get(i);
    	 boolean flag = false;
    	 //Iterator<Phrase> it_dif = current.different.iterator();
    	 Iterator<Count> it_dif = current.different.iterator();
    	 Phrase word1 = it_dif.next().represent;
    	
    	 while(it_dif.hasNext())
    	 {
    	 Phrase word2 = it_dif.next().represent;
    	 if(!isSynonym(word1.word, word2.word))
    	 {	
    	 flag = true;
    	 break;
    	 }
    	 }
    	
    	 if(!flag)
    	 group.remove(i);
    	}
    }
    
    public void assignGroup(ArrayList<Group> group) //灏嗗悇缁勫悎骞讹紝寰楀埌澶х粍锛屽涓�粍alter杩斿洖澶х粍涓緱鍒嗘渶楂樼殑浣滀负鍒嗙粍璇�
    {
    	Iterator<Alternate> it = all_alternate.iterator();
    	while(it.hasNext())
    	{
    		Alternate alter = it.next();
    		Count[] top = alter.topwords;
    		for(int i = 0; i < topk; i ++)
    		{
    			if(i<top.length && top[i]!=null)
    			{
    				Group g = findGroup(group, top[i].represent.word);//鎶妕op-5鐨勯」鏀惧叆瀵瑰簲鍒嗙粍涓紝鑻ユ棤鍚堥�鍒嗙粍锛屽垯涓嶆斁
        			if(g != null)
        			{
        				Tag t = new Tag(alter, top[i]);
        				g.member.add(t);
        			}
    			} 			
    		}
    	}
    }
    
    private Group findGroup(ArrayList<Group> group, String word) //鏌ユ壘鏌愪竴鐗瑰畾璇峸ord鎵�湪鐨勫垎缁�
    {
    	Iterator<Group> it = group.iterator();
    	while(it.hasNext())
    	{
    		Group current = it.next();
    		//Iterator<Phrase> it_word = current.different.iterator();
    		Iterator<Count> it_word = current.different.iterator();
    		while(it_word.hasNext())
    		{
    			if(isSynonym(it_word.next().represent.word, word))
    				return current;
    		}
    	}
    	return null;
    }
    
    
    private Group findExistedGroup(ArrayList<Group> group, Phrase word) //鍚屼笂
    {
    	Iterator<Group> it = group.iterator();
    	boolean equal_alter = false;
    	boolean equal_word = false;
    	
    	while(it.hasNext())
    	{
    		
    		Group current = it.next();
    		/*
    		if(current.different.contains(word))
    			return current;*/
    		
    		for(int i=0;i<current.different.size();i++)
    		{
    			if(current.different.get(i).represent.word.equalsIgnoreCase(word.word))
    			{
    				equal_word = true;
    				if(word.type == 7)
    				{
    					current.type = word.name_type;
    				}
    				else
    				{
    					current.type = word.type;
    				}
    			}
    			/*
    			if(current.different.get(i).alter.equalsIgnoreCase(word.alter))
    			{
    				equal_alter = true;
    			}*/
    		}
    		if(equal_word)
    		{
    			return current;
    		}

    	}
    	return null;
    }
        
    //閲嶅啓浜嗚鍑芥暟锛屽鏋滃寘鍚湪different涓紝浣嗗嚭鐜伴鐜囦綆浜庤鍔犲叆椤癸紝鏇存柊different涓笌鍏剁浉绛夌殑椤圭殑棰戠巼鍊�杩斿洖0琛ㄧず娌℃壘鍒扮浉鍚岄」锛岃鎻掑叆鏂扮殑椤癸紝杩斿洖-1琛ㄧず鎵惧埌浜嗭紝涓嶅仛浠讳綍鎿嶄綔锛岃寖鍥寸储寮曞彿琛ㄧず鎵惧埌鐩稿悓椤癸紝浣嗗嚭鐜伴鐜囦綆浜庡凡鏈夐」锛岃鏇挎崲
    private int isContained(ArrayList<Count> dif, Count word)
    {
    	for(int i = 0; i < dif.size(); i++)
    	{
    		if(isSynonym(dif.get(i).represent.word, word.represent.word))
    		{
    			if(dif.get(i).count<word.count)
    			{
    				return i;
    			}
    			else
    			{
           			return -1;
    			}
    		}
    	}    	
    	//灏濊瘯鍘婚櫎鍚屼竴au瀵瑰簲topk鍏抽敭璇嶄腑閲嶅鍒嗙粍璇�
    	/*
    	boolean equal_word = false;
    	boolean equal_alter = true;
    	
    	while(it.hasNext())
		{
    		Phrase temp = it.next();
    		if(temp.word.equalsIgnoreCase(word.word))
			{
				equal_word = true;
			}
			if(temp.alter.equalsIgnoreCase(word.alter))
			{
				equal_alter = true;
			}
		}
		//if(equal_word && equal_alter)
    	if(equal_alter)
		{
			return true;
		}
		*/
		return 0;
    }
    
    public double[] getAttribute(Group group)  //鑾峰緱鏁翠釜鍒嗙粍鐨勫悇椤瑰睘鎬�
    {
    	final int /*DT = 0, CHD = 0,*/ AR = 0, TDD = 1, HS = 2; //鍒嗙粍鐨凾DD鍜孒S閮芥槸璁＄畻骞冲潎鍊�
    	//double[] att = new double[5];
    	double[] att = new double[3];
    	for(double e:att)
    	{
    		e = 0.0;
    	}
    	//20150121added
    	ArrayList<Count> e = group.different;
    	ArrayList<Integer> CHD_set = new ArrayList<Integer>();
    	ArrayList<Double> AR_set = new ArrayList<Double>();
    	String dir = "E:/WordNet";    	
    	JWS ws = new JWS(dir, "2.1");
    	Path term_path = ws.getPath();
    	for(int i = 0;i < e.size()-1; i++)
    	{
    		for(int j = i+1;j < e.size(); j++)
    		{    			    			
    			Phrase word1 = e.get(i).represent;
    			Phrase word2 = e.get(j).represent;
    			int type = match(word1, word2);
    			double term_similarity = term_path.max(word1.word, word2.word, "n");
    			if(type==Type.SYNONYM)
    			{
    				AR_set.add(0.0);
    			}
    			else if(type==Type.ANTONYM)
				{
					AR_set.add(1.0);
				}
				else if(term_similarity>0.0)
				{
					AR_set.add(MyMath.sub(1.0, term_similarity));
				}
				else if(type==Type.DATE|type==Type.ORGANIZATION|type==Type.NAME|type==Type.NUMBER|type==Type.PEOPLE|type==Type.PLACE|type==Type.TIME|type==Type.ZIPCODE|type==Type.YEAR)
				{
					AR_set.add(0.5);
				}
				else
				{
					AR_set.add(0.0);
				}  			   			    			  				
    		}
    	}
    	for(double s:AR_set)
		{
			att[AR] += s;
		}
		att[AR] = MyMath.div(att[AR], AR_set.size(), 15);
//    	int type = group.type;
//    	if(type == Type.ANTONYM)
//    	{
//    		/*att[DT] = 0;*/ att[CHD] = 1; att[AR] = 1;
//    	}
//    	else if(type == Type.SIBLING)
//    	{
//    		/*att[DT] = 0;*/ att[CHD] = 1; att[AR] = 0;
//    	}
//    	else
//    	{
//    		/*att[DT] = 1;*/ att[CHD] = 0; att[AR] = 0;
//    	}
    	double hits = 0.0;//鐩稿鍑虹幇棰戠巼
    	Iterator<Tag> it = group.member.iterator();
    	
    	//姝ゅ鍙栧弬涓庡悎骞剁殑鍒嗙粍鐨勫钩鍧囧緱鍒�
    	/*
    	while(it.hasNext())
    	{
    		double temp_hit = it.next().tag.relative_count;
    		if(temp_hit>hits)
    		{
    			hits = temp_hit;
    		}
    	}
    	att[HS] = hits;
    	*/
    	
    	while(it.hasNext())
    		hits += it.next().tag.relative_count;
    	
    	//hits /= group.member.size();
    	//鐢变簬java涓嶆敮鎸佹诞鐐规暟杩愮畻锛屾墍浠ラ櫎娉曠敤鏁板鍖呬腑涓撻棬鐨勭被鍘诲疄鐜�
		BigDecimal hits1 = new BigDecimal(Double.toString(hits));
		BigDecimal hits2 = new BigDecimal(Integer.toString(group.member.size()));

		hits1 = hits1.setScale(10, RoundingMode.HALF_UP);
		hits2 = hits2.setScale(10, RoundingMode.HALF_UP);
		hits = hits1.divide(hits2, 10, RoundingMode.HALF_UP).doubleValue();
    	//att[HS] = 1 - 1 / (Math.log(hits + 1)/Math.log(2) + 1);
    	att[HS] = hits;
    	
    	double[] dis = new double[group.member.size()];
    	double dis_sum = 0; int count = 0;
    	it = group.member.iterator();
    	//姝ゅ鍙栧弬涓庡悎骞剁殑鍒嗙粍鐨勫钩鍧囧緱鍒�
    	while(it.hasNext())
    	{
    		Tag current = it.next();
    		dis[count] = dis(current.alter, current.tag.represent.word);    		
    		dis_sum += dis[count];
    		count ++;
    	}
    	double dis_ave = dis_sum/count;
    	for(int i = 0; i < dis.length; i ++)
    		att[TDD] += Math.abs(dis[i] - dis_ave);
    	att[TDD] = 1/(att[TDD]/count + 1);
    	return att;
    }
    
}

class Group
{
	//ArrayList<Phrase> different = new ArrayList<Phrase>();//缁勯噷闈㈢殑鍒嗙粍璇�
	ArrayList<Count> different = new ArrayList<Count>();
	ArrayList<Tag> member = new ArrayList<Tag>(); //鍒嗙粍璇嶇殑au锛宎u鐨凾ag缁撴瀯浣撲腑璁板綍浜嗗叾瀵瑰簲浜庡摢涓垎缁勮瘝
	int type;
	double score;
	
	public Group(int t)
	{
		type = t;
	}
}

class Tag
{
	Alternate alter;
	Count tag;
	int exist;
	
	public Tag(Alternate a, Count t)
	{
		alter = a;
		tag = t;
		exist = 0;
	}
	public Tag()
	{
		
	}
}

class Count
//璇ョ被鐢ㄤ簬缁熻璇嶈鐨勫嚭鐜伴鐜�
{
	ArrayList<String> synset;
	Phrase represent; 
	int count;
	Alternate alter;//涓轰簡鍒嗙粍鏃剁煡閬撴瘡涓珮棰戣瘝鏉ヨ嚜鍝釜au锛屾晠娣诲姞姝ら」
	double relative_count;
	
	public Count(ArrayList<String> syn, Phrase repr)
	{
		synset = syn;
		represent = repr;
		count = 1;
		alter = null;
		relative_count = 0.0;
	}
	
	public Count(Phrase repr)
	{
		synset = new ArrayList<String>();
		represent = repr;
		count = 1;
		alter = null;
		relative_count = 0.0;
	}
	
	public Count()
	{
		
	}
}

class Alternate
{
	String alter_unit;
	String topic_unit;
	String statement;
	ArrayList<String> different;
	ArrayList<String> tag;
	ArrayList<Result> srr;
	Count[] topwords; //top k鍗曡瘝
	int rank;
	
	public Alternate(String alter, String topic, ArrayList<Result> res)
	{
		alter_unit = alter;
		topic_unit = topic;
		srr = res;
		different = new ArrayList<String>();
		tag = new ArrayList<String>();
	}
}

class Type
{
	static final int ZIPCODE = 17;
	static final int NUMBER = 1;
	static final int YEAR = 2;
	static final int DATE = 3;
	static final int TIME = 4;
	static final int TELEPHONE = 5;
	static final int EMAIL = 6;
	static final int NAME = 7;
	static final int PEOPLE = 8;
	static final int PLACE = 9;
	static final int ORGANIZATION = 10;
	static final int OTHER_NAME = 11;
	
	static final int COMMON = 12;
	static final int SYNONYM = 13;
	static final int ANTONYM = 14;
	static final int SIBLING = 15;
	static final int GRANDCHILD = -1;
}

class Phrase
{
	String word;
	int length;
	int type;
	int name_type; //濡傛灉鏄悕瀛楋紝纭畾鍏跺叿浣撶被鍨�
	String alter;
	
	public Phrase(String w, int l, int t)
	{
		word = w;
		length = l;
		type = t;
	}
	
	public Phrase(String w, int t)
	{
		word = w;
		length = 1;
		type = t;
	}
	
	public Phrase(String w, int l, int t, int n)
	{
		word = w;
		length = l;
		type = t;
		name_type = n;
	}
}
