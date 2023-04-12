package com.github.bogdanlivadariu.reporting.rspec.builder;

import com.github.bogdanlivadariu.reporting.rspec.helpers.Constants;
import com.github.bogdanlivadariu.reporting.rspec.helpers.Helpers;
import com.github.bogdanlivadariu.reporting.rspec.xml.models.TestSuiteModel;
import com.github.bogdanlivadariu.reporting.rspec.xml.models.TestSuitesModel;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.io.FileUtils;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class RSpecReportBuilder {
    public static final String SUITES_OVERVIEW = "testSuitesOverview.html";

    private final String TEST_OVERVIEW_PATH;

    private final String TEST_SUMMARY_PATH;

    private String TEST_SUMMARY_REPORT = "rspec-reporting/testCaseSummaryReport";

    private String TEST_OVERVIEW_REPORT = "rspec-reporting/testOverviewReport";

    private List<TestSuiteModel> processedTestSuites;

    public RSpecReportBuilder(List<String> xmlReports, String targetBuildPath) throws FileNotFoundException,
            JAXBException {
        TEST_OVERVIEW_PATH = targetBuildPath + "/";
        TEST_SUMMARY_PATH = targetBuildPath + "/test-summary/";
        processedTestSuites = new ArrayList<>();

        JAXBContext cntx = JAXBContext.newInstance(TestSuitesModel.class);

        Unmarshaller unm = cntx.createUnmarshaller();

        for (String xml : xmlReports) {

            Logger.getGlobal().info(">>>>>>>>>>" + xml);
            TestSuitesModel ts = (TestSuitesModel) unm.unmarshal(new File(xml));

            ts.postProcess();

            processedTestSuites.addAll(ts.getTestsuites());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends List<?>> T cast(Object obj) {
        return (T) obj;
    }

    private void writeTestOverviewReport() throws IOException {
        Template template = new Helpers(new Handlebars()).registerHelpers().compile(TEST_OVERVIEW_REPORT);
        AllRSpecJUnitReports allFeatures = new AllRSpecJUnitReports("Test suites overview", processedTestSuites);
        FileUtils.writeStringToFile(new File(TEST_OVERVIEW_PATH + SUITES_OVERVIEW),
            template.apply(allFeatures), StandardCharsets.UTF_8);
    }

    private void writeTestCaseSummaryReport() throws IOException {
        Template template = new Helpers(new Handlebars()).registerHelpers().compile(TEST_SUMMARY_REPORT);
        for (TestSuiteModel ts : processedTestSuites) {
            String content = template.apply(ts);
            FileUtils.writeStringToFile(new File(TEST_SUMMARY_PATH + ts.getUniqueID() + ".html"),
                content, StandardCharsets.UTF_8);
        }
    }

    private void writeTestsPassedReport() throws IOException {
        Template template = new Helpers(new Handlebars()).registerHelpers().compile(TEST_OVERVIEW_REPORT);

        List<TestSuiteModel> onlyPassed = new ArrayList<>(processedTestSuites);

        onlyPassed.removeIf(f -> f.getOverallStatus().equalsIgnoreCase(Constants.FAILED));

        AllRSpecJUnitReports allTestSuites = new AllRSpecJUnitReports("Passed test suites report", onlyPassed);
        FileUtils.writeStringToFile(new File(TEST_OVERVIEW_PATH + "testsPassed.html"),
            template.apply(allTestSuites), StandardCharsets.UTF_8);
    }

    private void writeTestsFailedReport() throws IOException {
        Template template = new Helpers(new Handlebars()).registerHelpers().compile(TEST_OVERVIEW_REPORT);

        List<TestSuiteModel> onlyFailed = new ArrayList<>(processedTestSuites);

        onlyFailed.removeIf(f -> f.getOverallStatus().equalsIgnoreCase(Constants.PASSED));

        AllRSpecJUnitReports allTestSuites = new AllRSpecJUnitReports("Failed test suites report", onlyFailed);
        FileUtils.writeStringToFile(new File(TEST_OVERVIEW_PATH + "testsFailed.html"),
            template.apply(allTestSuites), StandardCharsets.UTF_8);
    }

    public boolean writeReportsOnDisk() throws IOException {
        writeTestOverviewReport();
        writeTestCaseSummaryReport();
        writeTestsPassedReport();
        writeTestsFailedReport();
        for (TestSuiteModel ts : processedTestSuites) {
            if (Integer.parseInt(ts.getFailures()) >= 1
                || Integer.parseInt(ts.getErrors()) >= 1
                || Integer.parseInt(ts.getSkipped()) >= 1
                || Integer.parseInt(ts.getTests()) < 1) {
                return false;
            }
        }
        return processedTestSuites.size() > 0;
    }
}
