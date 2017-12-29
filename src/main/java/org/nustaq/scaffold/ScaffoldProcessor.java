package org.nustaq.scaffold;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class ScaffoldProcessor {

    protected Scaffold conf;
    protected Map<String,String> vars = new HashMap<>();

    public ScaffoldProcessor(String confDir) throws Exception {
        this.conf = Scaffold.read(confDir+File.separator+"scaffold.scuf");
        processRequires(conf.require);
        this.conf.normalize(new File(confDir));
        this.conf.outputBaseDir = injectVars(this.conf.outputBaseDir);
    }

    protected String injectVars(String s) {
        for ( Map.Entry<String,String> en : vars.entrySet() ) {
            s = s.replace(en.getKey(),en.getValue());
        }
        return s;
    }

    protected void processRequires(String[] require) {
        for (int i = 0; i < require.length; i+=3) {
            String var = require[i];
            System.out.println(require[i+1]+" (example: "+require[i+2]+")");
            String val = readin();
            vars.put("{"+var+"}",val);
            vars.put("{"+var+":dashed}",dashed(val));
            vars.put("{"+var+":lower}",val.toLowerCase());
            vars.put("{"+var+":dir}",val.replace('.',File.separatorChar));
            vars.put("{"+var+":pack}",val.replace(File.separatorChar,'.'));
        }
    }

    protected String dashed(String s) {
        StringBuilder res = new StringBuilder(s.length());
        char prev = s.charAt(0);
        for (int i = 0; i < s.length(); i++ ) {
            char ch = s.charAt(i);
            if ( i == 0 ) {
                res.append(Character.toLowerCase(ch));
            } else {
                if (Character.isLowerCase(prev) && Character.isUpperCase(ch))
                    res.append('-');
                res.append(Character.toLowerCase(ch));
            }
            prev = ch;
        }
        return res.toString();
    }

    protected String readin() {
        String res = "";
        try {
            int c;
            while ( (c=System.in.read()) != 10 || res.length() == 0 ) {
                if ( c > 32 )
                    res += (char) (c&0xFF);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    protected void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (f.exists() && !f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }

    protected void processStep(Scaffold conf, Step step) throws IOException {
        Files.walkFileTree(Paths.get(conf.getTemplateBaseDir()), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if ( matches(fileName,step.getIgnoredDirs())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if ( step.getAcceptedDirs().length > 0 && !matches(fileName,step.getAcceptedDirs()) ) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                String absDir = normalizeDirString(file.getParent().toAbsolutePath().toString());
                if ( matches(fileName,step.getIgnoredFiles())) {
//                    System.out.println("IGNORE FILE:"+absDir+" name:"+fileName);
                } else if ( step.getAcceptedFiles().length == 0 || matches(fileName,step.getAcceptedFiles()) ) {
                    String relDir = absDir.substring(conf.getTemplateBaseDir().length());
                    relDir = normalizeDirString(relDir);
                    String outDir = conf.getOutputBaseDir();
                    for ( Map.Entry<String, String> it : step.getDirTransforms().entrySet() ) {
                        if ( matchesDir(relDir, it.getKey()) ) {
                            relDir = relDir.replace(it.getKey(),injectVars(it.getValue()));
                        }
                    }
                    for ( Map.Entry<String, String> it : step.getFileNameTransforms().entrySet() ) {
                        if ( matches(fileName, it.getKey()) ) {
                            fileName = replaceWithPattern(fileName,it.getKey(),injectVars(it.getValue()));
                        }
                    }
                    File outFile = new File(outDir + File.separator + relDir, fileName);
                    byte[] bytes = Files.readAllBytes(file);

                    if ( step.getSrcTransforms().size() > 0 ) {
                        String cont = new String(bytes, "UTF-8");
                        for ( Map.Entry<String,String> en : step.getSrcTransforms().entrySet() ) {
                            cont = cont.replace(en.getKey(),injectVars(en.getValue()));
                        }
                        bytes = cont.getBytes("UTF-8");
                    }

                    outFile.getParentFile().mkdirs();
                    Files.write( outFile.toPath(), bytes);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    protected String replaceWithPattern(String fileName, String key, String value) {
        if ( key.endsWith("*") ) {
            String raw = key.substring(0, key.length() - 1);
            int i = fileName.lastIndexOf(raw);
            return fileName.substring(0,i)+fileName.substring(i).replace(raw,value);
        }
        if ( key.startsWith("*") ) {
            String raw = key.substring(1);
            return fileName.replaceFirst(raw,value);
        }
        return fileName.replace(key,value);
    }

    private String normalizeDirString(String relDir) {
        relDir = replaceSep(relDir);
        if ( ! relDir.endsWith(File.separator) ) {
            relDir += "/";
        }
        return relDir;
    }

    public static String replaceSep(String relDir) {
        relDir = relDir.replace('\\', '/');
        return relDir;
    }

    protected boolean matches(String fileName, String pattern) {
        if ( pattern.endsWith("*") ) {
            return fileName.startsWith(pattern.substring(0,pattern.length()-1));
        }
        if ( pattern.startsWith("*") ) {
            return fileName.endsWith(pattern.substring(1));
        }
        return fileName.equals(pattern);
    }

    protected boolean matchesDir(String fileName, String pattern) {
        return fileName.indexOf(pattern) >= 0;
    }

    protected boolean matches(String fileName, String[] ignoredFiles) {
        for (int i = 0; i < ignoredFiles.length; i++) {
            String ignoredFile = ignoredFiles[i];
            if ( matches( fileName,ignoredFile ) )
                return true;
        }
        return false;
    }

    public void run() throws IOException {
        File out = new File(conf.getOutputBaseDir());

        delete(out);

        for (int i = 0; i < conf.steps.length; i++) {
            Step step = conf.steps[i];
            processStep(conf,step);
        }
        System.out.println("DONE. see "+conf.outputBaseDir);
    }

    public static void main(String[] args) throws Exception {
        if ( args == null || args.length != 1 ) {
            System.out.println("Please specify directory with template and scaffold.scuf file as argument");
            return;
        }
        System.out.println("#######################################################");
        System.out.println("###                                                 ###");
        System.out.println("###              Simple Scaffolder 1.0              ###");
        System.out.println("###                                                 ###");
        System.out.println("#######################################################");
        System.out.println();
        System.out.println();
        System.out.println("Initializing template engine ..");
        Thread.sleep(500 );
        System.out.println("Preprocessing ${wurblon} files ..");
        Thread.sleep(500 );
        System.out.println("Setup TemplateProcessorCompilerFactory ..");
        Thread.sleep(500 );
        System.out.println(".");
        Thread.sleep(500 );
        System.out.println(".");
        Thread.sleep(500 );
        System.out.println(".");
        System.out.println("TADAA ! ");
        System.out.println();
        Scaffold conf = new Scaffold();
        conf.steps = new Step[]{new Step()};
        new ScaffoldProcessor(args[0]).run();
    }
}
