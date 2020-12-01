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

/**
 *
 * @author zinal
 */
public class AsConfigTest {

    private static final String XML_CONFIG =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
            + "<idrcdc-autosub>\n"
            + "  <idrcdc-subscription name='SUB1' source='SRC1' target='DST1'>\n"
            + "  </idrcdc-subscription>\n"
            + "  <idrcdc-subscription name='SUB2' source='SRC1' target='DST1' skipNewBlobs='true'>\n"
            + "  </idrcdc-subscription>\n"
            + "  <idrcdc-subscription name='SUB3' source='SRC1' target='DST1' skipNewBlobs='false'>\n"
            + "  </idrcdc-subscription>\n"
            + "  <idrcdc-engine name='SRC1' type='Source'>\n"
            + "    <cmd-clear>ssh cdcuser@src1-host autosub-clear.sh SRC1</cmd-clear>\n"
            + "    <cmd-bmk-put>ssh cdcuser@src1-host autosub-bmk-put.sh SRC1 ${SUBSCRIPTION} ${BOOKMARK}</cmd-bmk-put>\n"
            + "  </idrcdc-engine>\n"
            + "  <idrcdc-engine name='DST1' type='Target'>\n"
            + "    <cmd-bmk-get>ssh cdcuser@dst1-host autosub-bmk-get.sh DST1 ${SUBSCRIPTION}</cmd-bmk-get>\n"
            + "  </idrcdc-engine>\n"
            + "</idrcdc-autosub>";

    public AsConfigTest() {
    }

    private InputStream getInputXml1() {
        return new ByteArrayInputStream(XML_CONFIG.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void test1() {
        AsConfig config1 = AsConfig.load(getInputXml1());
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
        AsConfig.save(config1, baos);
        AsConfig config2 = AsConfig.load(new ByteArrayInputStream(baos.toByteArray()));
        Assert.assertTrue(config1 != config2);
        Assert.assertEquals(config1, config2);
    }

}