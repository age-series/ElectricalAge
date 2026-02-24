package mods.eln.sim;

public class PhysicalConstant {
    public static final double zeroCelsiusInKelvin = 273.15;
    @Deprecated
    public static final double ambientTemperatureCelsius = 20;
    @Deprecated
    public static final double ambientTemperatureKelvin = zeroCelsiusInKelvin + ambientTemperatureCelsius;
}
