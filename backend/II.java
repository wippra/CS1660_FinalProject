import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class II {
	public static class IIMap extends Mapper<LongWritable, Text, Text, Text>{
		protected void map(LongWritable key, Text value,
				Context context) throws IOException ,InterruptedException {
			
			// Stopwords from NLTK and slide from lecture
			String[] stopwords_array = {"i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "you're", "you've", "you'll", "you'd", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "she's", "her", "hers", "herself", "it", "it's", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "that'll", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "don't", "should", "should've", "now", "d", "ll", "m", "o", "re", "ve", "y", "ain", "aren", "aren't", "couldn", "couldn't", "didn", "didn't", "doesn", "doesn't", "hadn", "hadn't", "hasn", "hasn't", "haven", "haven't", "isn", "isn't", "ma", "mightn", "mightn't", "mustn", "mustn't", "needn", "needn't", "shan", "shan't", "shouldn", "shouldn't", "wasn", "wasn't", "weren", "weren't", "won", "won't", "wouldn", "wouldn't", "around", "one", "every"};
			HashSet<String> stopwords = new HashSet<String>(Arrays.asList(stopwords_array));
			
			// Go through the input file
			StringTokenizer tokenizer = new StringTokenizer(value.toString());
			while(tokenizer.hasMoreTokens()) {
				// Do not consider case when comparing strings
				String word = tokenizer.nextToken();
				word = word.toLowerCase();
				
				// Ignore stopwords 
				if(stopwords.contains(word)) {
					continue;
				}
				
				// Emit the term and its source file to reducer for counting
				Text term = new Text(word);
				String fileName = ((FileSplit) context.getInputSplit()).getPath().toString();
				Text payload = new Text(fileName);
				context.write(term, payload);
			}				
		}
	}
	
	public static class IIReduce extends Reducer<Text, Text, Text, Text>{
		protected void reduce(Text term, Iterable<Text> payloads,
				Context context) throws java.io.IOException ,InterruptedException {
				
			// Create an inverted index of terms to list of <doc id, frequency in doc>
			HashMap<String, HashMap<String, Integer>> inverted_index = new HashMap<String, HashMap<String, Integer>>();
									
			for (Text payload : payloads) {
				String word = term.toString();
				String doc_id = payload.toString();
				if(inverted_index.containsKey(word)){
					// Need to update the existing index for this term
					HashMap<String, Integer> doc_freq = inverted_index.get(word);
					
					// Add one to the frequency of this term in doc_id
					if(doc_freq.containsKey(doc_id)) {
						doc_freq.replace( doc_id, doc_freq.get(doc_id) + 1);
					} else {
						doc_freq.put(doc_id, 1);
					}
					inverted_index.replace(doc_id, doc_freq);
				} else {
					// Need to create the entry for this term
					HashMap<String, Integer> doc_freq = new HashMap<String, Integer>();
					doc_freq.put(doc_id, 1);
					inverted_index.put(word, doc_freq);
				}
			}
			
			// After index has been fully created, format them for output
			
			for (HashMap.Entry<String, HashMap<String, Integer>> term_entry : inverted_index.entrySet()) {
				String word = term_entry.getKey();
				HashMap<String, Integer> doc_freq = term_entry.getValue();
				
				// Create the formatted input from each entry
				StringBuilder term_index = new StringBuilder();	
				for (HashMap.Entry<String, Integer> index_entry : doc_freq.entrySet()) {
					Integer frequency = index_entry.getValue();
					
					// Format each entry as: "DOC_PATH > FREQUENCY <<" for easy processing
					term_index.append(index_entry.getKey().toString() + ">" + frequency.toString() + "<<");
				}
				Text payload = new Text(term_index.toString());
				context.write(new Text(word), payload);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: Inverted Index <input path> <output path>");
			System.exit(-1);
		}
		
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "Inverted Index");
		job.setJarByClass(II.class);
		job.setMapperClass(IIMap.class);
		job.setReducerClass(IIReduce.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		FileInputFormat.setInputDirRecursive(job, true);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
