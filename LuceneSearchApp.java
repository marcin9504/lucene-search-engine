/*
 * Skeleton class for the Lucene search program implementation
 */


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class LuceneSearchApp {

    public LuceneSearchApp() {

    }

    public static String indexPath;

    public void index(List<RssFeedDocument> docs) throws Exception {

        // implement the Lucene indexing here
        Path iPath = Paths.get(indexPath);
        Directory directory = FSDirectory.open(iPath);
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);

        for (RssFeedDocument doc : docs) {
            indexWriter.addDocument(indexDoc(doc));
        }
        indexWriter.close();
    }

    public static Document indexDoc(RssFeedDocument rssFeedDocument) throws Exception {

        Document doc = new Document();
        Field titleField = new TextField("title", rssFeedDocument.getTitle(), Field.Store.YES);
        doc.add(titleField);
        Field descriptionField = new TextField("description", rssFeedDocument.getDescription(), Field.Store.YES);
        doc.add(descriptionField);
        Field pubDateField = new LongField("pubDate", rssFeedDocument.getPubDate().getTime(), Field.Store.YES);
        doc.add(pubDateField);
//        System.out.println(doc);
        return doc;
    }

    public static IndexSearcher getIndexSearcher() throws IOException {
        Path path = Paths.get(indexPath);
        Directory directory = FSDirectory.open(path);
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        return indexSearcher;
    }

    public List<String> search(List<String> inTitle, List<String> notInTitle, List<String> inDescription, List<String> notInDescription, String startDate, String endDate) throws Exception {

        printQuery(inTitle, notInTitle, inDescription, notInDescription, startDate, endDate);

        // implement the Lucene search here

        IndexSearcher indexSearcher = getIndexSearcher();
//        Analyzer analyzer = new StandardAnalyzer();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        List<String> results = new LinkedList<String>();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD));
        if (inTitle != null)
            for (String t : inTitle)
                builder.add(new TermQuery(new Term("title", t)), BooleanClause.Occur.MUST);
        if (notInTitle != null)
            for (String t : notInTitle)
                builder.add(new TermQuery(new Term("title", t)), BooleanClause.Occur.MUST_NOT);
        if (inDescription != null)
            for (String t : inDescription)
                builder.add(new TermQuery(new Term("description", t)), BooleanClause.Occur.MUST);
        if (notInDescription != null)
            for (String t : notInDescription)
                builder.add(new TermQuery(new Term("description", t)), BooleanClause.Occur.MUST_NOT);
        //TODO fix dates
        if (startDate != null)
        {
            builder.add(NumericRangeQuery.newLongRange("pubDate", format.parse(startDate).getTime(), null,
                    true, true), BooleanClause.Occur.MUST);
        }
        if (endDate != null)
        {
            Calendar c = Calendar.getInstance();
            c.setTime(format.parse(endDate));
            c.add(Calendar.DATE, 1);
            String endDateCorr = format.format(c.getTime());
            builder.add(NumericRangeQuery.newLongRange("pubDate", null, format.parse(endDateCorr).getTime(),
                    true, true), BooleanClause.Occur.MUST);
        }
        BooleanQuery query = builder.build();

//       System.out.println(query.toString());


        TopDocs topDocs = indexSearcher.search(query, indexSearcher.count(query));
        ScoreDoc hits[] = topDocs.scoreDocs;

        for (ScoreDoc hit : hits) {
            Document doc = indexSearcher.doc(hit.doc);
            results.add(doc.get("title"));
        }

        return results;
    }

    public void printQuery(List<String> inTitle, List<String> notInTitle, List<String> inDescription, List<String> notInDescription, String startDate, String endDate) {
        System.out.print("Search (");
        if (inTitle != null) {
            System.out.print("in title: " + inTitle);
            if (notInTitle != null || inDescription != null || notInDescription != null || startDate != null || endDate != null)
                System.out.print("; ");
        }
        if (notInTitle != null) {
            System.out.print("not in title: " + notInTitle);
            if (inDescription != null || notInDescription != null || startDate != null || endDate != null)
                System.out.print("; ");
        }
        if (inDescription != null) {
            System.out.print("in description: " + inDescription);
            if (notInDescription != null || startDate != null || endDate != null)
                System.out.print("; ");
        }
        if (notInDescription != null) {
            System.out.print("not in description: " + notInDescription);
            if (startDate != null || endDate != null)
                System.out.print("; ");
        }
        if (startDate != null) {
            System.out.print("startDate: " + startDate);
            if (endDate != null)
                System.out.print("; ");
        }
        if (endDate != null)
            System.out.print("endDate: " + endDate);
        System.out.println("):");
    }

    public void printResults(List<String> results) {
        if (results.size() > 0) {
            Collections.sort(results);
            for (int i = 0; i < results.size(); i++)
                System.out.println(" " + (i + 1) + ". " + results.get(i));
        } else
            System.out.println(" no results");
    }

    public static void main(String[] args) {
        indexPath = "./output2";
        if (args.length > 0) {
            LuceneSearchApp engine = new LuceneSearchApp();

            RssFeedParser parser = new RssFeedParser();
            parser.parse(args[0]);
            List<RssFeedDocument> docs = parser.getDocuments();

            try {
                engine.index(docs);


                List<String> inTitle;
                List<String> notInTitle;
                List<String> inDescription;
                List<String> notInDescription;
                List<String> results;

                // 1) search documents with words "kim" and "korea" in the title
                inTitle = new LinkedList<String>();
                inTitle.add("kim");
                inTitle.add("korea");
                results = engine.search(inTitle, null, null, null, null, null);
                engine.printResults(results);

                // 2) search documents with word "kim" in the title and no word "korea" in the description
                inTitle = new LinkedList<String>();
                notInDescription = new LinkedList<String>();
                inTitle.add("kim");
                notInDescription.add("korea");
                results = engine.search(inTitle, null, null, notInDescription, null, null);
                engine.printResults(results);

                // 3) search documents with word "us" in the title, no word "dawn" in the title and word "" and "" in the description
                inTitle = new LinkedList<String>();
                inTitle.add("us");
                notInTitle = new LinkedList<String>();
                notInTitle.add("dawn");
                inDescription = new LinkedList<String>();
                inDescription.add("american");
                inDescription.add("confession");
                results = engine.search(inTitle, notInTitle, inDescription, null, null, null);
                engine.printResults(results);

                // 4) search documents whose publication date is 2011-12-18
                results = engine.search(null, null, null, null, "2011-12-18", "2011-12-18");
                engine.printResults(results);

                // 5) search documents with word "video" in the title whose publication date is 2000-01-01 or later
                inTitle = new LinkedList<String>();
                inTitle.add("video");
                results = engine.search(inTitle, null, null, null, "2000-01-01", null);
                engine.printResults(results);

                // 6) search documents with no word "canada" or "iraq" or "israel" in the description whose publication date is 2011-12-18 or earlier
                notInDescription = new LinkedList<String>();
                notInDescription.add("canada");
                notInDescription.add("iraq");
                notInDescription.add("israel");
                results = engine.search(null, null, null, notInDescription, null, "2011-12-18");
                engine.printResults(results);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else
            System.out.println("ERROR: the path of a RSS Feed file has to be passed as a command line argument.");
    }
}