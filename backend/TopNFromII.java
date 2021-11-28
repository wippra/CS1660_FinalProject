import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;

public class TopNFromII {	
	public static class TopNMapper
	extends Mapper<Text, Text, Text, IntWritable> {
		private HashMap<String, Integer> term_freq;
		
		@Override
		public void setup(Context context) throws IOException, InterruptedException
		{
			term_freq = new HashMap<String, Integer>();
		}
		
		public void map(Text term, Text entries, Context context)
		throws IOException, InterruptedException {
			String word = term.toString();
			
			// Get a list of documents and frequencies for each term
			String[] doc_freqs = entries.toString().split("<<");
			
			// Get the total frequency for all documents
			for(String entry: doc_freqs) {
				String[] docid_freq = entry.split(">");
				if(docid_freq.length < 2) {
					continue;
				}
				Integer doc_freq = Integer.parseInt(docid_freq[1]);
				if(term_freq.containsKey(word)) {
					term_freq.replace( word, term_freq.get(word) + doc_freq);
				} else {
					term_freq.put(word, doc_freq);
				}
			}
		}
		
		@Override
		public void cleanup(Context context) throws IOException, InterruptedException
		{
			// Get N from the command line arguments
			Configuration conf = context.getConfiguration();
			Integer N = Integer.parseInt(conf.get("N"));
			
			// Add all values to an ArrayList for sorting
			ArrayList<Entry<String, Integer>> term_freqs = new ArrayList<Entry<String, Integer>>();
			for (HashMap.Entry<String, Integer> term_f: term_freq.entrySet()) {
				term_freqs.add(new java.util.AbstractMap.SimpleEntry<>(term_f.getKey(), term_f.getValue()));
			}
			
			// Sort all the values received by the mapper
			Collections.sort(term_freqs, new Comparator<Map.Entry<String, Integer> >() {
				public int compare(Map.Entry<String, Integer> o1,
								   Map.Entry<String, Integer> o2)
				{
					return (o2.getValue()).compareTo(o1.getValue());
				}
			});
			
			// Output the Top N results (or all if terms < N)
			int i = 0;
			for (Entry<String, Integer> term_f: term_freqs) {
				context.write(new Text(term_f.getKey()), new IntWritable(term_f.getValue()));
				i++;
				if(i >= N) {
					break;
				}
			}
		}
		
	}
	
	public static class TopNReducer
	extends Reducer<Text, IntWritable, Text, IntWritable> {
		private ArrayList<Entry<String, Integer>> term_freqs;
		
		// Since we can't determine top-n until we have seen all values, 
		//  create an array to store values until we can sort them at the end
		@Override
		public void setup(Context context) throws IOException, InterruptedException
		{
			term_freqs = new ArrayList<Entry<String, Integer>>();
		}
		
		public void reduce(Text key, Iterable<IntWritable> values, Context context) 
		throws IOException, InterruptedException {
			
			for(IntWritable freq : values) {
				term_freqs.add(new java.util.AbstractMap.SimpleEntry<>(key.toString(), freq.get()));
			}
		}
	
		// After we've received every value, then top-n can be determined
		@Override
		public void cleanup(Context context) throws IOException, InterruptedException
		{
			
			// Get N from the command line arguments
			Configuration conf = context.getConfiguration();
			Integer N = Integer.parseInt(conf.get("N"));
			
			// Sort all the values received by the reducer
			Collections.sort(term_freqs, new Comparator<Map.Entry<String, Integer> >() {
				public int compare(Map.Entry<String, Integer> o1,
								   Map.Entry<String, Integer> o2)
				{
					return (o2.getValue()).compareTo(o1.getValue());
				}
			});
			
			// Output the Top N results (or all if terms < N)
			int i = 0;
			for (Entry<String, Integer> term_f: term_freqs) {
				context.write(new Text(term_f.getKey()), new IntWritable(term_f.getValue()));
				i++;
				if(i >= N) {
					break;
				}
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		// Ensure arguments are valid
		if (args.length != 3) {
			System.err.println("Usage: TopN <input path> <output path> <N>");
			System.exit(-1);
		}
		
		Configuration conf = new Configuration();
		
		// Pass the N argument to the MapReduce functions
		conf.set("N", args[2]);
		
		Job job = Job.getInstance(conf, "Top N");
		job.setJarByClass(TopNFromII.class);
		job.setMapperClass(TopNMapper.class);
		job.setReducerClass(TopNReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		
		// Only use 1 reducer for Top N
		job.setNumReduceTasks(1);
		
		// Use KeyValueText Input Format because the input is the output of another MapReduce task
		job.setInputFormatClass(KeyValueTextInputFormat.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}

