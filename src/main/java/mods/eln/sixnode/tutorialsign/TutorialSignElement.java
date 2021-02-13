package mods.eln.sixnode.tutorialsign;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.electrical.ElectricalLoad;
import mods.eln.sim.thermal.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.Console;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

import static mods.eln.i18n.I18N.getCurrentLanguage;
import static mods.eln.i18n.I18N.tr;

public class TutorialSignElement extends SixNodeElement {

    static HashMap<String, String> baliseMap = null;

    public static final int setTextFileId = 1;

    String baliseName = "";

    public static void resetBalise() {
        baliseMap = null;
    }

    public static String getText(String balise) {
        if (baliseMap == null) {
            baliseMap = new HashMap<String, String>();
            /*
			try {
				File fXmlFile = Utils.getMapFile("EA/tutorialSign.xml");
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder;
				dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(fXmlFile);
				//Node root = doc.getElementById("sign");
				Node root = doc.getChildNodes().item(0);
				NodeList nList = root.getChildNodes();
				for (int idx = 0; idx < nList.getLength(); idx++){
					Node n = nList.item(idx);
					n.getNamespaceURI();
				}
				int i = 0;
			} catch (Exception e) {
			}
            */
            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            //doc.getDocumentElement().normalize();
            String localizedPath = "EA/tutorialSign/" + getCurrentLanguage() + ".txt"; //localized file path
            String defaultPath = "EA/tutorialSign/en_US.txt"; //if the localized file is not found, then load the default file (en_US)
            String oldPath = "EA/tutorialSign.txt"; //for compatibility
            if(Utils.mapFileExists(localizedPath))
                loadMapFromPath(localizedPath);
            else if(Utils.mapFileExists(defaultPath))
                loadMapFromPath(defaultPath);
            else
                loadMapFromPath(oldPath);
        }
        String text = baliseMap.get(balise);
        if (text == null) return tr("No text associated to this beacon");
        return text;
    }

    private static void loadMapFromPath(String path) {
        try {
            String file = Utils.readMapFile(path);
            String ret;
            if (file.contains("\r\n"))
                ret = "\r\n";
            else
                ret = "\n";

            file = file.replaceAll("#" + ret, "#");
            file = file.replaceAll(ret + "#", "#");

            String[] split = file.split("#");

            boolean add = false;
            String baliseTag = "";

            for (String str : split) {
                if (add)
                    baliseMap.put(baliseTag, str);
                else
                    baliseTag = str;
                add = !add;
            }
        } catch (IOException e) {
            //	e.printStackTrace();
        }
    }

    public TutorialSignElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
    }

    void setBalise(String name) {
        baliseName = name;
        needPublish();
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        setBalise(nbt.getString("baliseName"));
    }
	
/*
	private void setTextFile(String name) {
		if (name.matches("^[a-zA-Z0-9]*$") == false) {
			fileName = "OnlyAlphaNumeric";
			text = "OnlyAlphaNumeric";
		} else {		
			fileName = name;
			try {
				text = Utils.readMapFile("EATuto/" + fileName + ".txt");
			} catch (IOException e) {
				text = "file not found";
			}
		}
		needPublish();
	}*/


    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("baliseName", baliseName);
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        return 0;
    }

    @Override
    public String multiMeterString() {
        return "";
    }

    @NotNull
    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeUTF(baliseName);
            stream.writeUTF(getText(baliseName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            switch (stream.readByte()) {
                case setTextFileId:
                    setBalise(stream.readUTF());
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public void initialize() {
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        return false;
    }
}
