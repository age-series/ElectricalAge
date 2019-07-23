package mods.eln.sixnode.wirelesssignal;

import mods.eln.misc.Coordinate;

public interface IWirelessSignalTx {

    public Coordinate getCoordonate();

    public int getRange();

    public String getChannel();

    public double getValue();
}
