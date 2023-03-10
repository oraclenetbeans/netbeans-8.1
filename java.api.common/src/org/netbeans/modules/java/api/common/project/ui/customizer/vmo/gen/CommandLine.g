grammar CommandLine;

options {
	language=Java;
	output = AST;
	ASTLabelType = CommonTree;
}

@lexer::header {
package org.netbeans.modules.java.j2seproject.ui.customizer.vmo.gen;
import java.util.Queue;
import java.util.LinkedList;
}

@lexer::members {

    private final Queue<Token> tokens = new LinkedList<Token>();

    @Override
    public void recover(final RecognitionException re) {	
        input.seek(state.tokenStartCharIndex);
        input.setLine(state.tokenStartLine);
        input.setCharPositionInLine(state.tokenStartCharPositionInLine);
        state.type = TEXT;
        state.token = null;
        state.channel = Token.DEFAULT_CHANNEL;
        state.tokenStartCharIndex = input.index();
        state.tokenStartCharPositionInLine = input.getCharPositionInLine();
        state.tokenStartLine = input.getLine();
        state.text = null;
        //read upto white space and emmit as TEXT, todo: specail ERROR token should be better
        while (!((input.LA(1)>='\t' && input.LA(1)<='\n')||(input.LA(1)>='\f' && input.LA(1)<='\r')||input.LA(1)==' '||input.LA(1) == EOF)) {                
            input.consume();
        }
        tokens.add(emit());
    }
    
    @Override
    public Token nextToken() {
    	tokens.add(super.nextToken());
    	return tokens.poll();
    }
}

@parser::header {
package org.netbeans.modules.java.j2seproject.ui.customizer.vmo.gen;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.netbeans.modules.java.j2seproject.ui.customizer.vmo.*;
}

