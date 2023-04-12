package com.github.bogdanlivadariu.reporting.cucumber.builder;

import com.github.bogdanlivadariu.reporting.cucumber.helpers.Constants;
import com.github.bogdanlivadariu.reporting.cucumber.helpers.Helpers;
import com.github.bogdanlivadariu.reporting.cucumber.helpers.SpecialProperties;
import com.github.bogdanlivadariu.reporting.cucumber.json.models.Feature;
import com.github.bogdanlivadariu.reporting.cucumber.json.models.Tag;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

import static com.github.bogdanlivadariu.reporting.cucumber.helpers.Constants.*;

public class CucumberReportBuilder {

    public static final String FEATURES_OVERVIEW_HTML = "featuresOverview.html";

    private static final String FEATURES_PASSED_HTML = "featuresPassed.html";

    private static final String FEATURES_FAILED_HTML = "featuresFailed.html";

    private static final String FEATURE_SUMMARY_REPORT = "cucumber-reporting/featureSummaryReport";

    private static final String FEATURE_OVERVIEW_REPORT = "cucumber-reporting/featureOverviewReport";

    private final String FEATURE_TAG_REPORT;

    private final String REPORTS_SUMMARY_PATH;

    private final String REPORTS_OVERVIEW_PATH;

    private Gson gs = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private Handlebars bars = new Helpers(new Handlebars()).registerHelpers();

    private List<Feature> processedFeatures = null;

    public CucumberReportBuilder(List<String> jsonReports, String targetBuildPath, SpecialProperties props)
        throws IOException {
        REPORTS_SUMMARY_PATH = targetBuildPath + "/feature-reports/";

        REPORTS_OVERVIEW_PATH = targetBuildPath + "/";
        FEATURE_TAG_REPORT = targetBuildPath + "/tag-reports/";
        processedFeatures = prepareData(jsonReports, props);
    }

    public List<Feature> getProcessedFeatures() {
        return processedFeatures;
    }

    private void writeFeatureSummaryReports() throws IOException {
        Template template = bars.compile(FEATURE_SUMMARY_REPORT);
        for (Feature feature : getProcessedFeatures()) {
            String generatedFeatureHtmlContent = template.apply(feature);
            // generatedFeatureSummaryReports.put(feature.getUniqueID(), generatedFeatureHtmlContent);
            FileUtils.writeStringToFile(new File(REPORTS_SUMMARY_PATH + feature.getUniqueID() + ".html"),
                generatedFeatureHtmlContent, StandardCharsets.UTF_8);
        }
    }

    private void writeFeatureOverviewReport() throws IOException {
        Template template = bars.compile(FEATURE_OVERVIEW_REPORT);
        AllFeatureReports allFeatures = new AllFeatureReports(FEATURES_OVERVIEW, getProcessedFeatures());
        FileUtils.writeStringToFile(new File(REPORTS_OVERVIEW_PATH + FEATURES_OVERVIEW_HTML),
            template.apply(allFeatures), StandardCharsets.UTF_8);
    }

    private void writeFeaturePassedReport() throws IOException {
        Template template = bars.compile(FEATURE_OVERVIEW_REPORT);

        List<Feature> onlyPassed = new ArrayList<>(getProcessedFeatures());
        onlyPassed.removeIf(f -> f.getOverallStatus().equalsIgnoreCase(Constants.FAILED));

        AllFeatureReports allFeatures = new AllFeatureReports(FEATURES_PASSED_OVERVIEW, onlyPassed);
        FileUtils.writeStringToFile(new File(REPORTS_OVERVIEW_PATH + FEATURES_PASSED_HTML),
            template.apply(allFeatures), StandardCharsets.UTF_8);
    }

    private void writeFeatureFailedReport() throws IOException {
        Template template = bars.compile(FEATURE_OVERVIEW_REPORT);

        List<Feature> onlyFailed = new ArrayList<>(getProcessedFeatures());
        onlyFailed.removeIf(f -> f.getOverallStatus().equalsIgnoreCase(Constants.PASSED));
        AllFeatureReports allFeatures = new AllFeatureReports(FEATURES_FAILED_OVERVIEW, onlyFailed);
        FileUtils.writeStringToFile(new File(REPORTS_OVERVIEW_PATH + FEATURES_FAILED_HTML),
            template.apply(allFeatures), StandardCharsets.UTF_8);
    }

    private void writeFeatureTagsReport() throws IOException {
        LinkedHashMap<String, List<Feature>> allTags = new LinkedHashMap<>();
        for (Feature feature : getProcessedFeatures()) {
            // move the outputFileLocation one folder up for all the features that will be built
            feature.setOutputFileLocation("../" + feature.getOutputFileLocation());
            // put the tags in the proper place
            if (feature.getTags().length < 1) {
                if (allTags.containsKey(Constants.UNTAGGED)) {
                    allTags.get(Constants.UNTAGGED).add(feature);
                } else {
                    List<Feature> tagFeatures = new ArrayList<Feature>();
                    tagFeatures.add(feature);
                    allTags.put(Constants.UNTAGGED, tagFeatures);
                }
                continue;
            }
            for (Tag tag : feature.getTags()) {
                String tagName = tag.getName();
                if (allTags.containsKey(tagName)) {
                    allTags.get(tagName).add(feature);
                } else {
                    List<Feature> tagFeatures = new ArrayList<Feature>();
                    tagFeatures.add(feature);
                    allTags.put(tagName, tagFeatures);
                }
            }
            Template template = bars.compile(FEATURE_OVERVIEW_REPORT);

            for (Entry<String, List<Feature>> entry : allTags.entrySet()) {
                AllFeatureReports specificTagFeatures =
                    new AllFeatureReports(entry.getKey(), entry.getValue());
                FileUtils.writeStringToFile(new File(FEATURE_TAG_REPORT + entry.getKey() + ".html"),
                    template.apply(specificTagFeatures), StandardCharsets.UTF_8);
            }
        }
    }

    private List<Feature> prepareData(List<String> jsonReports, SpecialProperties props) throws IOException {
        Comparator<Feature> featureNameComparator = (first, second) -> first.getName().compareToIgnoreCase(second.getName());

        List<Feature> processedFeaturesLocal = new ArrayList<>();
        for (String jsonReport : jsonReports) {
            File jsonFileReport = new File(jsonReport);
            FileInputStream fis = new FileInputStream(jsonFileReport);
            String gson = IOUtils.toString(fis, StandardCharsets.UTF_8);
            fis.close();

            if (gson.isEmpty()) {
                continue;
            }
            Feature[] features = gs.fromJson(gson, Feature[].class);

            for (Feature feature : features) {
                processedFeaturesLocal.add(feature.postProcess(props));
            }

        }
        processedFeaturesLocal.sort(featureNameComparator);
        return processedFeaturesLocal;
    }

    /**
     * @return true if all the features have passed, false if at least one has failed.
     * @throws IOException if the reports cannot be written on disk
     */
    public boolean writeReportsOnDisk() throws IOException {
        writeFeatureSummaryReports();
        writeFeatureOverviewReport();
        writeFeaturePassedReport();
        writeFeatureFailedReport();
        writeFeatureTagsReport();
        for (Feature feature : getProcessedFeatures()) {
            if (feature.getOverallStatus().equalsIgnoreCase(Constants.FAILED)) {
                return false;
            }
        }
        return getProcessedFeatures().size() > 0;
    }
}
