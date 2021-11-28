import java.io.IOException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;

public class TermSearchFromII {	
	public static class SearchMapper
	extends Mapper<Text, Text, Text, IntWritable> {
		public void map(Text term, Text entries, Context context)
		throws IOException, InterruptedException {
			
			// Get the search term from the command line arguments
			Configuration conf = context.getConfiguration();
			String search_term = conf.get("term");
			
			// Do not consider case when comparing strings
			search_term = search_term.toLowerCase();
			String input_term = term.toString().toLowerCase();
			
			// Only output the inverted index row for the search term
			if(input_term.equals(search_term)) {
				
				// Extract the information (doc_id, frequency) from the inverted index
				String[] ii_entries = entries.toString().split("<<");
				for(String ii_entry: ii_entries) {
					String[] docid_freq = ii_entry.split(">");
					if(docid_freq.length < 2) {
						continue;
					}
					String doc_id = docid_freq[0];
					Integer frequency = Integer.parseInt(docid_freq[1]);
					
					// Write each document id and it's corresponding frequency as output
					context.write(new Text(doc_id), new IntWritable(frequency)); 
				}
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		// Ensure arguments are valid
		if (args.length != 3) {
			System.err.println("Usage: TermSearch <input path> <output path> <search term>");
			System.exit(-1);
		}
		
		Configuration conf = new Configuration();
		
		// Pass the term argument to the MapReduce functions
		conf.set("term", args[2]);
		
		Job job = Job.getInstance(conf, "Term Search");
		job.setJarByClass(TermSearchFromII.class);
		job.setMapperClass(SearchMapper.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		
		// Only use 1 reducer for Term Search
		job.setNumReduceTasks(1);
		
		// Use KeyValueText Input Format because the input is the output of another MapReduce task
		job.setInputFormatClass(KeyValueTextInputFormat.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}