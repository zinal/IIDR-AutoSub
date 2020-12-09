/*
** ****************************************************************************
** (c) Copyright IBM Corp. 2020 All rights reserved.
**
** The following sample of source code ("Sample") is owned by International
** Business Machines Corporation or one of its subsidiaries ("IBM") and is
** copyrighted and licensed, not sold. You may use, copy, modify, and
** distribute the Sample in any form without payment to IBM.
**
** The Sample code is provided to you on an "AS IS" basis, without warranty of
** any kind. IBM HEREBY EXPRESSLY DISCLAIMS ALL WARRANTIES, EITHER EXPRESS OR
** IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
** MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. Some jurisdictions do
** not allow for the exclusion or limitation of implied warranties, so the above
** limitations or exclusions may not apply to you. IBM shall not be liable for
** any damages you suffer as a result of using, copying, modifying or
** distributing the Sample, even if IBM has been advised of the possibility of
** such damages.
**
** Author:   Maksim Zinal <mzinal@ru.ibm.com>
 */
package com.ibm.idrcdc.autosub.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jdom2.CDATA;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.apache.commons.lang3.StringUtils;

/**
 * Monitoring configuration.
 * @author zinal
 */
public class AsConfig {

    public static final String EL_ROOT = "idrcdc-autosub";
    public static final String EL_ENGINE = "idrcdc-engine";
    public static final String EL_SUB = "idrcdc-subscription";
    public static final String EL_ENG_RSH = "cdc-rsh";
    public static final String EL_ENG_INST = "cdc-instance";
    public static final String EL_ENG_PATH = "cdc-path";
    public static final String EL_CMD_VERSION = "cmd-version";
    public static final String EL_CMD_EVENTS = "cmd-events";
    public static final String EL_CMD_CLEAR = "cmd-clear";
    public static final String EL_CMD_BMK_GET = "cmd-bmk-get";
    public static final String EL_CMD_BMK_PUT = "cmd-bmk-put";

    private final Map<String, AsEngine> engines = new HashMap<>();
    private final List<AsSubscription> subscriptions = new ArrayList<>();

    public Map<String, AsEngine> getEngines() {
        return engines;
    }

    public List<AsSubscription> getSubscriptions() {
        return subscriptions;
    }

    public AsEngine getEngine(String name) {
        return engines.get(name);
    }

    public void addEngine(AsEngine ae) {
        engines.put(ae.getName(), ae);
    }

    public void addSubscription(AsSubscription as) {
        subscriptions.add(as);
    }

    public boolean isEngineUsed(AsEngine engine) {
        for (AsSubscription sub : subscriptions) {
            if (engine == sub.getSource())
                return true;
            if (engine == sub.getTarget())
                return true;
        }
        return false;
    }

    public boolean isEngineUsed(String name) {
        if (name==null)
            name = "";
        for (AsSubscription sub : subscriptions) {
            if (name.equalsIgnoreCase(sub.getSource().getName()))
                return true;
            if (name.equalsIgnoreCase(sub.getTarget().getName()))
                return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.engines);
        hash = 37 * hash + Objects.hashCode(this.subscriptions);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AsConfig other = (AsConfig) obj;
        if (!Objects.equals(this.engines, other.engines)) {
            return false;
        }
        if (!Objects.equals(this.subscriptions, other.subscriptions)) {
            return false;
        }
        return true;
    }

    public static AsConfig load(Element root) {
        final AsConfig config = new AsConfig();
        for (Element el : root.getChildren(EL_ENGINE)) {
            config.addEngine(parseEngine(el));
        }
        for (Element el : root.getChildren(EL_SUB)) {
            config.addSubscription(parseSubscription(config, el));
        }
        return config;
    }

    public static Element save(AsConfig config) {
        Element root = new Element(EL_ROOT);
        for (AsEngine ae : config.engines.values()) {
            root.addContent(formatEngine(ae));
        }
        for (AsSubscription as : config.subscriptions) {
            root.addContent(formatSubscription(as));
        }
        return root;
    }

    private static AsEngine parseEngine(Element el) {
        AsEngine ae = new AsEngine(
                Misc.getAttr(el, "name"),
                EngineType.valueOf(Misc.getAttr(el, "type")));
        Element cur;
        cur = el.getChild(EL_ENG_RSH);
        if (cur!=null)
            ae.setRemoteExec(Misc.getText(cur));
        cur = el.getChild(EL_ENG_PATH);
        if (cur!=null)
            ae.setEngineInstallDir(Misc.getText(cur));
        cur = el.getChild(EL_ENG_INST);
        if (cur!=null)
            ae.setInstanceName(Misc.getText(cur));
        cur = el.getChild(EL_CMD_VERSION);
        if (cur!=null)
            ae.setCommandVersion(Misc.getText(cur));
        cur = el.getChild(EL_CMD_EVENTS);
        if (cur!=null)
            ae.setCommandEvents(Misc.getText(cur));
        cur = el.getChild(EL_CMD_CLEAR);
        if (cur!=null)
            ae.setCommandClear(Misc.getText(cur));
        cur = el.getChild(EL_CMD_BMK_GET);
        if (cur!=null)
            ae.setCommandBookmarkGet(Misc.getText(cur));
        cur = el.getChild(EL_CMD_BMK_PUT);
        if (cur!=null)
            ae.setCommandBookmarkPut(Misc.getText(cur));
        return ae;
    }

