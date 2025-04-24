package com.trainguy9512.locomotion.animation.pose.function;

import com.google.common.collect.Maps;
import com.trainguy9512.locomotion.animation.driver.VariableDriver;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class BlendPosesFunction implements PoseFunction<LocalSpacePose> {

    private final PoseFunction<LocalSpacePose> baseFunction;
    private final Map<BlendInput, VariableDriver<Float>> inputs;

    public BlendPosesFunction(PoseFunction<LocalSpacePose> baseFunction, Map<BlendInput, VariableDriver<Float>> inputs){
        this.baseFunction = baseFunction;
        this.inputs = inputs;
    }

    @Override
    public @NotNull LocalSpacePose compute(FunctionInterpolationContext context) {
        LocalSpacePose pose = this.baseFunction.compute(context);
        for(BlendInput blendInput : this.inputs.keySet()) {
            float weight = this.inputs.get(blendInput).getValueInterpolated(context.partialTicks());
            if(weight != 0f){
                if(blendInput.jointMask.isPresent()) {
                    pose = pose.interpolatedFilteredByJoints(blendInput.inputFunction.compute(context), weight, blendInput.jointMask.get());
                } else {
                    pose = pose.interpolated(blendInput.inputFunction.compute(context), weight);
                }
            }
        }
        return pose;
    }

    @Override
    public void tick(FunctionEvaluationState evaluationState) {
        this.baseFunction.tick(evaluationState);
        this.inputs.forEach((blendInput, weightDriver) -> {
            weightDriver.pushCurrentToPrevious();
            float weight = blendInput.weightFunction.apply(evaluationState);
            weightDriver.setValue(weight);

            if(weight != 0f) {
                blendInput.inputFunction.tick(evaluationState);
            }
        });
    }

    @Override
    public PoseFunction<LocalSpacePose> wrapUnique() {
        Builder builder = BlendPosesFunction.builder(this.baseFunction.wrapUnique());
        for(BlendInput blendInput : this.inputs.keySet()){
            builder.addBlendInput(blendInput.inputFunction.wrapUnique(), blendInput.weightFunction, blendInput.jointMask.orElse(null));
        }
        return builder.build();
    }

    @Override
    public Optional<AnimationPlayer> testForMostRelevantAnimationPlayer() {
        List<Optional<AnimationPlayer>> blendAnimationPlayers = new ArrayList<>();
        this.inputs.forEach(((blendInput, weightDriver) -> {
            if (weightDriver.getCurrentValue() >= 0.5f) {
                blendAnimationPlayers.add(blendInput.inputFunction.testForMostRelevantAnimationPlayer());
            }
        }));
        if (!blendAnimationPlayers.isEmpty()) {
            return blendAnimationPlayers.getLast().isPresent() ?
                    blendAnimationPlayers.getLast() :
                    this.baseFunction.testForMostRelevantAnimationPlayer();
        } else {
            return this.baseFunction.testForMostRelevantAnimationPlayer();
        }
    }


    public static Builder builder(PoseFunction<LocalSpacePose> base){
        return new Builder(base);
    }

    public static class Builder {

        private final PoseFunction<LocalSpacePose> baseFunction;
        private final Map<BlendInput, VariableDriver<Float>> inputs;

        private Builder(PoseFunction<LocalSpacePose> baseFunction){
            this.baseFunction = baseFunction;
            this.inputs = Maps.newHashMap();
        }

        public Builder addBlendInput(PoseFunction<LocalSpacePose> inputFunction, Function<FunctionEvaluationState, Float> weightFunction, @Nullable Set<String> jointMask){
            this.inputs.put(new BlendInput(inputFunction, weightFunction, Optional.ofNullable(jointMask)), VariableDriver.ofFloat(() -> 0f));
            return this;
        }

        public Builder addBlendInput(PoseFunction<LocalSpacePose> inputFunction, Function<FunctionEvaluationState, Float> weightFunction){
            return this.addBlendInput(inputFunction, weightFunction, null);
        }

        public BlendPosesFunction build(){
            return new BlendPosesFunction(this.baseFunction, this.inputs);
        }
    }

    public record BlendInput(PoseFunction<LocalSpacePose> inputFunction, Function<FunctionEvaluationState, Float> weightFunction, Optional<Set<String>> jointMask){

    }
}
