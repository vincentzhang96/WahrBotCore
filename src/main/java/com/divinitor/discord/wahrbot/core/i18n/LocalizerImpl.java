package com.divinitor.discord.wahrbot.core.i18n;

import java.util.*;
import java.util.function.Supplier;

public class LocalizerImpl implements Localizer {

    private final int maxRepeatCount;
    private Locale defaultLocale;
    private Map<String, LocalizerBundle> bundles;
    private Map<String, LocalizerPluralRule> rules;

    public LocalizerImpl() {
        this(Locale.getDefault());
    }

    public LocalizerImpl(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
        this.bundles = new HashMap<>();
        this.rules = new HashMap<>();
        this.maxRepeatCount = 10;
        this.registerPluralityRules(Localizer.defaultPluralityRules());
    }

    @Override
    public String localizeToLocale(String key, Locale locale, Object... args) {
        if (key.startsWith(PREFIX_DO_NOT_RESOLVE)) {
            return key.substring(PREFIX_DO_NOT_RESOLVE.length());
        }

        return doLocalize(0, key, locale, args);
    }

    private String doLocalize(int depth, String key, Locale locale, Object... args) {
        //  Only strip escapes at depth = 0!!!
//  Formatting is done by repeatedly resolving curly brace tokens and then square bracket tokens repeatedly
        //  until no more of either remain in the resultant string
        //  There is a maximum repeat limit to prevent infinite loops or unbounded string
        //  growth, governed by the system property co.phoenixlab.localizer.fmt.limits.repeat

        //  Number of times we've passed over the entire string
        int repeatCount = 0;
        //  Our current working string
        String working = this.lookup(key, locale);
        if (working == null) {
            return Localizer.NO_SUCH_KEY + key;
        }

        //  StringBuilder for building the resultant for each pass
        StringBuilder builder = new StringBuilder();
        //  Whether or not any substitution was made
        boolean substitution = true;
        while (repeatCount < maxRepeatCount && substitution) {
            try {
                builder.setLength(0);
                //  Resolve curly brace tokens first
                char[] chars = working.toCharArray();
                substitution = processCurlyTokens(builder, chars, args);

                //  Now resolve square bracket tags
                //  Reset
                int len = builder.length();
                if (len == chars.length) {
                    builder.getChars(0, len, chars, 0);
                } else {
                    chars = builder.toString().toCharArray();
                }
                builder.setLength(0);
                substitution |= processSquareBracketTokens(depth, builder, chars, key, locale, args);
                //  Prep for next iteration
                working = builder.toString();
                repeatCount++;
            } catch (IllegalArgumentException e) {
                return Localizer.BAD_FORMAT_STRING + e.getMessage();
            }
        }
        //  Unescape escaped strings
        if (depth == 0) {
            builder.setLength(0);
            char[] chars = working.toCharArray();
            boolean escaped = false;
            for (char c : chars) {
                if (escaped) {
                    builder.append(c);
                    escaped = false;
                } else {
                    if (c == '\\') {
                        escaped = true;
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        return builder.toString();
    }


    private boolean processCurlyTokens(StringBuilder builder, char[] chars, Object[] args) {
        boolean hasSubstitutionBeenMade = false;
        StringBuilder tokenBuilder = new StringBuilder();
        boolean isNextCharEscaped = false;
        boolean isInTag = false;
        int braceDepth = 0;
        for (char c : chars) {
            if (isNextCharEscaped) {
                if (isInTag) {
                    tokenBuilder.append(c);
                } else {
                    builder.append(c);
                }
                isNextCharEscaped = false;
            } else {
                switch (c) {
                    case '\\':
                        //  Escape next character
                        isNextCharEscaped = true;
                        //  Add since we unescape characters on final pass
                        if (isInTag) {
                            tokenBuilder.append(c);
                        } else {
                            builder.append(c);
                        }
                        break;
                    case '{':
                        //  Start curly brace tag
                        isInTag = true;
                        braceDepth++;
                        //  Reset our token StringBuilder
                        tokenBuilder.setLength(0);
                        break;
                    case '}':
                        if (isInTag) {
                            braceDepth--;
                            if (braceDepth == 0) {
                                builder.append(processCurlyBraceToken(tokenBuilder.toString(), args));
                                isInTag = false;
                                hasSubstitutionBeenMade = true;
                                break;
                            }
                            //  FALL THROUGH if not outmoster layer of tag
                        }
                        //  FALL THROUGH if not in curly tag
                    default:
                        if (isInTag) {
                            tokenBuilder.append(c);
                        } else {
                            builder.append(c);
                        }
                }
            }
        }
        if (isNextCharEscaped) {
            //  Backslash at end of string, not fatal so we just insert it
            builder.append('\\');
        }
        if (braceDepth > 0) {
            //  There's an unclosed tag somewhere
            throw new IllegalArgumentException();
        }
        return hasSubstitutionBeenMade;
    }

    private boolean processSquareBracketTokens(int depth, StringBuilder builder, char[] chars, String key, Locale locale, Object... args) {
        boolean hasSubstitutionBeenMade = false;
        StringBuilder tokenBuilder = new StringBuilder();
        boolean isNextCharEscaped = false;
        boolean isInTag = false;
        for (char c : chars) {
            if (isNextCharEscaped) {
                if (isInTag) {
                    tokenBuilder.append(c);
                } else {
                    builder.append(c);
                }
                isNextCharEscaped = false;
            } else {
                switch (c) {
                    case '\\':
                        //  Escape next character
                        isNextCharEscaped = true;
                        //  Add since we unescape characters on final pass
                        if (isInTag) {
                            tokenBuilder.append(c);
                        } else {
                            builder.append(c);
                        }
                        break;
                    case '[':
                        //  Start square bracket tag
                        isInTag = true;
                        //  Reset our token StringBuilder
                        tokenBuilder.setLength(0);
                        break;
                    case ']':
                        if (isInTag) {
                            builder.append(resolveSubkey(depth, key, tokenBuilder.toString(), locale, args));
                            isInTag = false;
                            hasSubstitutionBeenMade = true;
                            break;
                        }
                        //  FALL THROUGH if not in square tag
                    default:
                        if (isInTag) {
                            tokenBuilder.append(c);
                        } else {
                            builder.append(c);
                        }
                }
            }
        }
        if (isNextCharEscaped) {
            //  Backslash at end of string, not fatal so we just insert it
            builder.append('\\');
        }
        if (isInTag) {
            //  There's an unclosed tag somewhere
            throw new IllegalArgumentException();
        }
        return hasSubstitutionBeenMade;
    }

    @SuppressWarnings("unchecked")
    private String processCurlyBraceToken(String tokenContents, Object[] args) {
        /*
        Format:
        ARG_NUMBER|FORMAT_DESCRIPTOR
        ARG_NUMBER: The argument index to use
        FORMAT_DESCRIPTOR: The way the argument should be formatted when inserted into the string
        FORMAT_DESCRIPTOR format:
        %FORMAT_STRING: Standard String.format() format string
        #date[|DATE_FORMAT_STRING]: Formats the argument as a date, using the locale default short format if
            DATE_FORMAT_STRING is not provided
        #time[|TIME_FORMAT_STRING]: Formats the argument as a time, using the locale default short format if
            TIME_FORMAT_STRING is not provided
        #datetime[|DATE_TIME_FORMAT_STRING]: Formats the argument as a date and time, using the default short
            format if DATE_TIME_FORMAT_STRING is not provided
        (PLURALITY_ID1,PLURALITY_ID2,...;TEXT)[,more...]: A list of plurality matchers, using the given argument as the number.
        Plurality rules are evaluated left to right; whichever rule matches first will be used
         */
        String[] splits = tokenContents.split("\\|", 2);

        final Object arg;
        if (splits[0].startsWith("%")) {
            String namedArgName = splits[0].substring(1, splits[0].length() - 1);
            if (args.length == 0) {
                return Localizer.BAD_NAMED_ARG + splits[0] + ":NOARGS";
            }

            Object last = args[args.length - 1];
            if (last instanceof Map) {
                Map<String, Object> named = (Map<String, Object>) last;
                Object val = named.get(namedArgName);
                if (val == null) {
                    return Localizer.BAD_NAMED_ARG + splits[0] + ":VALNULL";
                }

                if (val instanceof Supplier) {
                    arg = ((Supplier) val).get();
                } else {
                    arg = val;
                }
            } else {
                return Localizer.BAD_NAMED_ARG + splits[0] + ":NOTMAP";
            }
        } else {

            int argId;
            try {
                argId = Integer.parseInt(splits[0]);
                if (argId < 0 || argId >= args.length) {
                    return Localizer.BAD_ARG_INDEX + splits[0];
                }
            } catch (NumberFormatException e) {
                return Localizer.BAD_ARG_INDEX + splits[0];
            }
            arg = args[argId];
        }

        if (splits.length == 1) {
            return String.valueOf(arg);
        }

        String formatDescriptor = splits[1];
        char first = formatDescriptor.charAt(0);
        switch (first) {
            case '%':
                return handleStringFormat(formatDescriptor, arg);
            case '#':
                return handleDateTimeFormat(formatDescriptor, arg);
            case '(':
                return handlePluralityRules(formatDescriptor, arg);
            default:
                return Localizer.BAD_ARG + formatDescriptor;
        }
    }

    private String handleStringFormat(String fmt, Object arg) {
        try {
            return String.format(fmt, arg);
        } catch (IllegalFormatException e) {
            return Localizer.BAD_FORMAT_STRING + fmt;
        }
    }

    private String handleDateTimeFormat(String fmt, Object arg) {
        return null;
    }

    private String handlePluralityRules(String rules, Object arg) {
        //  To make things efficient we'll walk forward through the rules string
        //  We take in the plurality ID first, see if it matches. If it does match, we'll then
        //  return the associated text body
        //  If it does not match, we skip ahead to the next rule and repeat
        //  If no rules match, then we return NO_MATCHING_PLURAL
        //  Example rule: (ONE;a potato),(ZERO,MANY;potatoes)
        //  FooBar rule (assuming defined matchers): (FOUR+FIVE;foobar),(FOUR;foo),(FIVE;bar)
        //  Rules can be ANDed together with + to only match if BOTH rules match.
        //  TODO

        //  First off, make sure what we have IS a number
        if (!(arg instanceof Number)) {
            return BAD_ARG + arg.getClass();
        }
        Number number = (Number) arg;
        char[] chars = rules.toCharArray();
        StringBuilder builder = new StringBuilder();
        int startIndex = 0;
        do {
            builder.setLength(0);
            //  Read in block
            int newIndex = readInPluralityRule(chars, startIndex, builder);
            if (newIndex == startIndex) {
                break;
            }
            startIndex = newIndex;
            String rule = builder.toString();
            if (rule.isEmpty()) {
                continue;
            }
            //  Split at the semicolon, but only the first one we run into
            //  We don't perform lookaround to exclude escaped semicolons since matcher names cannot include semicolons
            String[] split = rule.split(";", 2);
            if (split.length != 2) {
                //  Bad rule - rules must have at least one matcher and text
                continue;
            }
            //  Don't bother with commas either
            String[] matchers = split[0].split(",");
            boolean match = false;
            for (String matcher : matchers) {
                LocalizerPluralRule pluralRule = null;
                //  Fast path
                if (!matcher.contains("+")) {
                    pluralRule = getRule(matcher);
                } else {
                    String[] sub = matcher.split("\\+");
                    pluralRule = LocalizerPluralRule.TRUE();
                    for (String s : sub) {
                        LocalizerPluralRule r = getRule(s);
                        if (r != null) {
                            pluralRule = pluralRule.and(r);
                        }
                    }
                }
                if (pluralRule != null) {
                    match = pluralRule.test(number);
                    if (match) {
                        break;
                    }
                }
            }
            if (match) {
                return split[1];
            }
        } while (startIndex < chars.length);
        return Localizer.NO_MATCHING_PLURAL + number.toString();
    }

    private LocalizerPluralRule getRule(String name) {
        return rules.get(name.toUpperCase());
    }

    private int readInPluralityRule(char[] chars, int index, StringBuilder builder) {
        //  Find opening paren (non escaped)
        boolean escaped = false;
        boolean foundOpen = false;
        for (; index < chars.length; index++) {
            char c = chars[index];
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '(') {
                foundOpen = true;
                index++;    //  Skip the paren for inclusion
                break;
            }
        }
        if (!foundOpen) {
            return index;
        }
        int startPos = index;
        if (startPos >= chars.length) {
            return index;
        }
        //  Find closing paren
        escaped = false;
        for(; index < chars.length; index++) {
            char c = chars[index];
            if (escaped)  {
                escaped = false;
                builder.append(c);
                continue;
            }
            if (c == '\\') {
                escaped = true;
                //  Still need to append
                builder.append(c);
                continue;
            }
            if (c == ')') {
                break;
            }
            builder.append(c);
        }
        return index;
    }

    private String resolveSubkey(int depth, String baseKey, String tokenContents, Locale locale, Object... args) {
        if (tokenContents.length() == 0) {
            return Localizer.NO_SUCH_KEY + tokenContents;
        }
        boolean isRelative = tokenContents.charAt(0) == '.';    //  tokenContents.startsWith(".")
        String key;
        if (isRelative) {
            if (tokenContents.equals("..")) {
                //  go up one level
                int last = baseKey.lastIndexOf('.');
                if (last == -1) {
                    return Localizer.BAD_REFERENCE + baseKey + tokenContents;
                }
                key = baseKey.substring(0, last);
            } else {
                key = baseKey + tokenContents;
            }
        } else {
            key = tokenContents;
        }
        String ret = doLocalize(depth + 1, key, locale, args);
        if (ret != null) {
            return ret;
        }
        return Localizer.NO_SUCH_KEY + key;
    }

    private String lookup(String key, Locale locale) {
        for (LocalizerBundle bundle : this.bundles.values()) {
            String s = bundle.get(key, locale);
            if (s != null) {
                return s;
            }
        }
        return null;
    }


    @Override
    public Locale getDefaultLocale() {
        return this.defaultLocale;
    }

    @Override
    public boolean contains(String key) {
        for (LocalizerBundle bundle : bundles.values()) {
            if (bundle.contains(key, this.defaultLocale)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void setDefaultLocale(Locale locale) {
        this.defaultLocale = locale;
    }

    @Override
    public void registerBundle(String bundleKey, LocalizerBundle bundle) {
        this.bundles.put(bundleKey, bundle);
    }

    @Override
    public void unregisterBundle(String bundleKey) {
        this.bundles.remove(bundleKey);
    }

    @Override
    public void registerPluralityRules(Map<String, LocalizerPluralRule> rules) {
        rules.forEach((k, v) -> this.rules.put(k.toUpperCase(), v));
    }

    @Override
    public Map<String, LocalizerPluralRule> getPluralRules() {
        return this.rules;
    }
}
