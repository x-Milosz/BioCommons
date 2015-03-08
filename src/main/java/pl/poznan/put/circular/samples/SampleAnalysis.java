package pl.poznan.put.circular.samples;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariateOptimizer;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;

import pl.poznan.put.circular.Circular;
import pl.poznan.put.circular.Vector;
import pl.poznan.put.circular.exception.InvalidCircularOperationException;
import pl.poznan.put.circular.exception.InvalidCircularValueException;
import pl.poznan.put.circular.exception.InvalidVectorFormatException;

public class SampleAnalysis {
    private final Collection<Vector> data;
    private final List<Vector> dataSorted;
    private final Vector meanDirection;
    private final double meanResultantLength;
    private final double circularVariance;
    private final double circularStandardDeviation;
    private final double circularDispersion;
    private final double skewness;
    private final double kurtosis;
    private final Vector medianDirection;
    private final double meanDeviation;

    public SampleAnalysis(Collection<Vector> data) throws InvalidCircularValueException {
        super();
        this.data = data;
        this.dataSorted = new ArrayList<>(data);
        Collections.sort(dataSorted);

        TrigonometricMoment um1 = getUncenteredMoment(1);
        meanDirection = um1.getMeanDirection();
        meanResultantLength = um1.getMeanResultantLength();
        circularVariance = 1 - meanResultantLength;
        circularStandardDeviation = Math.sqrt(-2 * Math.log(meanResultantLength));

        TrigonometricMoment cm2 = getCenteredMoment(2);
        TrigonometricMoment um2 = getUncenteredMoment(2);
        circularDispersion = (1.0 - cm2.getMeanResultantLength()) / (2 * Math.pow(meanResultantLength, 2));
        skewness = cm2.getMeanResultantLength() * Math.sin(Vector.subtract(cm2.getMeanDirection(), meanDirection.multiply(2)).getRadians()) / Math.sqrt(circularVariance);
        kurtosis = (cm2.getMeanResultantLength() * Math.cos(Vector.subtract(um2.getMeanDirection(), meanDirection.multiply(2)).getRadians()) - Math.pow(meanResultantLength, 4)) / Math.pow(circularVariance, 2);

        UnivariatePointValuePair medianFunctionRoot = minimizeMedianFunction();
        medianDirection = new Vector(medianFunctionRoot.getPoint());
        meanDeviation = medianFunctionRoot.getValue();
    }

    public Vector getMeanDirection() {
        return meanDirection;
    }

    public double getMeanResultantLength() {
        return meanResultantLength;
    }

    public double getCircularVariance() {
        return circularVariance;
    }

    public double getCircularStandardDeviation() {
        return circularStandardDeviation;
    }

    public double getCircularDispersion() {
        return circularDispersion;
    }

    public double getSkewness() {
        return skewness;
    }

    public double getKurtosis() {
        return kurtosis;
    }

    public Vector getMedianDirection() {
        return medianDirection;
    }

    public double getMeanDeviation() {
        return meanDeviation;
    }

    public TrigonometricMoment getUncenteredMoment(int p) throws InvalidCircularValueException {
        return getMoment(p, false);
    }

    public TrigonometricMoment getCenteredMoment(int p) throws InvalidCircularValueException {
        return getMoment(p, true);
    }

    private TrigonometricMoment getMoment(int p, boolean isCentered) throws InvalidCircularValueException {
        double c = 0;
        double s = 0;

        for (Vector vector : data) {
            double radians = vector.getRadians();

            if (isCentered) {
                radians = Vector.subtract(radians, meanDirection.getRadians());
            }

            c += Math.cos(p * radians);
            s += Math.sin(p * radians);
        }

        c /= data.size();
        s /= data.size();

        double rho = Math.sqrt(Math.pow(c, 2) + Math.pow(s, 2));
        double mi;

        if (s > 0 && c > 0) {
            mi = Math.atan(s / c);
        } else if (c < 0) {
            mi = Math.atan(s / c) + Math.PI;
        } else {
            // s < 0 && c > 0
            mi = Math.atan(s / c) + 2 * Math.PI;
        }

        return new TrigonometricMoment(new Vector(mi), rho);
    }

    public double getCircularRank(Vector datapoint) throws InvalidCircularOperationException {
        if (!data.contains(datapoint)) {
            throw new InvalidCircularOperationException("Cannot calculate circular rank for an observation outside the sample range");
        }

        int rank = dataSorted.indexOf(datapoint) + 1;
        return 2 * Math.PI * rank / data.size();
    }

    private UnivariatePointValuePair minimizeMedianFunction() {
        UnivariateFunction medianObjectiveFunction = new UnivariateFunction() {
            @Override
            public double value(double x) {
                double sum = 0;

                for (Circular vector : data) {
                    sum += Vector.subtract(Math.PI, Vector.subtract(vector.getRadians(), x));
                }

                return Math.PI - sum / data.size();
            }
        };

        UnivariateOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
        return optimizer.optimize(new UnivariateObjectiveFunction(medianObjectiveFunction), GoalType.MINIMIZE, new SearchInterval(0, 2 * Math.PI), new MaxEval(1000));
    }

    @Override
    public String toString() {
        return "SampleAnalysis [meanDirection=" + meanDirection + ", meanResultantLength=" + meanResultantLength + ", circularVariance=" + circularVariance + ", circularStandardDeviation=" + circularStandardDeviation + ", circularDispersion=" + circularDispersion + ", skewness=" + skewness + ", kurtosis=" + kurtosis + ", medianDirection=" + medianDirection + ", meanDeviation=" + meanDeviation + "]";
    }

    public static void main(String[] args) throws IOException, InvalidVectorFormatException, InvalidCircularValueException, InvalidCircularOperationException {
        List<Vector> data = new ArrayList<>();
        List<String> lines = FileUtils.readLines(new File("data/D01"), "UTF-8");

        for (String line : lines) {
            if (line.startsWith("#")) {
                continue;
            }

            for (String token : StringUtils.split(line)) {
                if (!StringUtils.isBlank(token)) {
                    data.add(Vector.fromHourMinuteString(token));
                }
            }
        }

        SampleAnalysis sampleAnalysis = new SampleAnalysis(data);
        System.out.println(sampleAnalysis);
        System.out.println(sampleAnalysis.getCenteredMoment(1));
        System.out.println(sampleAnalysis.getCenteredMoment(2));
        System.out.println(sampleAnalysis.getCircularRank(data.get(0)));
        System.out.println(sampleAnalysis.getUncenteredMoment(1));
        System.out.println(sampleAnalysis.getUncenteredMoment(2));
    }
}
