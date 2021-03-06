/*
 * Copyright (c) 2014, Dries007
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the project nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package compiler;

import JavaEasembly.Main;
import compiler.components.Component;
import compiler.components.SrcComponent;
import util.exceptions.IncludeException;
import util.exceptions.PreprocessorException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static util.Constants.*;

/**
 * Warning: Regex madness ahead.
 *
 * @author Dries007
 */
public class Preprocessor {

    private Preprocessor() {
    }

    public static void process(LinkedList<Component> components, String srcText, Map<String, String> includeFiles) throws PreprocessorException, IOException {
        Matcher matcher;
        String[] split = srcText.split("\\n");
        for (int i = 0; i < split.length; i++) {
            int comment = split[i].indexOf(PREFIX_COMMENT);
            if (comment != -1) {
                split[i] = split[i].substring(0, comment);
            }
            split[i] = split[i].trim();
            if (!split[i].isEmpty()) {
                components.add(new SrcComponent(i, split[i]));
            }
        }

        HashMap<String, Macro> symbols = new HashMap<>();
        LinkedList<Boolean> ifList = new LinkedList<>();

        ListIterator<Component> i = components.listIterator();
        while (i.hasNext()) {
            String src = ((SrcComponent) i.next()).contents;
            if (src.isEmpty()) {
                continue;
            }
            if (src.charAt(0) == PREFIX_PRECOMPILER) // Initial check is fast
            {
                matcher = INCLUDE_A.matcher(src);
                if (matcher.matches()) {
                    i.remove();
                    include(i, new File(matcher.group(1)), includeFiles);
                    continue;
                }
                matcher = INCLUDE_R.matcher(src);
                if (matcher.matches()) {
                    i.remove();
                    include(i, new File(Main.includeFile, matcher.group(1)), includeFiles);
                    continue;
                }
            }
        }
        i = components.listIterator();
        while (i.hasNext()) {
            SrcComponent component = (SrcComponent) i.next();
            i.remove();
            final int line = component.getSrcLine();
            String src = component.contents;
            if (src.isEmpty()) {
                continue;
            }
            if (src.charAt(0) == PREFIX_PRECOMPILER) // Initial check is fast
            {
                matcher = DEFINE.matcher(src);
                if (matcher.matches()) {
                    if (symbols.containsKey(matcher.group(1))) {
                        throw new PreprocessorException(null, matcher.group(1) + " already defined.");
                    }
                    symbols.put(matcher.group(1), new Macro(matcher, i));
                    continue;
                }
                matcher = UNDEFINE.matcher(src);
                if (matcher.matches()) {
                    symbols.remove(matcher.group(1));
                    continue;
                }
                matcher = IFDEF.matcher(src);
                if (matcher.matches()) {
                    ifList.add(symbols.containsKey(matcher.group(1)));
                    continue;
                }
                matcher = IFNDEF.matcher(src);
                if (matcher.matches()) {
                    ifList.add(!symbols.containsKey(matcher.group(1)));
                    continue;
                }
                matcher = ELSE.matcher(src);
                if (matcher.matches()) {
                    ifList.add(!ifList.removeLast());
                    continue;
                }
                matcher = ENDIF.matcher(src);
                if (matcher.matches()) {
                    ifList.removeLast();
                    continue;
                }
            }
            boolean changes;
            do {
                changes = false;
                for (String key : symbols.keySet()) {
                    if (src.contains(key)) {
                        String oldLine = src;
                        src = symbols.get(key).acton(src);
                        if (!oldLine.equals(src)) {
                            changes = true;
                        }
                    }
                }
            } while (changes);
            i.add(new SrcComponent(line, replaceAcsii(src)));
        }
    }

    private static String replaceAcsii(String src) {
        Matcher matcher;
        while ((matcher = CHAR.matcher(src)).find()) {
            src = matcher.replaceFirst(Integer.toHexString((int) matcher.group(1).charAt(0)) + "h");
        }
        while ((matcher = STRING.matcher(src)).find()) {
            char[] chars = matcher.group(1).toCharArray();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < chars.length; i++) {
                stringBuilder.append(Integer.toHexString((int) chars[i])).append('h');
                if (i + 1 < chars.length) {
                    stringBuilder.append(", ");
                }
            }
            src = matcher.replaceFirst(stringBuilder.toString());
        }
        return src;
    }

    private static void include(ListIterator<Component> components, File file, Map<String, String> includeFiles) throws IncludeException, IOException {
        String text = FileUtils.readFileToString(file, PROPERTIES.getProperty(ENCODING, ENCODING_DEFAULT)).replaceAll("\\r\\n", "\n");
        includeFiles.put(FilenameUtils.getBaseName(file.getName()), text);
        String split[] = text.split("\\n");
        for (int i = 0; i < split.length; i++) {
            int comment = split[i].indexOf(PREFIX_COMMENT);
            if (comment != -1) {
                split[i] = split[i].substring(0, comment);
            }
            split[i] = split[i].trim();
            if (!split[i].isEmpty()) {
                components.add(new SrcComponent(i, split[i]));
            }
        }
    }

    /**
     * Warning: Regex madness ahead.
     *
     * @author Dries007
     */
    public static class Macro {

        private String name;
        private String[] args;
        private String text;
        private Pattern pattern;

        public Macro(Matcher matcher, ListIterator<Component> iterator) {
            name = matcher.group(1);
            args = matcher.group(2) != null ? matcher.group(2).split(", ?") : null;
            text = matcher.group(3);
            while (text.charAt(text.length() - 1) == '\\') {
                text = text.substring(0, text.length() - 1) + " " + iterator.next();
            }
            if (args != null) {
                StringBuilder patternBuilder = new StringBuilder();
                patternBuilder.append(name).append("\\(");
                for (int i = 0; i < args.length; i++) {
                    patternBuilder.append("([^,]+?)");
                    if (i != args.length - 1) {
                        patternBuilder.append(", ?");
                    }
                }
                patternBuilder.append("\\)");
                pattern = Pattern.compile(patternBuilder.toString());
            }
        }

        public String acton(String line) {
            if (args == null) {
                return line.replace(name, text);
            }
            Matcher matcher = pattern.matcher(line);
            if (!matcher.find()) {
                return line;
            }
            String replacement = text;
            for (int i = 0; i < args.length; i++) {
                replacement = replacement.replace(args[i], matcher.group(i + 1));
            }
            return matcher.replaceFirst(replacement);
        }
    }
}
