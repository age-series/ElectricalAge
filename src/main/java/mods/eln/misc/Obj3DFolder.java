package mods.eln.misc;

import mods.eln.misc.Obj3D.Obj3DPart;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class used to load all eln models and corresponding obj files.
 */
public class Obj3DFolder {

    private Map<String, Obj3D> nameToObjHash = new HashMap<String, Obj3D>();
    private final Obj3D missingObj = new Obj3D();
    private final Set<String> missingObjNamesWarned = new HashSet<String>();

    /**
     * Load all obj models available in the release mod asset folder.
     */
    public void loadAllElnModels() {
        try {
            // Find location of electrical age jar file.
            CodeSource codeSource = Obj3DFolder.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                File location = codeSourceLocationToFile(codeSource.getLocation().toString());
                if (location.isFile()) {
                    try (JarFile jarFile = new JarFile(location)) {
                        Enumeration<JarEntry> entries = jarFile.entries();
                        int modelCount = 0;
                        while (entries.hasMoreElements()) {
                            String filename = entries.nextElement().getName();
                            if (filename.startsWith("assets/eln/model/") && filename.toLowerCase().endsWith(".obj")) {
                                filename = filename.substring(filename.indexOf("/model/") + 7, filename.length());
                                Utils.println(String.format("Loading model %03d '%s'", ++modelCount, filename));
                                loadObj(filename);
                            }
                        }
                    }
                } else {
                    Integer modelCount = 0;
                    File modelFolder = new File(mods.eln.Eln.class.getResource("/assets/eln/model").toURI());
                    if (modelFolder.isDirectory()) {
                        loadModelsRecursive(modelFolder, modelCount);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    static File codeSourceLocationToFile(String locationString) throws URISyntaxException {
        String uriString = locationString;
        if (uriString.startsWith("jar:")) {
            int bangIndex = uriString.indexOf("!");
            if (bangIndex >= 0) {
                uriString = uriString.substring(0, bangIndex);
            }
            uriString = uriString.substring(4);
        }
        return new File(new URI(uriString));
    }

    private void loadModelsRecursive(File folder, Integer modelCount) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                loadModelsRecursive(file, modelCount);
            } else if (file.getName().toLowerCase().endsWith(".obj")) {
                String filename = file.getPath().replaceAll("\\\\", "/");
                filename = filename.substring(filename.indexOf("/model/") + 7, filename.length());
                Utils.println(String.format("Loading model %03d '%s'", ++modelCount, filename));
                loadObj(filename);
            }
        }
    }

    /**
     * Load an obj file of a model.
     *
     * @param modelPath path inside model folder (ex. Vumeter/Vumeter.obj)
     */
    private void loadObj(String modelPath) {
        Obj3D obj = new Obj3D();
        if (obj.loadFile(modelPath)) {
            String tag = modelPath.replaceAll(".obj", "").replaceAll(".OBJ", "");
            tag = tag.substring(tag.lastIndexOf('/') + 1, tag.length());
            if (nameToObjHash.containsKey(tag)) {
                Utils.println("Double load of model " + tag);
            }
            nameToObjHash.put(tag, obj);    // name of the file, without extension
            Utils.println(String.format(" - model '%s' loaded", modelPath));
        } else {
            Utils.println(String.format(" - unable to load model '%s'", modelPath));
        }
    }

    public Obj3D getObj(String obj3DName) {
        Obj3D obj = nameToObjHash.get(obj3DName);
        if (obj == null) {
            if (missingObjNamesWarned.add(obj3DName)) {
                Utils.println(String.format("Missing model '%s', using fallback empty model", obj3DName));
            }
            return missingObj;
        }
        return obj;
    }

    public Obj3DPart getPart(String objName, String partName) {
        Obj3D obj = getObj(objName);
        if (obj == null) return null;
        return obj.getPart(partName);
    }

    public void draw(String objName, String partName) {
        Obj3DPart part = getPart(objName, partName);
        if (part != null) part.draw();
    }

    public Set<String> getObjectList() {
        return nameToObjHash.keySet();
    }
}
