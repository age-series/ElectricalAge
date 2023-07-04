package mods.eln.sim;

public class ThermalLoad {

    /**
     * Current temperature, in celsius.
     */
    public double temperatureCelsius;
    public double Rp;
    /**
     * Thermal resistance, analogous to ohms.
     */
    public double Rs;

    /**
     * heatCapacity: <a href="https://en.wikipedia.org/wiki/Heat_capacity">Heat Capacity</a>
     * Joules/Kelvin
     */
    public double heatCapacity;
    /**
     * Current thermal power, in watts, of this load.
     * This will be negative if it's cooling down.
     */
    public double Pc;
    /**
     * Current resistive loss, in watts.
     */
    public double Prs;
    public double Psp;

    /**
     * Absolute heat transfer in this simulator tick.
     */
    public double PrsTemp = 0;
    /**
     * Heat power transferred during this simulator tick.
     */
    public double PspTemp = 0;
    /**
     * Relative heat transfer during this simulator tick.
     */
    public double PcTemp;

    boolean isSlow;

    public ThermalLoad() {
        setHighImpedance();
        temperatureCelsius = 0;
        PcTemp = 0;
        Pc = 0;
        Prs = 0;
        Psp = 0;
    }

    public ThermalLoad(double Tc, double Rp, double Rs, double heatCapacity) {
        this.temperatureCelsius = Tc;
        this.Rp = Rp;
        this.Rs = Rs;
        this.heatCapacity = heatCapacity;
        PcTemp = 0;
    }

    public void setRsByTao(double tao) {
        Rs = tao / heatCapacity;
    }

    public void setHighImpedance() {
        Rs = 1000000000.0;
        heatCapacity = 1;
        Rp = 1000000000.0;
    }

    public static final ThermalLoad externalLoad = new ThermalLoad(0, 0, 0, 0);

    public void setRp(double Rp) {
        this.Rp = Rp;
    }

    public double getPower() {
        if (Double.isNaN(Prs) || Double.isNaN(Pc) || Double.isNaN(temperatureCelsius) || Double.isNaN(Rp) || Double.isNaN(Psp)) return 0.0;
        return (Prs + Math.abs(Pc) + temperatureCelsius / Rp + Psp) / 2;
    }

    public void set(double Rs, double Rp, double C) {
        this.Rp = Rp;
        this.Rs = Rs;
        this.heatCapacity = C;
    }

    public static void moveEnergy(double energy, double time, ThermalLoad from, ThermalLoad to) {
        if(Double.isNaN(energy) || Double.isNaN(time)|| time == 0.0 || time == -0.0 ||Double.isNaN(from.PcTemp) || Double.isNaN(from.PspTemp)) return;
        double I = energy / time;
        double absI = Math.abs(I);
        from.PcTemp -= I;
        to.PcTemp += I;
        from.PspTemp += absI;
        to.PspTemp += absI;
    }

    public static void movePower(double power, ThermalLoad from, ThermalLoad to) {
        if(Double.isNaN(power) || Double.isNaN(from.PcTemp) || Double.isNaN(from.PspTemp)) return;
        double absI = Math.abs(power);
        from.PcTemp -= power;
        to.PcTemp += power;
        from.PspTemp += absI;
        to.PspTemp += absI;
    }

    public void movePowerTo(double power) {
        if(Double.isNaN(power)) return;
        PcTemp += power;
        PspTemp += power;
    }

    public double getTemperature() {
        if (Double.isNaN(temperatureCelsius)) {
            temperatureCelsius = 0.0;
        }
        return temperatureCelsius;
    }

    public boolean isSlow() {
        return isSlow;
    }

    public void setAsSlow() {
        isSlow = true;
    }

    public void setAsFast() {
        isSlow = false;
    }
}
