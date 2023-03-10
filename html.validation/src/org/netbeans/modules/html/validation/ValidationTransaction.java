/*
 * Copyright (c) 2005, 2006 Henri Sivonen
 * Copyright (c) 2007-2010 Mozilla Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package org.netbeans.modules.html.validation;

import com.thaiopensource.relaxng.impl.CombineValidator;
import com.thaiopensource.util.PropertyMap;
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.*;
import com.thaiopensource.validate.auto.AutoSchemaReader;
import com.thaiopensource.validate.prop.rng.RngProperty;
import com.thaiopensource.validate.prop.wrap.WrapProperty;
import com.thaiopensource.validate.rng.CompactSchemaReader;
import com.thaiopensource.xml.sax.XMLReaderCreator;
import java.io.*;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import nu.validator.htmlparser.common.*;
import nu.validator.htmlparser.sax.HtmlParser;
import nu.validator.messages.MessageEmitterAdapter;
import nu.validator.messages.TooManyErrorsException;
import nu.validator.servlet.ParserMode;
import nu.validator.source.SourceCode;
import nu.validator.spec.Spec;
import nu.validator.spec.html5.Html5SpecBuilder;
import nu.validator.xml.*;
import nu.validator.xml.dataattributes.DataAttributeDroppingSchemaWrapper;
import nu.validator.xml.langattributes.XmlLangAttributeDroppingSchemaWrapper;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.html.editor.lib.api.HtmlVersion;
import org.netbeans.modules.html.editor.lib.api.ProblemDescription;
import org.netbeans.modules.html.validation.patched.BufferingRootNamespaceSniffer;
import org.netbeans.modules.html.validation.patched.LocalCacheEntityResolver;
import org.netbeans.modules.html.validation.patched.RootNamespaceSniffer;
import org.openide.util.NbBundle;
import org.whattf.checker.XmlPiChecker;
import org.whattf.checker.jing.CheckerSchema;
import org.whattf.io.DataUri;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;

/**
 * This class code was mainly extracted from the original class {@link VerifierServletTransaction}.
 *
 * @author hsivonen, mfukala@netbeans.org
 */
public class ValidationTransaction implements DocumentModeHandler, SchemaResolver {

    private static final Logger LOGGER = Logger.getLogger(ValidationTransaction.class.getCanonicalName());