@parser::members {

	private static final String SERVER = "server";
	private static final String CLIENT = "client";
	private static final String ESA = "esa";
	private static final String ESA_LONG = "enablesystemassertions";
	private static final String DSA = "dsa";
	private static final String DSA_LONG = "disablesystemassertions";
	private static final String EA = "ea";
	private static final String EA_LONG = "enableassertions";
	private static final String DA = "da";
	private static final String DA_LONG = "disableassertions";
	private static final String VERBOSE = "verbose";
	private static final String SVERSION = "showversion";
	private static final String HELP = "?";
	private static final String HELP_LONG = "help";
	private static final String X = "X";
	private static final String XINT = "Xint";
	private static final String XBATCH = "Xbatch";
	private static final String XCJNI = "Xcheck";
	private static final String XFUTURE = "Xfuture";
	private static final String XNOCLSGC = "Xnoclassgc";
	private static final String XINCGC = "Xincgc";
	private static final String XPROF = "Xprof";
	private static final String XRS = "Xrs";
	private static final String XSHARE = "Xshare";
	private static final String JRE_SEARCH="jre-restrict-search";
	private static final String JRE_NO_SEARCH="jre-no-restrict-search";
	private static final String SPLASH = "splash";
	private static final String XLOGGC = "Xloggc";
	private static final String JAVAAGENT = "javaagent";
	private static final String AGENTLIB = "agentlib";
	private static final String AGENTPATH = "agentpath";
	private static final String BOOTCP = "Xbootclasspath";
	private static final String BOOTCPAPPEND = "Xbootclasspath/a";
	private static final String BOOTCPPREPEND = "Xbootclasspath/p";
	private static final String VERSION = "version";
	private static final String CLASSPATH = "cp";
	private static final String CLASSPATH_LONG = "classpath";

	private static final Set<String> switchOptions = new HashSet<String>() {
	    {
	    	this.addAll(Arrays.asList(
	    	SERVER,
	    	CLIENT,
	    	ESA,
	    	ESA_LONG,
	    	DSA,
	    	DSA_LONG,
	    	EA,
	    	EA_LONG,
	    	DA,
	    	DA_LONG,
	    	SVERSION,
	    	HELP,
	    	HELP_LONG,
	    	X,
	    	XINT,
	    	XBATCH,
	    	XFUTURE,
	    	XNOCLSGC,
	    	XINCGC,
	    	XPROF,
	    	XRS,
	    	JRE_SEARCH,
	    	JRE_NO_SEARCH));
	    }
	};
	
	private static final Set<String> paramOptions = new HashSet<String>(){
	    {
	        addAll(Arrays.asList(
	        SPLASH,
	        XLOGGC,
	        JAVAAGENT,
	        AGENTLIB,
	        AGENTPATH,
	        BOOTCP,
	        BOOTCPAPPEND,
	        BOOTCPPREPEND
	        ));
	    }
	};
	
	private static final Pattern memOptions = Pattern.compile("X(m[sx]|ss)\\d+[gGmMkK]");
	
	private static boolean isParamOption(final String text) {
		for (String option : paramOptions) {
		    if (text.startsWith(option+':')) {
		    	return true;
		    }
		}
		return false;
	}
	
	//xxx: Wrong! Should use TreeGrammer and not to populate customizer with custom nodes
	//Should be rewritten but I have no time for this
	public List<JavaVMOption<?>> parse() {
        Set<JavaVMOption<?>> result = new HashSet<JavaVMOption<?>>(); 
        try {
            vmOptions_return options_return = vmOptions();
            CommonTree root = options_return.tree;
            if (root instanceof JavaVMOption<?>) {
                result.add((JavaVMOption<?>) root);
            } else if (root != null) {
                result.addAll(root.getChildren());
            }                                       
        } catch (RecognitionException e) {
            e.printStackTrace();
        }
        result.addAll(getAllOptions());
        return new LinkedList<JavaVMOption<?>>(result); 
    }


    private static enum Kind {
        SWITCH, D, LOOSEPARAM, EQPARAM, COLUMNPARAM, FOLLOWED
    }


    private static class OptionDefinition {
        private OptionDefinition(String name, Kind kind) {
            this.kind = kind;
            this.name = name;
        }

        Kind kind;
        String name;
    }

    private static OptionDefinition[] optionsTemplates = {
            new OptionDefinition("client", Kind.SWITCH),
            new OptionDefinition("server", Kind.SWITCH),
            new OptionDefinition("ea", Kind.SWITCH),
            new OptionDefinition("da", Kind.SWITCH),
            new OptionDefinition("esa", Kind.SWITCH),
            new OptionDefinition("dsa", Kind.SWITCH),
            new OptionDefinition("verbose", Kind.SWITCH),
            new OptionDefinition("verbose:class", Kind.SWITCH),
            new OptionDefinition("verbose:jni", Kind.SWITCH),
            new OptionDefinition("verbose:gc", Kind.SWITCH),
            new OptionDefinition("version", Kind.SWITCH),
            new OptionDefinition("version", Kind.COLUMNPARAM),
            new OptionDefinition("showversion", Kind.SWITCH),
            new OptionDefinition("Xint", Kind.SWITCH),
            new OptionDefinition("Xbatch", Kind.SWITCH),
            new OptionDefinition("Xcheck:jni", Kind.SWITCH),
            new OptionDefinition("Xfuture", Kind.SWITCH),
            new OptionDefinition("Xnoclassgc", Kind.SWITCH),
            new OptionDefinition("Xincgc", Kind.SWITCH),
            new OptionDefinition("Xprof", Kind.SWITCH),
            new OptionDefinition("Xrs", Kind.SWITCH),
            new OptionDefinition("Xshare:off", Kind.SWITCH),
            new OptionDefinition("Xshare:on", Kind.SWITCH),
            new OptionDefinition("Xshare:auto", Kind.SWITCH),
            new OptionDefinition("jre-restrict-search", Kind.SWITCH),
            new OptionDefinition("jre-no-restrict-search", Kind.SWITCH),
            new OptionDefinition("Xmx", Kind.FOLLOWED),
            new OptionDefinition("Xms", Kind.FOLLOWED),
            new OptionDefinition("Xss", Kind.FOLLOWED),
            new OptionDefinition("splash", Kind.COLUMNPARAM),
            new OptionDefinition("javaagent", Kind.COLUMNPARAM),
            new OptionDefinition("agentlib", Kind.COLUMNPARAM),
            new OptionDefinition("agentpath", Kind.COLUMNPARAM),
    };

    public static List<JavaVMOption<?>> getAllOptions() {
        List<JavaVMOption<?>> result = new LinkedList<JavaVMOption<?>>();
        for (OptionDefinition optionsTemplate : optionsTemplates) {
            result.add(createOption(optionsTemplate));
        }
        return result;
    }

    private static JavaVMOption<?> createOption(OptionDefinition definition) {
        switch (definition.kind) {
            case SWITCH:
                return new SwitchNode(definition.name);
            case D:
                return new UserPropertyNode();
            case FOLLOWED:
                return new ParametrizedNode(definition.name, "");
            case COLUMNPARAM:
                return new ParametrizedNode(definition.name, ":");
            case EQPARAM:
                return new ParametrizedNode(definition.name, "=");
            case LOOSEPARAM:
                return new ParametrizedNode(definition.name, " ");
            default:
                throw new IllegalArgumentException("Invalid definition.");
        }
    }
}

