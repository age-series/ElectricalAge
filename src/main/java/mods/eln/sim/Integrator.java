package mods.eln.sim;

import java.util.function.Function;

public class Integrator {
    double timeStep;
    int integrationPhase, stepsTaken;

    double[] sample, integrations;

    public Integrator(double dt) {
        timeStep = dt;
        reset();
    }
    public void reset() {
        integrationPhase = stepsTaken = 0;
        sample = new double[]{0.0, 0.0, 0.0, 0.0, 0.0};
        integrations = new double[]{0.0, 0.0, 0.0, 0.0};
    }

    public double nextStep(double nextValue) {
        double summation = 0.0;

        stepsTaken++;

        summation += (sample[0] = sample[1]) * 7;
        summation += (sample[1] = sample[2]) * 32;
        summation += (sample[2] = sample[3]) * 12;
        summation += (sample[3] = sample[4]) * 32;
        summation += (sample[4] = nextValue) * 7;
        summation *= 2 * timeStep / 45;
        summation = (integrations[integrationPhase] += summation);

        integrationPhase = (integrationPhase + 1) & 3; // Effectively p = (p+1) mod 4
        return summation;
    }

    public static void main(String[] argv) {
        Function<Double, Double> testFunction = (Double x) -> Math.sin(10*x);
        Function<Double, Double> actualIntegrationFunction = (Double x) -> Math.sin(5*x)*Math.sin(5*x)/5;

        double dx = 1.0 / 20;

        Integrator integrator = new Integrator(dx);

        long t_end = Math.round(1 / dx) + 1;
        for (long t = 0; t < t_end; t++) {
            double x, test, actual, error;
            x = t * dx;
            test = integrator.nextStep(testFunction.apply(x));
            actual = actualIntegrationFunction.apply(x);
            error = test - actual;

            System.out.printf(
                    "x = %1f, Y ~= %2f, Y = %3f, E = %4e%n"
                    , x, test, actual, error
            );
        }
    }
}
