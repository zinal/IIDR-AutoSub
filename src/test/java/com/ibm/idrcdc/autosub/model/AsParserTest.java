/*
 * AutoSub sample code
 */
package com.ibm.idrcdc.autosub.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.Assert;
import com.ibm.idrcdc.autosub.config.*;

/**
 *
 * @author zinal
 */
public class AsParserTest {

    private static final String XML_CONFIG =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
            + "<idrcdc-autosub>\n"
            + "  <idrcdc-subscription name='SUB1' source='SRC1' target='DST1'>\n"
            + "  </idrcdc-subscription>\n"
            + "  <idrcdc-subscription name='SUB2' source='SRC1' target='DST1' skipNewBlobs='true'>\n"
            + "  </idrcdc-subscription>\n"
            + "  <idrcdc-subscription name='SUB3' source='SRC1' target='DST1' skipNewBlobs='false'>\n"
            + "  </idrcdc-subscription>\n"
            + "  <idrcdc-engine name='SRC1' mode='Source'>\n"
            + "    <cdc-rsh>ssh cdcuser@host1</cdc-rsh>\n"
            + "    <cdc-path>/datum/sw/cdc-oracle</cdc-path>\n"
            + "    <cdc-instance>wrk1</cdc-instance>\n"
            + "  </idrcdc-engine>\n"
            + "  <idrcdc-engine name='DST1' mode='Target'>\n"
            + "    <cdc-rsh>ssh cdcuser@host2</cdc-rsh>\n"
            + "    <cdc-path>/datum/sw/cdc-ds</cdc-path>\n"
            + "    <cdc-instance>DS</cdc-instance>\n"
            + "  </idrcdc-engine>\n"
            + "</idrcdc-autosub>";

    public AsParserTest() {
    }

    private InputStream getInputXml1() {
        return new ByteArrayInputStream(XML_CONFIG.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void test1() {
        AsConfig config1 = AsParser.load(getInputXml1());
        Assert.assertEquals(2, config1.getEngines().size());
        Assert.assertEquals(3, config1.getSubscriptions().size());
        Assert.assertEquals(false, config1.getSubscriptions().get(0).isSkipNewBlobs());
        Assert.assertEquals(true,  config1.getSubscriptions().get(1).isSkipNewBlobs());
        Assert.assertEquals(false, config1.getSubscriptions().get(2).isSkipNewBlobs());
        Assert.assertNotNull(config1.getSubscriptions().get(0).getSource());
        Assert.assertNotNull(config1.getSubscriptions().get(0).getTarget());
        Assert.assertNotEquals(config1.getSubscriptions().get(0).getSource(),
                config1.getSubscriptions().get(0).getTarget());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AsParser.save(config1, baos);
        AsConfig config2 = AsParser.load(new ByteArrayInputStream(baos.toByteArray()));
        Assert.assertTrue(config1 != config2);
        Assert.assertEquals(config1, config2);
    }

}