vmOptions
	:	(WS?option)*WS? -> option*;
	
option	:	'-' switchOption -> switchOption |
		nonSwitchOption  -> nonSwitchOption;
		  
switchOption
@init {
	int index = 0;
	String name = null;
	String value = null;
}
	:	{switchOptions.contains(input.LT(1).getText())}?=> t=TEXT         				   	-> {new SwitchNode($t)} |
		{VERBOSE.equals(input.LT(1).getText()) || input.LT(1).getText().startsWith(VERBOSE+':')}?=> t=TEXT     	-> {new SwitchNode($t)} |
		{VERSION.equals(input.LT(1).getText()) || input.LT(1).getText().startsWith(VERSION+':')}?=> t=TEXT {index = $t.getText().indexOf(':'); if (index > 0) {name=$t.getText().substring(0,index); value = (index+1) == $t.getText().length() ? "" : $t.getText().substring(index+1);} else {name=$t.getText();} } -> { index < 0 ? new SwitchNode($t) : new ParametrizedNode($t, name, ":", value)} |
		{input.LT(1).getText().startsWith(XSHARE+':')}?=> t=TEXT -> {new SwitchNode($t)} |
		{input.LT(1).getText().startsWith(XCJNI+':')}?=> t=TEXT	  -> {new SwitchNode($t)} |
		{input.LT(1).getText().charAt(0) == 'D'}?=> t=TEXT '=' eText    -> {new UserPropertyNode($t, $eText.text, $t.pos)} |
		{isParamOption(input.LT(1).getText())}?=> t=TEXT {index = $t.getText().indexOf(':'); if (index > 0) {name=$t.getText().substring(0,index); value = (index+1) == $t.getText().length() ? "" : $t.getText().substring(index+1);}} -> {new ParametrizedNode($t, name, ":", value)} |
		{memOptions.matcher(input.LT(1).getText()).matches()}?=> t=TEXT	  -> {new ParametrizedNode($t, 3)} |
		{CLASSPATH.equals(input.LT(1).getText()) || CLASSPATH_LONG.equals(input.LT(1).getText())}?=> t=TEXT WS eText -> {new ParametrizedNode($t, " ", $eText.text, false)} |
		t=TEXT -> {new UnrecognizedOption($t)};
		
eText	:	
		'\'' TEXT '\''
	|	'"' TEXT  '"'
	|	    TEXT;
	
nonSwitchOption
	:	t=TEXT -> {new UnknownOption($t)};
		
WS	:	(' '|'\r'|'\t'|'\u000C'|'\n')+;

TEXT	:	LETTER (LETTER|'-'|';'|':'|'{'|'}')*;

fragment
LETTER
    :  '\u0021' |        
       '\u0023'..'\u0026' |       
       '\u002b' |
       '\u002e'..'\u0039' |
       '\u0041'..'\u005a' |       
       '\u005c' |
       '\u005f' |
       '\u0061'..'\u007a' |
       '\u007e' |
       '\u00c0'..'\u00d6' |
       '\u00d8'..'\u00f6' |
       '\u00f8'..'\u00ff' |
       '\u0100'..'\u1fff' |
       '\u3040'..'\u318f' |
       '\u3300'..'\u337f' |
       '\u3400'..'\u3d2d' |
       '\u4e00'..'\u9fff' |
       '\uf900'..'\ufaff'
    ;