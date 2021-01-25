package mods.eln.metrics

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder
import com.amazonaws.services.cloudwatch.model.Dimension
import com.amazonaws.services.cloudwatch.model.MetricDatum
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest
import mods.eln.Eln


object CloudwatchMetricsImpl: IMetricsAbstraction {
    val cw = AmazonCloudWatchClientBuilder.defaultClient()

    /**
     * Put a metric to CloudWatch.
     *
     * @param value The value of the metric in question
     * @param unit The unit type
     * @param metricName The name of the metric (for example, time to complete X)
     * @param namespace The part of the code we are in. For example, "MNA"
     * @param description The description (unused for CloudWatch)
     */
    override fun putMetric(value: Double, unit: UnitTypes, metricName: String, namespace: String, description: String) {
        val dimension = Dimension().withName("Server").withValue(Eln.metricsDimension)
        val datum = MetricDatum().withMetricName(metricName).withUnit(unit.symbol).withValue(value).withDimensions(dimension)
        val request = PutMetricDataRequest().withNamespace("ELN/$namespace").withMetricData(datum)
        cw.putMetricData(request)
    }
}
