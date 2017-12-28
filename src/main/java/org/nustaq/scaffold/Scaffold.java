package org.nustaq.scaffold;

import org.nustaq.kson.Kson;

import java.io.File;
import java.io.Serializable;

public class Scaffold implements Serializable {

    String templateBaseDir;
    String outputBaseDir = "./output";

    String require[];

    public void normalize(File selfLocationDir) {
        if ( templateBaseDir == null ) {
            templateBaseDir = selfLocationDir.getAbsolutePath();
        }
        templateBaseDir = new File(templateBaseDir).getAbsolutePath();
        outputBaseDir = new File(outputBaseDir).getAbsolutePath();
    }

    Step steps[] = {};

    public String getTemplateBaseDir() {
        return templateBaseDir;
    }

    public String getOutputBaseDir() {
        return outputBaseDir;
    }

    public static Scaffold read(String path) throws Exception {
        return (Scaffold) new Kson().map(Scaffold.class,Step.class).readObject(new File(path));
    }

    public static void main(String[] args) throws Exception {
        Scaffold s = new Scaffold();
        s.steps = new Step[]{ new Step(), new Step()};
        System.out.println(new Kson().map(Scaffold.class,Step.class).writeObject(s));
    }

}
