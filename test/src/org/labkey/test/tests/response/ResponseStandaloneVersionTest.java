package org.labkey.test.tests.response;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Git;
import org.labkey.test.io.Grep;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

@Category({Git.class})
public class ResponseStandaloneVersionTest
{

    /**
     * 'labkeyVersion' defined in 'init.gradle' for standalone build should match version from standard build
     */
    @Test
    public void testVersionForStandaloneBuild() throws IOException
    {
        final File root = new File(TestFileUtils.getLabKeyRoot());
        final File rootProperties = new File(root, "gradle.properties");
        final File initGradle = new File(root, "server/modules/Response/init.gradle");
        final Pattern versionPattern = Pattern.compile("labkeyVersion ?=[^0-9]*([0-9]+\\.[0-9]+)");

        final String rootVersion = Grep.findMatch(rootProperties, versionPattern);
        Assert.assertNotNull("Unable to determine 'labkeyVersion' from " + rootProperties.getAbsolutePath(), rootVersion);

        final String standaloneVersion = Grep.findMatch(initGradle, versionPattern);
        Assert.assertNotNull("Unable to determine 'labkeyVersion' from " + initGradle.getAbsolutePath(), standaloneVersion);

        Assert.assertEquals("Version for standalone build doesn't equal root LabKey version", rootVersion, standaloneVersion);
    }

}