    public static void enableDebug() {
        LOGGER.setLevel(Level.FINE);
        LOGGER.addHandler(new Handler() {

            @Override
            public void publish(LogRecord record) {
                System.out.println(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });
    }
    private static final Pattern SPACE = Pattern.compile("\\s+");
    protected static final int HTML5_SCHEMA = 3;
    protected static final int XHTML1STRICT_SCHEMA = 2;
    protected static final int XHTML1FRAMESET_SCHEMA = 4;
    protected static final int XHTML1TRANSITIONAL_SCHEMA = 1;
    protected static final int XHTML5_SCHEMA = 7;
    private static Spec html5spec;
    private static int[] presetDoctypes;
    private static String[] presetLabels;
    private static String[] presetUrls;
    private static String[] presetNamespaces;
    // XXX SVG!!!
    private static final String[] KNOWN_CONTENT_TYPES = {
        "application/atom+xml", "application/docbook+xml",
        "application/xhtml+xml", "application/xv+xml", "image/svg+xml"};
    private static final String[] NAMESPACES_FOR_KNOWN_CONTENT_TYPES = {
        "http://www.w3.org/2005/Atom", "http://docbook.org/ns/docbook",
        "http://www.w3.org/1999/xhtml", "http://www.w3.org/1999/xhtml",
        "http://www.w3.org/2000/svg"};
    private static final String[] ALL_CHECKERS = {
        "http://c.validator.nu/table/", "http://c.validator.nu/nfc/",
        "http://c.validator.nu/text-content/",
        "http://c.validator.nu/unchecked/",
        "http://c.validator.nu/usemap/", "http://c.validator.nu/obsolete/",
        "http://c.validator.nu/xml-pi/"};
    private static final String[] ALL_CHECKERS_HTML4 = {
        "http://c.validator.nu/table/", "http://c.validator.nu/nfc/",
        "http://c.validator.nu/unchecked/", "http://c.validator.nu/usemap/"};
    private static boolean INITIALIZED = false;
    
    private static String INTERNAL_ERROR_MSG_SEE_LOG = NbBundle.getMessage(ValidationTransaction.class, "MSG_Unexpected_Validator_Error_See_IDE_Log"); //NOI18N
    private static String INTERNAL_ERROR_MSG = NbBundle.getMessage(ValidationTransaction.class, "MSG_Unexpected_Validator_Error"); //NOI18N
    
    protected String document = null;
    ParserMode parser = ParserMode.AUTO;
    private boolean laxType = false;
    protected MessageEmitterAdapter errorHandler;
    protected final AttributesImpl attrs = new AttributesImpl();
    private PropertyMap jingPropertyMap;
    protected LocalCacheEntityResolver entityResolver;
    private static String[] preloadedSchemaUrls;
    private static Schema[] preloadedSchemas;
    private String schemaUrls = null;
    protected Validator validator = null;
    private BufferingRootNamespaceSniffer bufferingRootNamespaceSniffer = null;
    private String contentType = null;
    protected HtmlParser htmlParser = null;
    protected SAXParser xmlParser = null;
    protected XMLReader reader;
    private CharacterHandlerReader sourceReader;
    protected TypedInputSource documentInput;
    protected PrudentHttpEntityResolver httpRes;
    protected DataUriEntityResolver dataRes;
    protected ContentTypeParser contentTypeParser;
    private Map<String, Validator> loadedValidatorUrls = new HashMap<String, Validator>();
    private boolean checkNormalization = false;
    private boolean rootNamespaceSeen = false;
    private SourceCode sourceCode = new SourceCode();
    private boolean showSource;
    private BaseUriTracker baseUriTracker = null;
    private String charsetOverride = null;
    private Set<String> filteredNamespaces = new LinkedHashSet<String>(); // linked
    private LexicalHandler lexicalHandler;
    private Reader codeToValidate;
    private long validationTime;
    private ProblemsHandler problemsHandler = new ProblemsHandler();
    private LinesMapper linesMapper = new LinesMapper();
    private HtmlVersion version;
    private String encoding;

    public static synchronized ValidationTransaction create(HtmlVersion version) {
        return new ValidationTransaction(version);
    }

    private static void initializeLocalEntities_HACK() {
        //some of the validator's resources are read directly by URLConnection-s
        //using no entity resolver. The URLs are first checked in System properties
        //and if there's no property value defined the default network URL (http://...)
        //is used. This causes the support not working offline and if online
        //makes the initialization really slow.

        //hacked by loading the resources from the internall files cache via
        //returned internall URLs.

        //IMO should be fixed in validator.nu by using the local cache entity resolver.

        //MessageEmitterAdapter:
        URL url = LocalCacheEntityResolver.getResource("http://wiki.whatwg.org/wiki/MicrosyntaxDescriptions");
        System.setProperty("nu.validator.spec.microsyntax-descriptions", url.toExternalForm());

        url = LocalCacheEntityResolver.getResource("http://wiki.whatwg.org/wiki/Validator.nu_alt_advice");
        System.setProperty("nu.validator.spec.alt-advice", url.toExternalForm());

//        //CharsetData:
//        url = LocalCacheEntityResolver.getResource("http://www.iana.org/assignments/character-sets");
//        System.setProperty("org.whattf.datatype.charset-registry", url.toExternalForm());
//
//        //LanguageData:
//        url = LocalCacheEntityResolver.getResource("http://www.iana.org/assignments/language-subtag-registry");
//        System.setProperty("org.whattf.datatype.lang-registry", url.toExternalForm());


    }

    private static synchronized void initialize() {
        if (INITIALIZED) {
            return;
        }

        ProgressHandle progress = ProgressHandleFactory.createHandle(
                NbBundle.getMessage(ValidationTransaction.class, "MSG_InitHTMLValidation")); //NOI18N

        progress.start();
        progress.switchToIndeterminate();

        initializeLocalEntities_HACK();

        try {
            LOGGER.fine("Starting initialization.");

            BufferedReader r = new BufferedReader(new InputStreamReader(LocalCacheEntityResolver.getPresetsAsStream(), "UTF-8"));
            String line;
            List<String> doctypes = new LinkedList<String>();
            List<String> namespaces = new LinkedList<String>();
            List<String> labels = new LinkedList<String>();
            List<String> urls = new LinkedList<String>();

            LOGGER.fine("Starting to loop over config file lines.");

            while ((line = r.readLine()) != null) {
                if ("".equals(line.trim())) {
                    break;
                }
                String s[] = line.split("\t");
                doctypes.add(s[0]);
                namespaces.add(s[1]);
                labels.add(s[2]);
                urls.add(s[3]);
            }

//            progress.start(10 * (urls.size() + 50) /* reading the html spec */);
//            progress.progress(NbBundle.getMessage(ValidationTransaction.class, "MSG_LoadingSchemaFiles"));

            LOGGER.fine("Finished reading config.");

            String[] presetDoctypesAsStrings = doctypes.toArray(new String[0]);
            presetNamespaces = namespaces.toArray(new String[0]);
            presetLabels = labels.toArray(new String[0]);
            presetUrls = urls.toArray(new String[0]);

            LOGGER.fine("Converted config to arrays.");

            for (int i = 0; i < presetNamespaces.length; i++) {
                String str = presetNamespaces[i];
                if ("-".equals(str)) {
                    presetNamespaces[i] = null;
                } else {
                    presetNamespaces[i] = presetNamespaces[i].intern();
                }
            }

            LOGGER.fine("Prepared namespace array.");

            presetDoctypes = new int[presetDoctypesAsStrings.length];
            for (int i = 0; i < presetDoctypesAsStrings.length; i++) {
                presetDoctypes[i] = Integer.parseInt(presetDoctypesAsStrings[i]);
            }

            LOGGER.fine("Parsed doctype numbers into ints.");

//            String prefix = System.getProperty("nu.validator.servlet.cachepathprefix");

//            log4j.fine("The cache path prefix is: " + prefix);

            ErrorHandler eh = new SystemErrErrorHandler();
            LocalCacheEntityResolver er = new LocalCacheEntityResolver(new NullEntityResolver());
            er.setAllowRnc(true);
            PropertyMapBuilder pmb = new PropertyMapBuilder();
            pmb.put(ValidateProperty.ERROR_HANDLER, eh);
            pmb.put(ValidateProperty.ENTITY_RESOLVER, er);
            pmb.put(ValidateProperty.XML_READER_CREATOR,
                    new XMLReaderCreatorImpl(eh, er));
            RngProperty.CHECK_ID_IDREF.add(pmb);
            PropertyMap pMap = pmb.toPropertyMap();

            LOGGER.fine("Parsing set up. Starting to read schemas.");

            SortedMap<String, Schema> schemaMap = new TreeMap<String, Schema>();

            schemaMap.put("http://c.validator.nu/table/",
                    CheckerSchema.TABLE_CHECKER);
            schemaMap.put("http://hsivonen.iki.fi/checkers/table/",
                    CheckerSchema.TABLE_CHECKER);
            schemaMap.put("http://c.validator.nu/nfc/",
                    CheckerSchema.NORMALIZATION_CHECKER);
            schemaMap.put("http://hsivonen.iki.fi/checkers/nfc/",
                    CheckerSchema.NORMALIZATION_CHECKER);
            schemaMap.put("http://c.validator.nu/debug/",
                    CheckerSchema.DEBUG_CHECKER);
            schemaMap.put("http://hsivonen.iki.fi/checkers/debug/",
                    CheckerSchema.DEBUG_CHECKER);
            schemaMap.put("http://c.validator.nu/text-content/",
                    CheckerSchema.TEXT_CONTENT_CHECKER);
            schemaMap.put("http://hsivonen.iki.fi/checkers/text-content/",
                    CheckerSchema.TEXT_CONTENT_CHECKER);
            schemaMap.put("http://c.validator.nu/usemap/",
                    CheckerSchema.USEMAP_CHECKER);
            schemaMap.put("http://n.validator.nu/checkers/usemap/",
                    CheckerSchema.USEMAP_CHECKER);
            schemaMap.put("http://c.validator.nu/unchecked/",
                    CheckerSchema.UNCHECKED_SUBTREE_WARNER);
            schemaMap.put("http://s.validator.nu/html5/assertions.sch",
                    CheckerSchema.ASSERTION_SCH);
            schemaMap.put("http://c.validator.nu/obsolete/",
                    CheckerSchema.CONFORMING_BUT_OBSOLETE_WARNER);
            schemaMap.put("http://c.validator.nu/xml-pi/",
                    CheckerSchema.XML_PI_CHECKER);

            for (int i = 0; i < presetUrls.length; i++) {
                String[] urls1 = SPACE.split(presetUrls[i]);
                for (int j = 0; j < urls1.length; j++) {
                    String url = urls1[j];
                    if (schemaMap.get(url) == null && !isCheckerUrl(url)) {
                        Schema sch = proxySchemaByUrl(url, er, pMap);
                        schemaMap.put(url, sch);
//                        progress.progress(10);
                    }
                }
            }

            LOGGER.fine("Schemas read.");

            preloadedSchemaUrls = new String[schemaMap.size()];
            preloadedSchemas = new Schema[schemaMap.size()];
            int i = 0;
            for (Map.Entry<String, Schema> entry : schemaMap.entrySet()) {
                preloadedSchemaUrls[i] = entry.getKey().intern();
                Schema s = entry.getValue();
                String u = entry.getKey();
                if (isDataAttributeDroppingSchema(u)) {
                    s = new DataAttributeDroppingSchemaWrapper(
                            s);
                }
                if (isXmlLangAllowingSchema(u)) {
                    s = new XmlLangAttributeDroppingSchemaWrapper(s);
                }
                preloadedSchemas[i] = s;
                i++;
            }

//            progress.progress(NbBundle.getMessage(ValidationTransaction.class, "MSG_LoadingHtmlSpecification"));
            LOGGER.fine("Reading spec.");

            html5spec = Html5SpecBuilder.parseSpec(LocalCacheEntityResolver.getHtml5SpecAsStream());
//            progress.progress(50);

            LOGGER.fine("Spec read.");

            LOGGER.fine("Initialization complete.");

            INITIALIZED = true;

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            progress.finish();

        }
    }

     private static boolean isDataAttributeDroppingSchema(String key) {
        return ("http://s.validator.nu/xhtml5.rnc".equals(key)
                || "http://s.validator.nu/html5.rnc".equals(key)
                || "http://s.validator.nu/html5-its.rnc".equals(key)
                || "http://s.validator.nu/xhtml5-rdfalite.rnc".equals(key)
                || "http://s.validator.nu/html5-rdfalite.rnc".equals(key)
                || "http://s.validator.nu/w3c-xhtml5.rnc".equals(key)
                || "http://s.validator.nu/w3c-html5.rnc".equals(key)
                || "http://s.validator.nu/w3c-xhtml5-microdata-rdfalite.rnc".equals(key)
                || "http://s.validator.nu/w3c-xhtml5-microdata-rdfa.rnc".equals(key)
                || "http://s.validator.nu/w3c-html5-microdata-rdfalite.rnc".equals(key)
                || "http://s.validator.nu/w3c-html5-microdata-rdfa.rnc".equals(key));
    }

    private static boolean isXmlLangAllowingSchema(String key) {
        return ("http://s.validator.nu/xhtml5.rnc".equals(key)
                || "http://s.validator.nu/html5.rnc".equals(key)
                || "http://s.validator.nu/html5-its.rnc".equals(key)
                || "http://s.validator.nu/xhtml5-rdfalite.rnc".equals(key)
                || "http://s.validator.nu/html5-rdfalite.rnc".equals(key)
                || "http://s.validator.nu/w3c-xhtml5.rnc".equals(key)
                || "http://s.validator.nu/w3c-html5.rnc".equals(key)
                || "http://s.validator.nu/w3c-xhtml5-microdata-rdfalite.rnc".equals(key)
                || "http://s.validator.nu/w3c-xhtml5-microdata-rdfa.rnc".equals(key)
                || "http://s.validator.nu/w3c-html5-microdata-rdfalite.rnc".equals(key)
                || "http://s.validator.nu/w3c-html5-microdata-rdfa.rnc".equals(key));
    }
    
    private static boolean isCheckerUrl(String url) {
        if ("http://c.validator.nu/all/".equals(url)
                || "http://hsivonen.iki.fi/checkers/all/".equals(url)) {
            return true;
        } else if ("http://c.validator.nu/all-html4/".equals(url)
                || "http://hsivonen.iki.fi/checkers/all-html4/".equals(url)) {
            return true;
        } else if ("http://c.validator.nu/base/".equals(url)) {
            return true;
        } else if ("http://c.validator.nu/rdfalite/".equals(url)) {
            return true;
        }
        for (int i = 0; i < ALL_CHECKERS.length; i++) {
            if (ALL_CHECKERS[i].equals(url)) {
                return true;
            }
        }
        return false;
    }

    public ValidationTransaction(HtmlVersion version) {
        this.version = version;
        initialize();
    }

    public List<ProblemDescription> getFoundProblems() {
        return problemsHandler.getProblems();
    }

    /** return a list of problems with the given severity and higher (more severe issues) */
    public List<ProblemDescription> getFoundProblems(int ofThisTypeAndMoreSevere) {
        return getFoundProblems(new ProblemDescriptionFilter.SeverityFilter(ofThisTypeAndMoreSevere));
    }

    public List<ProblemDescription> getFoundProblems(ProblemDescriptionFilter filter) {
        List<ProblemDescription> filtered = new ArrayList<ProblemDescription>();
        for (ProblemDescription pd : getFoundProblems()) {
            if (filter.accepts(pd)) {
                filtered.add(pd);
            }
        }
        return filtered;
    }

    public long getValidationTime() {
        return validationTime;
    }

    public void validateCode(Reader code, String sourceURI, Set<String> filteredNamespaces, String encoding) throws SAXException {
        long from = System.currentTimeMillis();

        codeToValidate = code;
        document = sourceURI; //represents an URI where the document can be loaded
        parser = htmlVersion2ParserMode(version);

        LOGGER.fine(String.format("Using %s parser.", parser.name()));

//        charsetOverride = "UTF-8";
        this.encoding = encoding;
        this.filteredNamespaces = filteredNamespaces;
        if (!filteredNamespaces.isEmpty()) {
            StringBuilder fns = new StringBuilder();
            for (String ns : filteredNamespaces) {
                fns.append(ns).append(", ");
            }
            LOGGER.fine(String.format("Filtering following namespaces: %s", fns));
        }

        int lineOffset = 0;

        errorHandler = new MessageEmitterAdapter(sourceCode,
                showSource, null, lineOffset,
                new NbMessageEmitter(problemsHandler, linesMapper, true));

        errorHandler.setLoggingOk(true);
        errorHandler.setErrorsOnly(false);

        validate();

        validationTime = System.currentTimeMillis() - from;
    }

    public boolean isSuccess() {
        return getFoundProblems(ProblemDescription.WARNING).isEmpty();

    }

    private ParserMode htmlVersion2ParserMode(HtmlVersion version) {
        if (version.isXhtml()) {
            return ParserMode.XML_NO_EXTERNAL_ENTITIES; //we do not use the parser for validation, no need to load external entities
        } else {
            switch (version) {
                case HTML41_STRICT:
                    return ParserMode.HTML401_STRICT;
                case HTML41_TRANSATIONAL:
                    return ParserMode.HTML401_TRANSITIONAL;
                case HTML41_FRAMESET:
                    return ParserMode.AUTO; //???
                case HTML5:
                    return ParserMode.HTML;
                default:
                    return ParserMode.AUTO;
            }
        }

    }

    private boolean isHtmlUnsafePreset() {
        if ("".equals(schemaUrls)) {
            return false;
        }
        boolean preset = false;
        for (int i = 0; i < presetUrls.length; i++) {
            if (presetUrls[i].equals(schemaUrls)) {
                preset = true;
                break;
            }
        }
        if (!preset) {
            return false;
        }
        return !(schemaUrls.startsWith("http://s.validator.nu/xhtml10/xhtml-basic.rnc")
                || schemaUrls.startsWith("http://s.validator.nu/xhtml10/xhtml-strict.rnc")
                || schemaUrls.startsWith("http://s.validator.nu/xhtml10/xhtml-transitional.rnc")
                || schemaUrls.startsWith("http://s.validator.nu/xhtml10/xhtml-frameset.rnc")
                || schemaUrls.startsWith("http://s.validator.nu/html5/html5full.rnc")
                || schemaUrls.startsWith("http://s.validator.nu/html5/html5full-aria.rnc") || schemaUrls.startsWith("http://s.validator.nu/html5-aria-svg-mathml.rnc"));

    }

    @SuppressWarnings("deprecation")
    void validate() throws SAXException {

//        httpRes = new PrudentHttpEntityResolver(SIZE_LIMIT, laxType,
//                errorHandler);
//        dataRes = new DataUriEntityResolver(httpRes, laxType, errorHandler);
//        contentTypeParser = new ContentTypeParser(errorHandler, laxType);
//        entityResolver = new LocalCacheEntityResolver(dataRes);

        entityResolver = new LocalCacheEntityResolver(new NullEntityResolver());

        setAllowRnc(true);
        try {
            this.errorHandler.start(document);
            PropertyMapBuilder pmb = new PropertyMapBuilder();
            pmb.put(ValidateProperty.ERROR_HANDLER, errorHandler);
            pmb.put(ValidateProperty.ENTITY_RESOLVER, entityResolver);
            pmb.put(ValidateProperty.XML_READER_CREATOR,
                    new XMLReaderCreatorImpl(errorHandler,
                    entityResolver));
            pmb.put(ValidateProperty.SCHEMA_RESOLVER, this);
            RngProperty.CHECK_ID_IDREF.add(pmb);
            jingPropertyMap = pmb.toPropertyMap();

//            tryToSetupValidator();

            setAllowRnc(false);

            loadDocAndSetupParser();
            if (htmlParser != null) {
                setErrorProfile();
            }

            reader.setErrorHandler(errorHandler);
            contentType = documentInput.getType();
            sourceCode.initialize(documentInput);

            WiretapXMLReaderWrapper wiretap = new WiretapXMLReaderWrapper(
                    reader);
            boolean isXhtml = parser == ParserMode.XML_EXTERNAL_ENTITIES_NO_VALIDATION
                    || parser == ParserMode.XML_NO_EXTERNAL_ENTITIES;

            ContentHandler recorder = isXhtml
                    ? new XercesInaccurateLocatorWorkaround(sourceCode.getLocationRecorder(), linesMapper)
                    : sourceCode.getLocationRecorder();

            if (baseUriTracker == null) {
                wiretap.setWiretapContentHander(recorder);
            } else {
                wiretap.setWiretapContentHander(new CombineContentHandler(
                        recorder, baseUriTracker));
            }
            wiretap.setWiretapLexicalHandler((LexicalHandler) recorder);
            reader = wiretap;

            if (htmlParser != null) {
                htmlParser.addCharacterHandler(linesMapper);
                htmlParser.addCharacterHandler(sourceCode);
                htmlParser.setMappingLangToXmlLang(true);
                htmlParser.setErrorHandler(errorHandler.getExactErrorHandler());
                htmlParser.setTreeBuilderErrorHandlerOverride(errorHandler);
                errorHandler.setHtml(true);
            } else if (xmlParser != null) {
                // this must be after wiretap!
                if (!filteredNamespaces.isEmpty()) {
                    reader = new NamespaceDroppingXMLReaderWrapper(reader,
                            filteredNamespaces);
                }
                xmlParser.getXMLReader().setErrorHandler(errorHandler.getExactErrorHandler());
                sourceReader.addCharacterHandler(linesMapper);
            } else {
                throw new RuntimeException("Bug. Unreachable.");
            }
            reader = new AttributesPermutingXMLReaderWrapper(reader); // make
            // RNG
            // validation
            // better
            if (charsetOverride != null) {
                String charset = documentInput.getEncoding();
                if (charset == null) {
                    errorHandler.warning(new SAXParseException(
                            "Overriding document character encoding from none to \u201C"
                            + charsetOverride + "\u201D.", null));
                } else {
                    errorHandler.warning(new SAXParseException(
                            "Overriding document character encoding from \u201C"
                            + charset + "\u201D to \u201C"
                            + charsetOverride + "\u201D.", null));
                }
                documentInput.setEncoding(charsetOverride);
            }
            reader.parse(documentInput);
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.INFO, getDocumentErrorMsg(), e);
            errorHandler.internalError(
                    e,
                    INTERNAL_ERROR_MSG_SEE_LOG);
        } catch (TooManyErrorsException e) {
            LOGGER.log(Level.FINE, getDocumentErrorMsg(), e);
            errorHandler.fatalError(e);
        } catch (SAXException e) {
            LOGGER.log(Level.FINE, getDocumentErrorMsg(), e);
        } catch (IOException e) {
            LOGGER.log(Level.INFO, getDocumentErrorMsg(), e);
            errorHandler.ioError(e);
        } catch (IncorrectSchemaException e) {
            LOGGER.log(Level.INFO, getDocumentErrorMsg(), e);
            errorHandler.schemaError(e);
        } catch (RuntimeException e) {
            String message = reportRuntimeExceptionOnce(e) 
                    ? INTERNAL_ERROR_MSG_SEE_LOG 
                    : INTERNAL_ERROR_MSG;
            errorHandler.internalError(e, message);
        } catch (Error e) {
            LOGGER.log(Level.INFO, getDocumentInternalErrorMsg(), e);
            errorHandler.internalError(
                    e,
                    INTERNAL_ERROR_MSG_SEE_LOG);
        } finally {
            errorHandler.end(successMessage(), failureMessage());
        }
    }

    private static final Set<Marker> REPORTED_RUNTIME_EXCEPTIONS = new HashSet<Marker>();

    /**
     * Report REs only once per ide session and use lower log levels for known issues
     * 
     * @return true if the exception has been logged and is visible in the IDE log
     */
    private boolean reportRuntimeExceptionOnce(RuntimeException e) {
        int hash = document.hashCode();
        hash = 21 * hash + e.getClass().hashCode();
        if(e.getMessage() != null) {
            hash = 21 * hash + e.getMessage().hashCode();
        } else {
            //no message provided, so use the whole stacktrace hashcode
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            hash = 21 * hash + sw.toString().hashCode();
        }

        Level level = isKnownProblem(e) ? Level.FINE : Level.INFO;
        Marker marker = new Marker(hash);
        if(REPORTED_RUNTIME_EXCEPTIONS.add(marker)) {
            LOGGER.log(level, getDocumentInternalErrorMsg(), e);
        }
        return LOGGER.isLoggable(level);
    }

    private static boolean isKnownProblem(RuntimeException e) {
        //issue #194939
        Class eClass = e.getClass();
        if(eClass.equals(StringIndexOutOfBoundsException.class)) {
            StackTraceElement[] stelements = e.getStackTrace();
            if(stelements.length >= 1) {
                if(stelements[1].getClassName().equals("com.thaiopensource.validate.schematron.OutputHandler") //NOI18N
                        && stelements[1].getMethodName().equals("startElement")) { //NOI18N
                    return true;
                }
            }
        } else if(eClass.equals(IllegalStateException.class)) {
            //Bug 199647 - Failed validation and IllegalStateException during pojects scanning
            String msg = "Two cells in effect cannot start on the same column, so this should never happen!"; //NOI18N
            return e.getMessage() != null && e.getMessage().indexOf(msg) != -1;
        }

        return false;
    }

    private String getDocumentErrorMsg() {
        return new StringBuilder().append("An error occurred during validation of ").append(document).toString(); //NOI18N
    }

    private String getDocumentInternalErrorMsg() {
        return new StringBuilder().append("An internal error occurred during validation of ").append(document).toString(); //NOI18N
    }

    /**
     * @return
     * @throws SAXException
     */
    protected String successMessage() throws SAXException {
        return "The document validates according to the specified schema(s).";
    }

    protected String failureMessage() throws SAXException {
        return "There were errors.";
    }

    /**
     * @throws SAXException
     * @throws IOException
     * @throws IncorrectSchemaException
     */
    protected void tryToSetupValidator() throws SAXException, IOException,
            IncorrectSchemaException {
        validator = validatorByUrls(schemaUrls);
    }

    protected void setErrorProfile() {
//        profile = request.getParameter("profile");

        HashMap<String, String> profileMap = new HashMap<String, String>();

//        if ("pedagogical".equals(profile)) {
//            profileMap.put("xhtml1", "warn");
//        } else if ("polyglot".equals(profile)) {
//            profileMap.put("xhtml1", "warn");
//            profileMap.put("xhtml2", "warn");
//        } else {
//            return; // presumed to be permissive
//        }

        htmlParser.setErrorProfile(profileMap);
    }

    /**
     * @throws SAXException
     * @throws IOException
     * @throws IncorrectSchemaException
     * @throws SAXNotRecognizedException
     * @throws SAXNotSupportedException
     */
    protected void loadDocAndSetupParser() throws SAXException, IOException,
            IncorrectSchemaException, SAXNotRecognizedException,
            SAXNotSupportedException, ParserConfigurationException {
        switch (parser) {
            case HTML_AUTO:
            case HTML:
            case HTML401_STRICT:
            case HTML401_TRANSITIONAL:
                if (isHtmlUnsafePreset()) {
                    String message = "The chosen preset schema is not appropriate for HTML.";
                    SAXException se = new SAXException(message);
                    errorHandler.schemaError(se);
                    throw se;
                }
                setAllowGenericXml(false);
                setAllowHtml(true);
                setAcceptAllKnownXmlTypes(false);
                setAllowXhtml(false);
                loadDocumentInput(false);
                newHtmlParser();
                DoctypeExpectation doctypeExpectation;
                int schemaId;
                switch (parser) {
                    case HTML:
                        doctypeExpectation = DoctypeExpectation.HTML;
                        schemaId = HTML5_SCHEMA;
                        break;
                    case HTML401_STRICT:
                        doctypeExpectation = DoctypeExpectation.HTML401_STRICT;
                        schemaId = XHTML1STRICT_SCHEMA;
                        break;
                    case HTML401_TRANSITIONAL:
                        doctypeExpectation = DoctypeExpectation.HTML401_TRANSITIONAL;
                        schemaId = XHTML1TRANSITIONAL_SCHEMA;
                        break;
                    default:
                        doctypeExpectation = DoctypeExpectation.AUTO;
                        schemaId = 0;
                        break;
                }
                htmlParser.setDoctypeExpectation(doctypeExpectation);
                htmlParser.setDocumentModeHandler(this);
//                htmlParser.setProperty("http://validator.nu/properties/body-fragment-context-mode", bodyFragmentContextMode);
                reader = htmlParser;
                if (validator == null) {
                    LOGGER.fine(String.format("Using following schemas: %s", getSchemasForDoctypeId(schemaId)));
                    validator = validatorByDoctype(schemaId);
                }
                if (validator != null) {
                    reader.setContentHandler(validator.getContentHandler());
                }
                break;
            case XML_NO_EXTERNAL_ENTITIES:
            case XML_EXTERNAL_ENTITIES_NO_VALIDATION:
                setAllowGenericXml(true);
                setAllowHtml(false);
                setAcceptAllKnownXmlTypes(true);
                setAllowXhtml(true);
                loadDocumentInput(true);

                if (version != null) {
                    switch (version) {
                        case XHTML10_TRANSATIONAL:
                            schemaId = XHTML1TRANSITIONAL_SCHEMA;
                            break;
                        case XHTML10_STICT:
                            schemaId = XHTML1STRICT_SCHEMA;
                            break;
                        case XHTML10_FRAMESET:
                            schemaId = XHTML1FRAMESET_SCHEMA;
                            break;
                        default:
                            schemaId = 0;
                    }

                    if (schemaId != 0) {
                        validator = validatorByDoctype(schemaId);

                        LOGGER.fine(String.format("Using following schemas: %s", getSchemasForDoctypeId(schemaId)));
                    }
                }


                setupXmlParser();
                break;
            default:
                setAllowGenericXml(true);
                setAllowHtml(true);
                setAcceptAllKnownXmlTypes(true);
                setAllowXhtml(true);
                loadDocumentInput(false);
                if ("text/html".equals(documentInput.getType())) {
                    if (isHtmlUnsafePreset()) {
                        String message = "The Content-Type was \u201Ctext/html\u201D, but the chosen preset schema is not appropriate for HTML.";
                        SAXException se = new SAXException(message);
                        errorHandler.schemaError(se);
                        throw se;
                    }
                    errorHandler.info("The Content-Type was \u201Ctext/html\u201D. Using the HTML parser.");
                    newHtmlParser();
                    htmlParser.setDoctypeExpectation(DoctypeExpectation.AUTO);
                    htmlParser.setDocumentModeHandler(this);
                    reader = htmlParser;
                    if (validator != null) {
                        reader.setContentHandler(validator.getContentHandler());
                    }
                } else {
                    errorHandler.info("The Content-Type was \u201C"
                            + documentInput.getType()
                            + "\u201D. Using the XML parser (not resolving external entities).");
                    setupXmlParser();
                }
                break;
        }
    }

    /**
     *
     */
    protected void newHtmlParser() {
        htmlParser = new HtmlParser();
        htmlParser.setCommentPolicy(XmlViolationPolicy.ALLOW);
        htmlParser.setContentNonXmlCharPolicy(XmlViolationPolicy.ALLOW);
        htmlParser.setContentSpacePolicy(XmlViolationPolicy.ALTER_INFOSET);
        htmlParser.setNamePolicy(XmlViolationPolicy.ALLOW);
        htmlParser.setStreamabilityViolationPolicy(XmlViolationPolicy.FATAL);
        htmlParser.setXmlnsPolicy(XmlViolationPolicy.ALTER_INFOSET);
        htmlParser.setMappingLangToXmlLang(true);
        htmlParser.setHtml4ModeCompatibleWithXhtml1Schemata(true);
        htmlParser.setHeuristics(Heuristics.ALL);
        htmlParser.setEntityResolver(entityResolver);
    }

    protected Validator validatorByDoctype(int schemaId) throws SAXException,
            IOException, IncorrectSchemaException {
        if (schemaId == 0) {
            return null;
        }
        for (int i = 0; i < presetDoctypes.length; i++) {
            if (presetDoctypes[i] == schemaId) {
                return validatorByUrls(presetUrls[i]);
            }
        }
        throw new RuntimeException("Doctype mappings not initialized properly.");
    }

    /**
     * @param entityResolver2
     * @return
     * @throws SAXNotRecognizedException
     * @throws SAXNotSupportedException
     */
    protected void setupXmlParser() throws SAXNotRecognizedException,
            SAXNotSupportedException,
            ParserConfigurationException,
            SAXException {

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        xmlParser = factory.newSAXParser();
//        xmlParser.getXMLReader().setFeature(
//                "http://apache.org/xml/features/continue-after-fatal-error",
//                true);
        sourceReader.addCharacterHandler(sourceCode);
        reader = new IdFilter(xmlParser.getXMLReader());
        if (lexicalHandler != null) {
            xmlParser.setProperty("http://xml.org/sax/properties/lexical-handler",
                    (LexicalHandler) lexicalHandler);
        }

        reader.setFeature("http://xml.org/sax/features/string-interning", true);
        reader.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                parser == ParserMode.XML_EXTERNAL_ENTITIES_NO_VALIDATION);
        reader.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                parser == ParserMode.XML_EXTERNAL_ENTITIES_NO_VALIDATION);
        if (parser == ParserMode.XML_EXTERNAL_ENTITIES_NO_VALIDATION) {
            reader.setEntityResolver(entityResolver);
        } else {
            reader.setEntityResolver(new NullEntityResolver());
        }
        if (validator == null) {
            bufferingRootNamespaceSniffer = new BufferingRootNamespaceSniffer(
                    this);
            reader.setContentHandler(bufferingRootNamespaceSniffer);
        } else {
            reader.setContentHandler(new RootNamespaceSniffer(this,
                    validator.getContentHandler()));
            reader.setDTDHandler(validator.getDTDHandler());
        }
    }

    /**
     * @param validator
     * @return
     * @throws SAXException
     * @throws IOException
     * @throws IncorrectSchemaException
     */
    private Validator validatorByUrls(String schemaList) throws SAXException,
            IOException, IncorrectSchemaException {
        Validator v = null;
        String[] schemas = SPACE.split(schemaList);
        for (int i = schemas.length - 1; i > -1; i--) {
            String url = schemas[i];
            if ("http://c.validator.nu/all/".equals(url)
                    || "http://hsivonen.iki.fi/checkers/all/".equals(url)) {
                for (int j = 0; j < ALL_CHECKERS.length; j++) {
                    v = combineValidatorByUrl(v, ALL_CHECKERS[j]);
                }
            } else if ("http://c.validator.nu/all-html4/".equals(url)
                    || "http://hsivonen.iki.fi/checkers/all-html4/".equals(url)) {
                for (int j = 0; j < ALL_CHECKERS_HTML4.length; j++) {
                    v = combineValidatorByUrl(v, ALL_CHECKERS_HTML4[j]);
                }
            } else {
                v = combineValidatorByUrl(v, url);
            }
        }
        return v;
    }

    /**
     * @param val
     * @param url
     * @return
     * @throws SAXException
     * @throws IOException
     * @throws IncorrectSchemaException
     */
    private Validator combineValidatorByUrl(Validator val, String url)
            throws SAXException, IOException, IncorrectSchemaException {
        if (!"".equals(url)) {
            Validator v = validatorByUrl(url);
            if (val == null) {
                val = v;
            } else {
                val = new CombineValidator(v, val);
            }
        }
        return val;
    }

    /**
     * @param url
     * @return
     * @throws SAXException
     * @throws IOException
     * @throws IncorrectSchemaException
     */
    private Validator validatorByUrl(String url) throws SAXException,
            IOException, IncorrectSchemaException {
        Validator v = loadedValidatorUrls.get(url);
        if (v != null) {
            return v;
        }


        if ("http://s.validator.nu/html5/html5full-aria.rnc".equals(url)
                || "http://s.validator.nu/xhtml5-aria-rdf-svg-mathml.rnc".equals(url)
                || "http://s.validator.nu/html5/html5full.rnc".equals(url)
                || "http://s.validator.nu/html5/xhtml5full-xhtml.rnc".equals(url)
                || "http://s.validator.nu/html5-aria-svg-mathml.rnc".equals(url)) {
            errorHandler.setSpec(html5spec);
        }
        Schema sch = resolveSchema(url, jingPropertyMap);
        Validator validatorInstance = sch.createValidator(jingPropertyMap);
        if (validatorInstance.getContentHandler() instanceof XmlPiChecker) {
            lexicalHandler = (LexicalHandler) validatorInstance.getContentHandler();
        }

        loadedValidatorUrls.put(url, v);
        return validatorInstance;
    }

    public Schema resolveSchema(String url, PropertyMap options)
            throws SAXException, IOException, IncorrectSchemaException {
        int i = Arrays.binarySearch(preloadedSchemaUrls, url);
        if (i > -1) {
            Schema rv = preloadedSchemas[i];
            if (options.contains(WrapProperty.ATTRIBUTE_OWNER)) {
                if(rv instanceof ProxySchema && ((ProxySchema)rv).getWrappedSchema() instanceof CheckerSchema) {
                    errorHandler.error(new SAXParseException(
                            "A non-schema checker cannot be used as an attribute schema.",
                            null, url, -1, -1));
                    throw new IncorrectSchemaException();
                } else {
                    // ugly fall through
                }
            } else {
                return rv;
            }
        }

        //this code line should not normally be encountered since the necessary
        //schemas have been preloaded
        LOGGER.log(Level.INFO, "Going to create a non preloaded Schema for {0}", url); //NOI18N
        
        TypedInputSource schemaInput = (TypedInputSource) entityResolver.resolveEntity(
                null, url);
        SchemaReader sr = null;
        if ("application/relax-ng-compact-syntax".equals(schemaInput.getType())) {
            sr = CompactSchemaReader.getInstance();
        } else {
            sr = new AutoSchemaReader();
        }
        Schema sch = sr.createSchema(schemaInput, options);
        return sch;
    }

    private static Schema proxySchemaByUrl(String uri, EntityResolver resolver, PropertyMap pMap) {
        return new ProxySchema(uri, resolver, pMap);
    }
    
    /**
     * @param url
     * @return
     * @throws SAXException
     * @throws IOException
     * @throws IncorrectSchemaException
     */
    private static Schema schemaByUrl(String url, EntityResolver resolver,
            PropertyMap pMap) throws SAXException, IOException,
            IncorrectSchemaException {
        LOGGER.fine(String.format("Will load schema: %s", url));
        long a = System.currentTimeMillis();
        TypedInputSource schemaInput;
        try {
            schemaInput = (TypedInputSource) resolver.resolveEntity(
                    null, url);
        } catch (ClassCastException e) {
            LOGGER.log(Level.SEVERE, url, e);
            throw e;
        }

        SchemaReader sr = null;
        if ("application/relax-ng-compact-syntax".equals(schemaInput.getType())) {
            sr = CompactSchemaReader.getInstance();
            LOGGER.log(Level.FINE, "Used CompactSchemaReader");
        } else {
            sr = new AutoSchemaReader();
            LOGGER.log(Level.FINE, "Used AutoSchemaReader");
        }
        long c = System.currentTimeMillis();

        Schema sch = sr.createSchema(schemaInput, pMap);
        LOGGER.log(Level.FINE, String.format("Schema created in %s ms.", (System.currentTimeMillis() - c)));
        return sch;
    }

    protected String shortenDataUri(String uri) {
        if (DataUri.startsWithData(uri)) {
            return "data:\u2026";
        } else {
            return uri;
        }
    }

    public void rootNamespace(String namespace, Locator locator) throws SAXException {
        if (validator == null) {
            int index = -1;
            for (int i = 0; i < presetNamespaces.length; i++) {
                if (namespace.equals(presetNamespaces[i])) {
                    index = i;
                    break;
                }
            }
            if (index == -1) {
                String message = "Cannot find preset schema for namespace: \u201C"
                        + namespace + "\u201D.";
                SAXException se = new SAXException(message);
                errorHandler.schemaError(se);
                throw se;
            }
            String label = presetLabels[index];
            String urls = presetUrls[index];
            errorHandler.info("Using the preset for " + label
                    + " based on the root namespace " + namespace);
            try {
                validator = validatorByUrls(urls);
            } catch (IOException ioe) {
                // At this point the schema comes from memory.
                throw new RuntimeException(ioe);
            } catch (IncorrectSchemaException e) {
                // At this point the schema comes from memory.
                throw new RuntimeException(e);
            }
            if (bufferingRootNamespaceSniffer == null) {
                throw new RuntimeException(
                        "Bug! bufferingRootNamespaceSniffer was null.");
            }
            bufferingRootNamespaceSniffer.setContentHandler(validator.getContentHandler());
        }

        if (!rootNamespaceSeen) {
            rootNamespaceSeen = true;
            if (contentType != null) {
                int i;
                if ((i = Arrays.binarySearch(KNOWN_CONTENT_TYPES, contentType)) > -1) {
                    if (!NAMESPACES_FOR_KNOWN_CONTENT_TYPES[i].equals(namespace)) {
                        String message = "".equals(namespace) ? "\u201C"
                                + contentType
                                + "\u201D is not an appropriate Content-Type for a document whose root element is not in a namespace."
                                : "\u201C"
                                + contentType
                                + "\u201D is not an appropriate Content-Type for a document whose root namespace is \u201C"
                                + namespace + "\u201D.";
                        SAXParseException spe = new SAXParseException(message,
                                locator);
                        errorHandler.warning(spe);
                    }
                }
            }
        }
    }

    public void documentMode(DocumentMode mode, String publicIdentifier,
            String systemIdentifier, boolean html4SpecificAdditionalErrorChecks)
            throws SAXException {
        if (validator == null) {
            try {
                if ("-//W3C//DTD XHTML 1.0 Transitional//EN".equals(publicIdentifier)) {
                    errorHandler.info("XHTML 1.0 Transitional doctype seen. Appendix C is not supported. Proceeding anyway for your convenience. The parser is still an HTML parser, so namespace processing is not performed and \u201Cxml:*\u201D attributes are not supported. Using the schema for "
                            + getPresetLabel(XHTML1TRANSITIONAL_SCHEMA)
                            + "."
                            + (html4SpecificAdditionalErrorChecks ? " HTML4-specific tokenization errors are enabled."
                            : ""));
                    validator = validatorByDoctype(XHTML1TRANSITIONAL_SCHEMA);
                } else if ("-//W3C//DTD XHTML 1.0 Strict//EN".equals(publicIdentifier)) {
                    errorHandler.info("XHTML 1.0 Strict doctype seen. Appendix C is not supported. Proceeding anyway for your convenience. The parser is still an HTML parser, so namespace processing is not performed and \u201Cxml:*\u201D attributes are not supported. Using the schema for "
                            + getPresetLabel(XHTML1STRICT_SCHEMA)
                            + "."
                            + (html4SpecificAdditionalErrorChecks ? " HTML4-specific tokenization errors are enabled."
                            : ""));
                    validator = validatorByDoctype(XHTML1STRICT_SCHEMA);
                } else if ("-//W3C//DTD HTML 4.01 Transitional//EN".equals(publicIdentifier)) {
                    errorHandler.info("HTML 4.01 Transitional doctype seen. Using the schema for "
                            + getPresetLabel(XHTML1TRANSITIONAL_SCHEMA)
                            + "."
                            + (html4SpecificAdditionalErrorChecks ? ""
                            : " HTML4-specific tokenization errors are not enabled."));
                    validator = validatorByDoctype(XHTML1TRANSITIONAL_SCHEMA);
                } else if ("-//W3C//DTD HTML 4.01//EN".equals(publicIdentifier)) {
                    errorHandler.info("HTML 4.01 Strict doctype seen. Using the schema for "
                            + getPresetLabel(XHTML1STRICT_SCHEMA)
                            + "."
                            + (html4SpecificAdditionalErrorChecks ? ""
                            : " HTML4-specific tokenization errors are not enabled."));
                    validator = validatorByDoctype(XHTML1STRICT_SCHEMA);
                } else if ("-//W3C//DTD HTML 4.0 Transitional//EN".equals(publicIdentifier)) {
                    errorHandler.info("Legacy HTML 4.0 Transitional doctype seen.  Please consider using HTML 4.01 Transitional instead. Proceeding anyway for your convenience with the schema for "
                            + getPresetLabel(XHTML1TRANSITIONAL_SCHEMA)
                            + "."
                            + (html4SpecificAdditionalErrorChecks ? ""
                            : " HTML4-specific tokenization errors are not enabled."));
                    validator = validatorByDoctype(XHTML1TRANSITIONAL_SCHEMA);
                } else if ("-//W3C//DTD HTML 4.0//EN".equals(publicIdentifier)) {
                    errorHandler.info("Legacy HTML 4.0 Strict doctype seen. Please consider using HTML 4.01 instead. Proceeding anyway for your convenience with the schema for "
                            + getPresetLabel(XHTML1STRICT_SCHEMA)
                            + "."
                            + (html4SpecificAdditionalErrorChecks ? ""
                            : " HTML4-specific tokenization errors are not enabled."));
                    validator = validatorByDoctype(XHTML1STRICT_SCHEMA);
                } else {
                    errorHandler.info("Using the schema for "
                            + getPresetLabel(HTML5_SCHEMA)
                            + "."
                            + (html4SpecificAdditionalErrorChecks ? " HTML4-specific tokenization errors are enabled."
                            : ""));
                    validator = validatorByDoctype(HTML5_SCHEMA);
                }
            } catch (IOException ioe) {
                // At this point the schema comes from memory.
                throw new RuntimeException(ioe);
            } catch (IncorrectSchemaException e) {
                // At this point the schema comes from memory.
                throw new RuntimeException(e);
            }
            ContentHandler ch = validator.getContentHandler();
            ch.setDocumentLocator(htmlParser.getDocumentLocator());
            ch.startDocument();
            reader.setContentHandler(ch);
        } else {
            if (html4SpecificAdditionalErrorChecks) {
                errorHandler.info("HTML4-specific tokenization errors are enabled.");
            }
        }
    }

    private String getPresetLabel(int schemaId) {
        for (int i = 0; i < presetDoctypes.length; i++) {
            if (presetDoctypes[i] == schemaId) {
                return presetLabels[i];
            }
        }
        return "unknown";
    }

    /**
     * @param acceptAllKnownXmlTypes
     * @see nu.validator.xml.ContentTypeParser#setAcceptAllKnownXmlTypes(boolean)
     */
    protected void setAcceptAllKnownXmlTypes(boolean acceptAllKnownXmlTypes) {
//        contentTypeParser.setAcceptAllKnownXmlTypes(acceptAllKnownXmlTypes);
//        dataRes.setAcceptAllKnownXmlTypes(acceptAllKnownXmlTypes);
//        httpRes.setAcceptAllKnownXmlTypes(acceptAllKnownXmlTypes);
    }

    /**
     * @param allowGenericXml
     * @see nu.validator.xml.ContentTypeParser#setAllowGenericXml(boolean)
     */
    protected void setAllowGenericXml(boolean allowGenericXml) {
//        contentTypeParser.setAllowGenericXml(allowGenericXml);
//        httpRes.setAllowGenericXml(allowGenericXml);
//        dataRes.setAllowGenericXml(allowGenericXml);
    }

    /**
     * @param allowHtml
     * @see nu.validator.xml.ContentTypeParser#setAllowHtml(boolean)
     */
    protected void setAllowHtml(boolean allowHtml) {
//        contentTypeParser.setAllowHtml(allowHtml);
//        httpRes.setAllowHtml(allowHtml);
//        dataRes.setAllowHtml(allowHtml);
    }

    /**
     * @param allowRnc
     * @see nu.validator.xml.ContentTypeParser#setAllowRnc(boolean)
     */
    protected void setAllowRnc(boolean allowRnc) {
//        contentTypeParser.setAllowRnc(allowRnc);
//        httpRes.setAllowRnc(allowRnc);
//        dataRes.setAllowRnc(allowRnc);
        entityResolver.setAllowRnc(allowRnc);
    }

    /**
     * @param allowXhtml
     * @see nu.validator.xml.ContentTypeParser#setAllowXhtml(boolean)
     */
    protected void setAllowXhtml(boolean allowXhtml) {
//        contentTypeParser.setAllowXhtml(allowXhtml);
//        httpRes.setAllowXhtml(allowXhtml);
//        dataRes.setAllowXhtml(allowXhtml);
    }

    public void loadDocumentInput(boolean xhtmlContent) {
        assert codeToValidate != null;

        //Aelfred removal workaround - we need to somehow preserve the
        //functionality added by hsivonen - CharacterHandler-s.
        //So for xml we use a patched reader which does more or less the same.
        //for html content the flow remains.
        Reader readerImpl = xhtmlContent
                ? sourceReader = new CharacterHandlerReader(codeToValidate)
                : codeToValidate;

        documentInput = new TypedInputSource(readerImpl);
        documentInput.setType("text/html"); //NOI18N
//        documentInput.setLength(codeToValidate.length());
        documentInput.setEncoding(encoding);
    }

    private String getSchemasForDoctypeId(int schemaId) {
        for (int i = 0; i < presetDoctypes.length; i++) {
            if (presetDoctypes[i] == schemaId) {
                return presetUrls[i];
            }
        }
        return null;
    }

    private static class XMLReaderCreatorImpl implements XMLReaderCreator {

        private ErrorHandler errorHandler;
        private EntityResolver entityResolver;

        public XMLReaderCreatorImpl(ErrorHandler errorHandler, EntityResolver entityResolver) {
            this.errorHandler = errorHandler;
            this.entityResolver = entityResolver;
        }

        public XMLReader createXMLReader() throws SAXException {
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setValidating(false);
                XMLReader r = factory.newSAXParser().getXMLReader();
                r.setFeature("http://xml.org/sax/features/external-general-entities", true); //NOI18N
                r.setFeature("http://xml.org/sax/features/external-parameter-entities", true); //NOI18N
                r.setEntityResolver(this.entityResolver);
                r.setErrorHandler(this.errorHandler);
                return r;
            } catch (ParserConfigurationException ex) {
                throw new SAXException("Cannot create XMLReader instance", ex); //NOI18N
            }

        }
    }

    //xerces's default locator returns slightly shifted positions for character content
    //this affects the LocationRecorder and hence the error positions quite nastily
    private static class XercesInaccurateLocatorWorkaround implements ContentHandler, LexicalHandler {

        //nu.validator.source.LocationRecorder is not accessible
        private ContentHandler contentHandler;
        private LexicalHandler lexicalHandler;
        private LinesMapper mapper;
        private ColumnAdjustingLocator locator;
        private Locator originalLocator;

        public XercesInaccurateLocatorWorkaround(Object source, LinesMapper mapper) {
            this.contentHandler = (ContentHandler) source;
            this.lexicalHandler = (LexicalHandler) source;
            this.mapper = mapper;
        }

        public void setDocumentLocator(Locator locator) {
            this.originalLocator = locator;
            this.locator = new ColumnAdjustingLocator(locator);
            contentHandler.setDocumentLocator(this.locator);
        }

        public void startDocument() throws SAXException {
            contentHandler.startDocument();
        }

        public void endDocument() throws SAXException {
            contentHandler.endDocument();
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            contentHandler.startPrefixMapping(prefix, uri);
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            contentHandler.endPrefixMapping(prefix);
        }

        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            contentHandler.startElement(uri, localName, qName, atts);
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            contentHandler.endElement(uri, localName, qName);
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            assert locator != null;
            int line = originalLocator.getLineNumber();
            int column = originalLocator.getColumnNumber();
            int offset = mapper.getSourceOffsetForLocation(line - 1, column);

            int diff = findBackwardDiff(mapper.getSourceText(), offset, ch, start, length);
            
            locator.setColumnNumberDiff(-diff);
            contentHandler.characters(ch, start, length);
            locator.setColumnNumberDiff(0);
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            contentHandler.ignorableWhitespace(ch, start, length);
        }

        public void processingInstruction(String target, String data) throws SAXException {
            contentHandler.processingInstruction(target, data);
        }

        public void skippedEntity(String name) throws SAXException {
            contentHandler.skippedEntity(name);
        }

        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            lexicalHandler.startDTD(name, publicId, systemId);
        }

        public void endDTD() throws SAXException {
            lexicalHandler.endDTD();
        }

        public void startEntity(String name) throws SAXException {
            lexicalHandler.startEntity(name);
        }

        public void endEntity(String name) throws SAXException {
            lexicalHandler.endEntity(name);
        }

        public void startCDATA() throws SAXException {
            lexicalHandler.startCDATA();
        }

        public void endCDATA() throws SAXException {
            lexicalHandler.endCDATA();
        }

        public void comment(char[] ch, int start, int length) throws SAXException {
            lexicalHandler.comment(ch, start, length);
        }

        private static class ColumnAdjustingLocator implements Locator {

            private Locator delegate;
            private int diff;

            public ColumnAdjustingLocator(Locator delegate) {
                this.delegate = delegate;
            }

            public void setColumnNumberDiff(int diff) {
                this.diff = diff;
            }

            public String getPublicId() {
                return delegate.getPublicId();
            }

            public String getSystemId() {
                return delegate.getSystemId();
            }

            public int getLineNumber() {
                return delegate.getLineNumber();
            }

            public int getColumnNumber() {
                return delegate.getColumnNumber() + diff;
            }
        }
    }

    static int PATTERN_LEN_LIMIT = 10; //consider backward match PATTER_LEN_LIMIT long as OK

    static int findBackwardDiff(CharSequence text, int tlen, char[] pattern, int pstart, int plen) {
        assert text.length() >= tlen;
        assert plen > 0;
        int pend = pstart + plen - 1;
        int limitedpstart = plen - PATTERN_LEN_LIMIT > 0 ? pstart + (plen - PATTERN_LEN_LIMIT) : pstart;
        int pidx = pend;
        int point = tlen;
        boolean inp = false;
        for (int i = tlen - 1; i >= 0; i--) {
            char textChar = text.charAt(i);
            char patternChar = pattern[pidx--];
            if (textChar != patternChar) {
                pidx = pend;
                if (inp) {
                    i = point - 1;
                    inp = false;
                }
                point = i;

            } else {
                if (limitedpstart == pidx + 1) {
                    break; //match, reached start of prefix
                }
                if (pidx == 0) {
                    break;
                }
                inp = true;
            }
        }
        return tlen - point;
    }
    
    /**
     * A Schema instance delegate, the delegated instance if softly reachable so it should 
     * not be GCed so often. If the delegate is GCed a new instance is recreated.
     */
    private static class ProxySchema implements Schema {
    
        private String uri;
        private EntityResolver resolver;
        private PropertyMap pMap;
        
        private SoftReference<Schema> delegateWeakRef;
        
        private ProxySchema(String uri, EntityResolver resolver, PropertyMap pMap) {
            this.uri = uri;
            this.resolver = resolver;
            this.pMap = pMap;
        }

        //exposing just because of some instanceof test used in the code
        private Schema getWrappedSchema() throws SAXException, IOException, IncorrectSchemaException {
            return getSchemaDelegate();
        }
        
        public Validator createValidator(PropertyMap pm) {
            try {
                return getSchemaDelegate().createValidator(pm);
            } catch (Exception ex) { //SAXException, IOException, IncorrectSchemaException
                LOGGER.log(Level.INFO, "Cannot create schema delegate", ex); //NOI18N
            }
            return null;
        }

        public PropertyMap getProperties() {
            try {
                return getSchemaDelegate().getProperties();
            } catch (Exception ex) { //SAXException, IOException, IncorrectSchemaException
                LOGGER.log(Level.INFO, "Cannot create schema delegate", ex); //NOI18N
            }
            return null;
        }
        
        private synchronized Schema getSchemaDelegate() throws SAXException, IOException, IncorrectSchemaException {
            Schema delegate = delegateWeakRef != null ? delegateWeakRef.get() : null;
            if(delegate == null) {
                long a = System.currentTimeMillis();
                delegate = schemaByUrl(uri, resolver, pMap);
                long b = System.currentTimeMillis();
                delegateWeakRef = new SoftReference<Schema>(delegate);
                LOGGER.log(Level.FINE, "Created new Schema instance for {0} in {1}ms.", new Object[]{uri, (b-a)});
            } else {
                LOGGER.log(Level.FINE, "Using cached Schema instance for {0}", uri);
            }
            return delegate;
        }
        
        
    }
    
    private static final class Marker {
        
        private final int hashCode;

        public Marker(int hashCode) {
            this.hashCode = hashCode;
        }
        
        @Override
        public boolean equals(Object o) {
            return o.hashCode() == hashCode();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        
    }
    
}
