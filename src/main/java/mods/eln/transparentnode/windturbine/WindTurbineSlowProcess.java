package mods.eln.transparentnode.windturbine;

import mods.eln.misc.Coordinate;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

class WindTurbineSlowProcess implements IProcess, INBTTReady {
    private static final double AIR_DENSITY = 1.225;
    private static final double OPTIMAL_TIP_SPEED_RATIO = 6.0;
    private static final double MAX_POWER_COEFFICIENT = 0.36;
    private static final double CUT_IN_WIND = 3.0;
    private static final double CUT_OUT_DRAG_FACTOR = 0.015;
    //private static final double localWinDeriveMax = 0.1;
    private static final double environmentTimeCounterReset = 10.0;
    private static final double localWindTimeCounterReset = 1.0;
    private static final double localWindMax = 3.0;
    private static final double localWinDeriveLostFactor = 0.3;
    private static final double localWinDeriveDeriveMax = 0.1;

    private final WindTurbineElement turbine;
    private final String name;

    private double environmentWindFactor = 0.0;
    private double environmentTimeCounter = 0;
    private double localWind = 0;
    private double localWindDerive = 0;
    private double localWindTimeCounter = 0;
    private int counter = 0;
    private double rotorRads = 0.0;
    private double lastElectricalPower = 0.0;
    private double lastAerodynamicPower = 0.0;
    private double stallFactor = 0.0;

    WindTurbineSlowProcess(String name, WindTurbineElement turbine) {
        this.turbine = turbine;
        this.name = name;
    }

    double getWind() {
        return Math.abs(localWind + Utils.getWind(turbine.node.coordinate.dimension, turbine.node.coordinate.y +
            turbine.descriptor.offY)) * environmentWindFactor;
    }

    double getRotorRads() {
        return rotorRads;
    }

    double getRotorEnergy() {
        return rotorEnergy(rotorRads, turbine.descriptor.rotorInertia);
    }

    double getLastElectricalPower() {
        return lastElectricalPower;
    }

    double getLastAerodynamicPower() {
        return lastAerodynamicPower;
    }

    static double optimalRotorRads(double wind, double rotorRadius) {
        if (rotorRadius <= 0.0) return 0.0;
        return Math.max(0.0, OPTIMAL_TIP_SPEED_RATIO * wind / rotorRadius);
    }

    static double powerCoefficient(double rotorRads, double wind, double rotorRadius) {
        if (wind < CUT_IN_WIND || rotorRads <= 0.0 || rotorRadius <= 0.0) return 0.0;
        double ratio = rotorRads * rotorRadius / (wind * OPTIMAL_TIP_SPEED_RATIO);
        if (ratio <= 0.0) return 0.0;
        double curve = 2.0 * ratio / (1.0 + ratio * ratio);
        return MAX_POWER_COEFFICIENT * Math.max(0.0, Math.min(1.0, curve));
    }

    static double availableWindPower(double wind, double sweptArea) {
        if (wind < CUT_IN_WIND) return 0.0;
        return 0.5 * AIR_DENSITY * sweptArea * wind * wind * wind;
    }

    static double aerodynamicPower(double rotorRads, double wind, double rotorRadius, double sweptArea) {
        if (wind < CUT_IN_WIND) return 0.0;
        double windPower = availableWindPower(wind, sweptArea);
        if (rotorRads <= 0.1) {
            return windPower * MAX_POWER_COEFFICIENT * 0.08;
        }
        return windPower * powerCoefficient(rotorRads, wind, rotorRadius);
    }

    private static double rotorEnergy(double rads, double inertia) {
        return 0.5 * inertia * rads * rads;
    }

    private void setRotorEnergy(double energy) {
        if (!Double.isFinite(energy) || energy <= 0.0) {
            rotorRads = 0.0;
            return;
        }
        rotorRads = Math.sqrt(2.0 * energy / turbine.descriptor.rotorInertia);
    }

    void setWind(double wind) {
        this.localWind = wind;
    }

