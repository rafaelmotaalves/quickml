package quickml.supervised.inspection;

import com.google.common.collect.*;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickml.supervised.PredictiveModel;
import quickml.supervised.PredictiveModelBuilderFactory;
import quickml.supervised.crossValidation.CrossValidator;
import quickml.supervised.crossValidation.CrossValidatorBuilder;
import quickml.supervised.crossValidation.StationaryCrossValidatorBuilder;
import quickml.supervised.crossValidation.crossValLossFunctions.*;
import quickml.data.*;
import quickml.supervised.PredictiveModelBuilder;

import java.util.*;

public class AttributeImportanceFinder {
    private static final Logger logger = LoggerFactory.getLogger(AttributeImportanceFinder.class);
    Set<String> attributesToNotRemove = Sets.newHashSet();

    public AttributeImportanceFinder() {

    }
    public AttributeImportanceFinder(Set<String> attributesToNotRemove) {
        this.attributesToNotRemove = attributesToNotRemove;
    }

    public<PM extends PredictiveModel<AttributesMap, PredictionMap>,  PMB extends PredictiveModelBuilder<AttributesMap, PM>>  void determineAttributeImportance(CrossValidatorBuilder<AttributesMap, PredictionMap> crossValidatorBuilder,
                                             PredictiveModelBuilderFactory<AttributesMap,  PM, PMB> predictiveModelBuilderFactory, Map<String, Object> config, final Iterable<? extends Instance<AttributesMap>> trainingData,
                                             int iterations, double percentageOfFeaturesToRemovePerIteration, String primaryLossFunction, Map<String, CrossValLossFunction<PredictionMap>> crossValLossFunctionMap) {

        Set<String> attributes = getAllAttributesInTrainingSet(trainingData);
        attributes.add("noAttributesRemoved");
        //do recursive feature elimination
        List<Pair<String, MultiLossFunctionWithModelConfigurations<PredictionMap>>> attributesWithLosses = Lists.newArrayList();
        for (int i = 0; i < iterations; i++) {
            CrossValidator<AttributesMap, PredictionMap> crossValidator = crossValidatorBuilder.createCrossValidator();
            crossValLossFunctionMap = Maps.newHashMap();
            crossValLossFunctionMap.put("LogLoss", new ClassifierLogCVLossFunction(.000001));
            crossValLossFunctionMap.put("AUC", new WeightedAUCCrossValLossFunction(1.0));
            crossValLossFunctionMap.put("LogLossCorrectedForDownSampling", new LossFunctionCorrectedForDownsampling(new ClassifierLogCVLossFunction(0.000001), 0.99, Double.valueOf(0.0)));

            attributesWithLosses = crossValidator.getAttributeImportances(predictiveModelBuilderFactory, config, trainingData, primaryLossFunction, attributes, crossValLossFunctionMap);
            if (i < iterations - 1) {
                updateAttributesUsedInTraining(trainingData, attributesWithLosses, attributes, percentageOfFeaturesToRemovePerIteration);
            }
            logger.info("model losses" + getModelLoss(attributesWithLosses).toString());
            for (Pair<String, MultiLossFunctionWithModelConfigurations<PredictionMap>> pair : attributesWithLosses) {
                logger.info("attribute: " + pair.getValue0() + ".  losses: " + pair.getValue1().getLossesWithModelConfigurations().get(primaryLossFunction).getLoss());
            }
        }

        for (Pair<String, MultiLossFunctionWithModelConfigurations<PredictionMap>> pair : attributesWithLosses) {
            logger.info("attribute: " + pair.getValue0() + ".  losses: " + pair.getValue1().getLossesWithModelConfigurations().get(primaryLossFunction).getLoss());
        }
    }

    private Map<String, Double> getModelLoss( List<Pair<String, MultiLossFunctionWithModelConfigurations<PredictionMap>>> attributesWithLosses) {
        for (int i = attributesWithLosses.size() - 1; i >= 0; i--) {
            if (attributesWithLosses.get(i).getValue0().equals("noAttributesRemoved")) {
                return attributesWithLosses.get(i).getValue1().getLossMap();
            }
        }
        return null;
    }

    private void updateAttributesUsedInTraining(final Iterable<? extends Instance<AttributesMap>> trainingData, List<Pair<String, MultiLossFunctionWithModelConfigurations<PredictionMap>>> attributesWithLosses,
                                                Set<String> allAttributes, double percentageOfAttributesToRemoveAtEachIteration) {
        int numberOfAttributesToRemove = (int) (percentageOfAttributesToRemoveAtEachIteration * allAttributes.size());
        Set<String> attributesToRemove = Sets.newHashSet();
        for (int i = attributesWithLosses.size() - 1; i >= Math.max(0, attributesWithLosses.size() - 1 - numberOfAttributesToRemove); i--) {

            String attributeToRemove = attributesWithLosses.get(i).getValue0();
            if (!attributesToNotRemove.contains(attributeToRemove)) {
                attributesToRemove.add(attributeToRemove);
                allAttributes.remove(attributeToRemove);
            }
        }
        allAttributes.add("noAttributesRemoved");

        //remove attributes from training data
        for (Instance<AttributesMap> instance : trainingData) {
            AttributesMap attributes = instance.getAttributes();
            for (String attributeName : attributesToRemove) {
                if (attributes.containsKey(attributeName))
                    attributes.remove(attributeName);
            }
        }
    }

    private Set<String> getAllAttributesInTrainingSet(Iterable<? extends Instance<AttributesMap>> trainingData) {
        Set<String> attributes = Sets.newHashSet();
        for (Instance<AttributesMap> instance : trainingData) {
            attributes.addAll(instance.getAttributes().keySet());
        }
        return attributes;
    }
}