package com.github.bogdanlivadariu.reporting.junit.builder;

import com.github.bogdanlivadariu.reporting.junit.helpers.Helpers;
import com.github.jknack.handlebars.Handlebars;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static com.github.bogdanlivadariu.reporting.junit.helpers.Constants.*;
import static org.junit.Assert.assertEquals;

public class HelperTest {
    private static Handlebars instance;

    @BeforeClass
    public static void setup() {
        Helpers a = new Helpers(new Handlebars());
        instance = a.registerHelpers();
    }

    @Test
    public void resolveTooltipTest() throws IOException {

        assertEquals("This test has Failed",
            instance.helper("resolve-tooltip")
                .apply("Failed", null));
    }

    @Test
    public void isCollapsedTest() throws IOException {
        String helperName = "is-collapsed";
        assertEquals("collapse",
            instance.helper(helperName).apply(PASSED, null));

        assertEquals("collapse in",
            instance.helper(helperName).apply(FAILED, null));

        assertEquals(null,
            instance.helper(helperName).apply(SKIPPED, null));

        assertEquals(null,
            instance.helper(helperName).apply(ERRORED, null));
    }

    @Test
    public void resolveTitleTest() throws IOException {
        String helperName = "resolve-title";
        assertEquals("This step has passed",
            instance.helper(helperName).apply(PASSED, null));

        assertEquals("This step has failed",
            instance.helper(helperName).apply(FAILED, null));

        assertEquals("This step has been skipped",
            instance.helper(helperName).apply(SKIPPED, null));

        assertEquals(null,
            instance.helper(helperName).apply(ERRORED, null));
    }

    @Test
    public void resultColorTest() throws IOException {
        String helperName = "result-color";
        assertEquals("success",
            instance.helper(helperName).apply(PASSED, null));

        assertEquals("danger",
            instance.helper(helperName).apply(FAILED, null));

        assertEquals("info",
            instance.helper(helperName).apply(SKIPPED, null));

        assertEquals("warning",
            instance.helper(helperName).apply(ERRORED, null));
    }

}
