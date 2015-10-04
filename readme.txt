In dataSet.txt, it is the data set used in this paper. We transform them to the specific form and save them as two files "data.txt" and "data group identification.txt" for our program in the path "GroupVerify-ICDM/data".

In the following, we introduce some details about our program.
First, execute TestAll.java, it will find missing terms. Second, execute LatentConditionPosition.java, it will find a proper position for a missing term. When we execute TestAll.java, we need to ensure that the path "data/missingconditioncandidates" is empty and the files "group.txt" and  "combinedgroup.txt" do not exist under the path "data/". When we execute LatentConditionPosition.java, we need to ensure that the file "position.txt" do not exist under the path "data/". The needed search results from Yahoo have been downloaded and placed in the path "data/source_pages". We filter out tags in these source page and place the extracted contents in the path "data/extracted_results". So collecting search results from the search engine does not execute. If you want to do it, we detail the related configures in the following. 

In Search.java:
The variable num is used for setting the number of pages returned by the search engine. In each page, it contains ten results including titles and snippets.
The variable result_num controls the number of search results obtained.
The variables download_enable, extract_enable and write_result are respectively used for controlling search result pages downloading, titles and snippets extraction and extracted data saving.
The variable source_path records the path storing download pages.
The variable result_path records the path storing extracted contents from the download pages which filters out html tags.

In TestAll.java:
The variable top_k is used for setting the number of terms extracted from query results according to occurrence frequency.

In MyAtom.java:
The variable topk is the same as above variable top_k.
The variable weight records weights in Missing Term Possibility (MTP) computation of terms, which can be obtained by a genetic algorithm. 
In the function getGroup(), "if(true)" represents that we consider the Freebase and it can be modified to "if(false)" when we do not consider the Freebase. If we consider the Freebase, "if(min>14||min==-1)" means the word distance should not exceed 14. If we just consider whether a missing term candidate appear in the description of the corresponding entities in Freebase, we modify "if(min>14||min==-1)" to "if(min==-1)".
In the function findRelatedSubset, the variable dir records the path of the program WordNet 2.1 and the variable similarity_thresold is the threshold in the method CMTGS.  
In the function selectTopKWords, the variable missingcondition records the path of top-k high frequent terms.

The weights mentioned in Section 5.1.2 are set in MyAtom.java, which is obtained by generic algorithm in Gene.java. As we just get the top-1 group in the methods MTGS and CMTGS, in the function selectBestGroup in MyAtom.java, we directly use the variable best to record the group with the highest score. All Freebase and related Wikipedia pages have been downloaded and saved in the path "data/identify". Based on them, we compute word distances for missing term mining with the Knowledge Base in Section 5.1.4 and save them in the path "data/freebasedistance.txt".