    private static Element formatEngine(AsEngine ae) {
        Element cur = new Element(EL_ENGINE);
        cur.setAttribute("name", ae.getName());
        cur.setAttribute("type", ae.getType().name());
        addNonBlankText(cur, EL_ENG_RSH, ae.getRemoteExec());
        addNonBlankText(cur, EL_ENG_PATH, ae.getEngineInstallDir());
        addNonBlankText(cur, EL_ENG_INST, ae.getInstanceName());
        addNonBlankText(cur, EL_CMD_VERSION, ae.getCommandVersion());
        addNonBlankText(cur, EL_CMD_EVENTS, ae.getCommandEvents());
        addNonBlankText(cur, EL_CMD_CLEAR, ae.getCommandClear());
        addNonBlankText(cur, EL_CMD_BMK_GET, ae.getCommandBookmarkGet());
        addNonBlankText(cur, EL_CMD_BMK_PUT, ae.getCommandBookmarkPut());
        return cur;
    }

    private static void addNonBlankText(Element cur, String tag, String text) {
        if (!StringUtils.isBlank(text)) {
            Element cmd = new Element(tag);
            cmd.addContent(new CDATA(text));
            cur.addContent(cmd);
        }
    }

    private static AsSubscription parseSubscription(AsConfig config, Element el) {
        AsEngine source = config.getEngine(Misc.getAttr(el, "source"));
        AsEngine target = config.getEngine(Misc.getAttr(el, "target"));
        AsSubscription as  = new AsSubscription(
                Misc.getAttr(el, "name"), source, target);
        if (source==null || target==null)
            throw new RuntimeException("Source or target missing for subscription " + as.getName());
        as.setSkipNewBlobs(Misc.getAttr(el, "skipNewBlobs", false));
        return as;
    }

    private static Element formatSubscription(AsSubscription as) {
        Element cur = new Element(EL_SUB);
        cur.setAttribute("name", as.getName());
        cur.setAttribute("source", as.getSource().getName());
        cur.setAttribute("target", as.getTarget().getName());
        cur.setAttribute("skipNewBlobs", as.isSkipNewBlobs() ? "true" : "false");
        return cur;
    }

    public static AsConfig load(InputStream is) {
        Element cur;
        try {
            cur = new SAXBuilder().build(is).detachRootElement();
        } catch(IOException | JDOMException ix) {
            throw new RuntimeException("Failed to parse input stream", ix);
        }
        return load(cur);
    }

    public static AsConfig load(File inputFile) {
        Element cur;
        try {
            cur = new SAXBuilder().build(inputFile).detachRootElement();
        } catch(IOException | JDOMException ix) {
            throw new RuntimeException("Failed to parse file " + inputFile, ix);
        }
        return load(cur);
    }

    public static AsConfig load(String inputFile) {
        return load(new File(inputFile));
    }

    public static AsConfig loadDir(File inputDir) {
        // Find the names of files to be loaded
        final List<String> files = new ArrayList<>();
        try {
            Files.walk(Paths.get(inputDir.getPath())).filter(Files::isRegularFile)
                    .forEach((f) -> {
                        String fname = f.toString();
                        if (fname.endsWith(".xml"))
                            files.add(fname);
                    });
        } catch(IOException ix) {
            throw new RuntimeException("Error reading config directory", ix);
        }
        if (files.isEmpty())
            throw new RuntimeException("No config files in directory " + inputDir);
        // Construct the XML DOM tree containing the content from all the files
        Element root = new Element(EL_ROOT);
        for (String fname : files) {
            Element cur;
            try {
                cur = new SAXBuilder().build(fname).detachRootElement();
            } catch(IOException | JDOMException ix) {
                throw new RuntimeException("Failed to parse file " + fname, ix);
            }
            for ( Element item : new ArrayList<>(cur.getChildren()) ) {
                root.addContent(item.detach());
            }
        }
        // Convert the JDOM model into the configuration object
        return load(root);
    }

    public static AsConfig loadDir(String inputDir) {
        return loadDir(new File(inputDir));
    }

    public static void save(AsConfig config, OutputStream os) {
        try {
            new XMLOutputter(Format.getPrettyFormat())
                    .output(new Document(save(config)), os);
        } catch(IOException ix) {
            throw new RuntimeException("Cannot write configuration", ix);
        }
    }

    public static void save(AsConfig config, File outputFile) {
        try (OutputStream os = new FileOutputStream(outputFile)) {
            save(config, os);
        } catch(IOException ix) {
            throw new RuntimeException("Cannot create output file", ix);
        }
    }

    public static void save(AsConfig config, String outputFile) {
        save(config, new File(outputFile));
    }

    public static AsConfig load(AsGlobals globals) {
        return load(globals.getDataFile());
    }

    public static AsConfig loadDir(AsGlobals globals) {
        return loadDir(globals.getConfigDirectory());
    }

}
