package org.benchy.repository;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import org.benchy.BenchmarkResult;
import org.benchy.TestCaseResult;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * @author Peter Veentjer
 */
public class GoogleSpreadsheetRepository implements BenchmarkResultRepository {
    private SpreadsheetService spreadsheetService;
    private SpreadsheetEntry benchmarkSheetEntry;

    public GoogleSpreadsheetRepository(String loginname, String password) {
        try {
            spreadsheetService = new SpreadsheetService("exampleCo-exampleApp-1");
            spreadsheetService.setUserCredentials(loginname, password);

            //URL metafeedUrl = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full");
            //SpreadsheetFeed feed = spreadsheetService.getFeed(metafeedUrl, SpreadsheetFeed.class);

            System.out.println("-----downloading--------------");
            URL metafeedUrl = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full");
            SpreadsheetFeed feed = spreadsheetService.getFeed(metafeedUrl, SpreadsheetFeed.class);
            List<SpreadsheetEntry> spreadsheets = feed.getEntries();
            for (int i = 0; i < spreadsheets.size(); i++) {
                SpreadsheetEntry entry = spreadsheets.get(i);
                if (entry.getTitle().getPlainText().equals("benchmarks")) {
                    benchmarkSheetEntry = entry;
                }
                System.out.println("\t" + entry.getTitle().getPlainText());
            }
            System.out.println("-----printing spreadsheets--------------");

        } catch (AuthenticationException e) {
            throw new RuntimeException(e);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BenchmarkResult load(Date date, String benchmarkName) {
        throw new RuntimeException();
    }

    @Override
    public BenchmarkResult loadLast(Date date, String benchmarkName) {
        throw new RuntimeException();
    }

    @Override
    public BenchmarkResult loadLast(String benchmark) {
        throw new RuntimeException();
    }

    @Override
    public void store(BenchmarkResult result) {
        try {
            WorksheetEntry worksheet = find(result.getBenchmarkName());

            URL listfeedURL = worksheet.getListFeedUrl();

            System.out.println("listfeedUrl:" + listfeedURL);
            System.out.println("-------- begin print----------------------------");
            ListFeed feed = spreadsheetService.getFeed(listfeedURL, ListFeed.class);
            for (ListEntry entry : feed.getEntries()) {
                System.out.println(entry.getTitle().getPlainText());
                for (String tag : entry.getCustomElements().getTags()) {
                    System.out.println("  " + entry.getCustomElements().getValue(tag) + "");
                }
            }
            System.out.println("----------end print-----------------------------");


            for (TestCaseResult testCaseResult : result.getTestCaseResults()) {
                ListEntry newEntry = new ListEntry();

                // Split first by the commas between the different fields.
                for (Object key : testCaseResult.getProperties().keySet()) {
                    String tag = "foo";//(String) key;
                    String value = "bar";//estCaseResult.get(tag);                    
                    newEntry.getCustomElements().setValueLocal(tag, value);
                }
                System.out.println("adding");
                ListEntry insertedRow = spreadsheetService.insert(listfeedURL, newEntry);
            }


            // worksheet.

            //for(TestCaseResult testCaseResult: result.getTestCaseResults()){
            //    worksheet.get
            //}


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private WorksheetEntry find(String benchmarkName) throws Exception {
        URL worksheetFeedUrl = benchmarkSheetEntry.getWorksheetFeedUrl();
        WorksheetFeed worksheetFeed = spreadsheetService.getFeed(worksheetFeedUrl,
                WorksheetFeed.class);

        for (WorksheetEntry worksheet : worksheetFeed.getEntries()) {
            String currTitle = worksheet.getTitle().getPlainText();
            if (currTitle.equals(benchmarkName)) {
                return worksheet;
            }
        }

        return null;
    }

    private WorksheetEntry create(String benchmarkName) throws Exception {


        WorksheetEntry worksheet = new WorksheetEntry();
        worksheet.setTitle(new PlainTextConstruct(benchmarkName));
        worksheet.setRowCount(200);
        worksheet.setColCount(30);

        URL worksheetFeedUrl = benchmarkSheetEntry.getWorksheetFeedUrl();
        spreadsheetService.insert(worksheetFeedUrl, worksheet);

        return worksheet;
    }

    private WorksheetEntry getOrCreateWorkSheet(String x) {
        return null;
    }
}