    @Override
    public void process(double time) {
        WindTurbineDescriptor d = turbine.descriptor;
        environmentTimeCounter -= time;
        if (environmentTimeCounter < 0.0) {
            environmentTimeCounter += environmentTimeCounterReset * (0.75 + Math.random() * 0.5);

            int x1, x2, y1, y2, z1, z2;

            Coordinate coord = new Coordinate(turbine.node.coordinate);

            x1 = coord.x - d.rayX;
            x2 = coord.x + d.rayX;
            y1 = coord.y - d.rayY + d.offY;
            y2 = coord.y + d.rayY + d.offY;
            z1 = coord.z - d.rayZ;
            z2 = coord.z + d.rayZ;

            int blockBusyCount = -d.blockMalusSubCount;
            boolean notInCache = false;
            if (turbine.node.coordinate.getWorldExist()) {
                World world = turbine.node.coordinate.world();
                //IChunkProvider chunk = world.getChunkProvider();

                for (int x = x1; x <= x2; x++) {
                    for (int y = y1; y <= y2; y++) {
                        for (int z = z1; z <= z2; z++) {
                            if (!world.blockExists(x, y, z)) {
                                notInCache = true;
                                break;
                            }
                            if (world.getBlock(x, y, z) != Blocks.air) {
                                blockBusyCount++;
                            }
                        }
                        if (notInCache) break;
                    }
                    if (notInCache) break;
                }
            } else {
                notInCache = true;
            }
            if (!notInCache) {
                environmentWindFactor = Math.max(0.0, Math.min(1.0, 1.0 - blockBusyCount * d.blockMalus));

                Utils.println("EnvironementWindFactor : " + environmentWindFactor);
            }
        }

        localWindTimeCounter -= time;
        if (localWindTimeCounter < 0) {
            localWindTimeCounter += localWindTimeCounterReset;

            localWindDerive *= 1 - (localWinDeriveLostFactor * localWindTimeCounterReset);
            localWindDerive += (Math.random() * 2.0 - 1.0) * localWinDeriveDeriveMax * localWindTimeCounterReset;
        }

        localWind += localWindDerive * time;

        if (localWind > localWindMax) {
            localWind = localWindMax;
            localWindDerive = 0.0;
        }
        if (localWind < -localWindMax) {
            localWind = -localWindMax;
            localWindDerive = 0.0;
        }

        double wind = getWind();

        /*if (wind > d.maxWind) {
            if (Math.random() < (wind - d.maxWind) * 0.02) {
                turbine.selfDestroy();
            }
        }*/

        double aeroPower = aerodynamicPower(rotorRads, wind, d.rotorRadius, d.rotorSweptArea);
        double electricalPower = Math.max(0.0, turbine.powerSource.getPower());
        double shaftElectricalPower = electricalPower / d.generationEfficiency;
        double dragPower = CUT_OUT_DRAG_FACTOR * rotorRads * rotorRads + 0.15 * Math.max(rotorRads, 1.0);
        double nextEnergy = getRotorEnergy() + (aeroPower - shaftElectricalPower - dragPower) * time;
        setRotorEnergy(nextEnergy);

        double optimalRads = optimalRotorRads(wind, d.rotorRadius);
        stallFactor = optimalRads <= 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, rotorRads / optimalRads));
        lastElectricalPower = electricalPower;
        lastAerodynamicPower = aeroPower;

        counter++;
        if (counter % 20 == 0) {
            Utils.println("Wind : " + getWind() + "  Rads : " + rotorRads + " AeroP : " + aeroPower + " ElecP : " + electricalPower);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        localWind = nbt.getDouble(str + name + "localWind");
        environmentWindFactor = nbt.getDouble(str + name + "environementWindFactor");
        rotorRads = nbt.getDouble(str + name + "rotorRads");
        lastElectricalPower = nbt.getDouble(str + name + "lastElectricalPower");
        lastAerodynamicPower = nbt.getDouble(str + name + "lastAerodynamicPower");
        stallFactor = nbt.getDouble(str + name + "stallFactor");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        nbt.setDouble(str + name + "localWind", localWind);
        nbt.setDouble(str + name + "environementWindFactor", environmentWindFactor);
        nbt.setDouble(str + name + "rotorRads", rotorRads);
        nbt.setDouble(str + name + "lastElectricalPower", lastElectricalPower);
        nbt.setDouble(str + name + "lastAerodynamicPower", lastAerodynamicPower);
        nbt.setDouble(str + name + "stallFactor", stallFactor);
    }
}
