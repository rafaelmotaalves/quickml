package quickml.supervised.featureEngineering;

import quickml.data.MapWithDefaultOfZero;
import quickml.supervised.classifier.Classifier;
import quickml.supervised.classifier.PredictiveModel;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A predictive model that wraps another predictive model but modifies the input
 * Attributes based on one or more "enrichers".  This objected is created by a
 * {@link FeatureEngineeringPredictiveModelBuilder}.
 */
public class FeatureEngineeredPredictiveModel extends Classifier {
    private static final long serialVersionUID = 7279329500376419142L;
    private final PredictiveModel<Map<String, Serializable>, MapWithDefaultOfZero> wrappedPredictiveModel;
    private final List<AttributesEnricher> attributesEnrichers;

    public FeatureEngineeredPredictiveModel(PredictiveModel<Map<String, Serializable>, MapWithDefaultOfZero> wrappedPredictiveModel, List<AttributesEnricher> attributesEnrichers) {
        this.wrappedPredictiveModel = wrappedPredictiveModel;
        this.attributesEnrichers = attributesEnrichers;
    }

    @Override
    public MapWithDefaultOfZero predict(Map<String, Serializable> attributes) {
        Map<String, Serializable> enrichedAttributes = enrichAttributes(attributes);
        return wrappedPredictiveModel.predict(enrichedAttributes);
    }

    private Map<String, Serializable> enrichAttributes(final Map<String, Serializable> attributes) {
        Map<String, Serializable> enrichedAttributes = attributes;
        for (AttributesEnricher attributesEnricher : attributesEnrichers) {
            enrichedAttributes = attributesEnricher.apply(enrichedAttributes);
        }
        return enrichedAttributes;
    }

    @Override
    public void dump(final Appendable appendable) {
        wrappedPredictiveModel.dump(appendable);
    }
}
